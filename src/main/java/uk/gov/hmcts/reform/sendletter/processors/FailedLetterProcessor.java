package uk.gov.hmcts.reform.sendletter.processors;

import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

public class FailedLetterProcessor implements  ILetterProcessor {
    @Override
    public void process(LetterRepository repository) {
        // Find uploaded payments that are older than a threshold age,
        // and mark them as failed.
    }
}
