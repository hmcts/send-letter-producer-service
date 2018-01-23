package uk.gov.hmcts.reform.sendletter.services;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.hmcts.reform.sendletter.SampleData;

public class LetterChecksumGeneratorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void will_throw_not_implemented_exception() {
        LetterChecksumGenerator generator = new LetterChecksumGenerator();

        exception.expect(NotImplementedException.class);

        generator.generateChecksum(SampleData.letter());
    }
}
