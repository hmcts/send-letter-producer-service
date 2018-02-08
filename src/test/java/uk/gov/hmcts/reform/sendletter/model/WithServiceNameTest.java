package uk.gov.hmcts.reform.sendletter.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@RunWith(SpringRunner.class)
public class WithServiceNameTest {

    @Autowired
    private JacksonTester<WithServiceName<Letter>> jacksonTester;

    @Test
    public void should_serialize_as_expected() throws Exception {
        // given
        Letter letter = SampleData.letter();
        String serviceName = "my service name";

        // when
        JsonContent<WithServiceName<Letter>> json =
            jacksonTester.write(new WithServiceName<>(letter, serviceName));

        // then
        assertThat(json)
            .hasJsonPathArrayValue("@.documents")
            .hasJsonPathValue("@.type")
            .hasJsonPathValue("@.service");
    }
}
