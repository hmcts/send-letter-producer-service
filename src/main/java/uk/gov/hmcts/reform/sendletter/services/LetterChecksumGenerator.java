package uk.gov.hmcts.reform.sendletter.services;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.Letter;

@Component
public class LetterChecksumGenerator {

    public String generateChecksum(Letter letter) {
        throw new NotImplementedException();
    }
}
