package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.JsonPath;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class SendLetterTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${letter-resubmission-interval-in-hours}")
    private int resubmissionInterval;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @SpyBean
    private AppInsights insights;

    @Mock
    private IQueueClient queueClient;

    @SpyBean
    private LetterRepository letterRepository;

    @After
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM letters", Collections.emptyMap());
    }

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        MvcResult result = send(readResource("letter.json"))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();

        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());
        verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(true), anyString());
    }

    @Test
    public void should_return_200_when_same_letter_is_sent_twice() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        // given
        String letter = readResource("letter.json");

        // when
        String response1 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String response2 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // then
        assertThat(response1).isEqualTo(response2);

        // and
        UUID letterId = UUID.fromString(JsonPath.read(response1, "$.letter_id"));
        SqlRowSet rs = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );

        rs.next();

        assertThat(rs.getInt("submit_index")).isEqualTo(1);
        assertThat(rs.getTimestamp("resubmitted_at")).isNull();
    }

    @Test
    public void should_allow_resubmit_and_increase_the_index() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        // given
        String letter = readResource("letter.json");
        String response1 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID letterId = UUID.fromString(JsonPath.read(response1, "$.letter_id"));
        String query = String.format(
            "UPDATE letters SET created_at = CURRENT_TIMESTAMP - (%d * interval '1 hour') WHERE id = :id",
            resubmissionInterval + 1
        );
        jdbcTemplate.update(query, new MapSqlParameterSource().addValue("id", letterId));

        // when
        String response2 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // then
        assertThat(response1).isEqualTo(response2);

        // and
        SqlRowSet rs = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );

        rs.next();

        assertThat(rs.getInt("submit_index")).isEqualTo(2);
        assertThat(rs.getTimestamp("resubmitted_at")).isNotNull();
    }

    @Test
    public void should_not_allow_resubmit_when_resubmission_already_happened_within_allowed_window() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        // given
        String letter = readResource("letter.json");
        String response1 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID letterId = UUID.fromString(JsonPath.read(response1, "$.letter_id"));

        // and
        String query = String.format(
            "UPDATE letters "
                + "SET submit_index = 2,"
                + "  resubmitted_at = CURRENT_TIMESTAMP - (%d * interval '1 hour' / double precision '2.0') "
                + "WHERE id = :id",
            resubmissionInterval + 1
        );
        jdbcTemplate.update(query, new MapSqlParameterSource().addValue("id", letterId));
        SqlRowSet rs1 = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );
        rs1.next();

        // when
        String response2 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // then
        assertThat(response1).isEqualTo(response2);

        // and
        SqlRowSet rs2 = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );
        rs2.next();

        assertThat(rs2.getInt("submit_index")).isEqualTo(rs1.getInt("submit_index"));
        assertThat(rs2.getTimestamp("resubmitted_at")).isEqualTo(rs1.getTimestamp("resubmitted_at"));
    }

    @Test
    public void should_allow_resubmit_when_resubmission_happened_before_allowed_window() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        // given
        String letter = readResource("letter.json");
        String response1 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID letterId = UUID.fromString(JsonPath.read(response1, "$.letter_id"));

        // and
        String query = String.format(
            "UPDATE letters "
                + "SET submit_index = 2,"
                + "  resubmitted_at = CURRENT_TIMESTAMP - (%d * interval '1 hour') "
                + "WHERE id = :id",
            resubmissionInterval + 1
        );
        jdbcTemplate.update(query, new MapSqlParameterSource().addValue("id", letterId));
        SqlRowSet rs1 = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );
        rs1.next();

        // when
        String response2 = send(letter).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // then
        assertThat(response1).isEqualTo(response2);

        // and
        SqlRowSet rs2 = jdbcTemplate.queryForRowSet(
            "SELECT * FROM letters WHERE id = :id",
            new MapSqlParameterSource().addValue("id", letterId)
        );
        rs2.next();

        assertThat(rs2.getInt("submit_index")).isEqualTo(rs1.getInt("submit_index") + 1);
        assertThat(rs2.getTimestamp("resubmitted_at")).isNotEqualTo(rs1.getTimestamp("resubmitted_at"));
    }

    @Test
    public void should_return_500_when_sending_message_has_failed() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        willThrow(ServiceBusException.class).given(queueClient).send(any(Message.class));

        send(readResource("letter.json")).andExpect(status().isInternalServerError());

        verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(false), anyString());
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("").andExpect(status().isBadRequest());

        verifyNoMoreInteractions(insights);
    }

    private ResultActions send(String content) throws Exception {
        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
