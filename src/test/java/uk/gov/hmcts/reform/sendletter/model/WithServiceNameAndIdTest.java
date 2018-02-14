package uk.gov.hmcts.reform.sendletter.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RunWith(SpringRunner.class)
public class WithServiceNameAndIdTest {

    @Autowired
    private JacksonTester<WithServiceNameAndId<Letter>> jacksonTester;

    @Test
    public void should_serialize_as_expected() throws Exception {
        // given
        Letter letter = SampleData.letter();
        String serviceName = "my service name";
        UUID id = UUID.randomUUID();

        // when
        JsonContent<WithServiceNameAndId<Letter>> json =
            jacksonTester.write(new WithServiceNameAndId<>(letter, serviceName, id));

        // then
        assertThat(json)
            .hasJsonPathArrayValue("@.documents")
            .hasJsonPathValue("@.type")
            .hasJsonPathMapValue("@.additional_data")
            .hasJsonPathValue("@.service")
            .hasJsonPathValue("@.id");
    }
}
