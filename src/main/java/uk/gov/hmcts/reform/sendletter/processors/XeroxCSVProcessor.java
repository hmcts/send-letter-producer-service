package uk.gov.hmcts.reform.sendletter.processors;

import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

public class XeroxCSVProcessor implements ILetterProcessor {

    @Override
    public void process(LetterRepository repository) {
        // Try to fetch the files from Xerox and update each letter's status.
    }
}
