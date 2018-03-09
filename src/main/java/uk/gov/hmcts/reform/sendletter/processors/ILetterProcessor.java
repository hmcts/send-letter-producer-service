package uk.gov.hmcts.reform.sendletter.processors;

import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

public interface ILetterProcessor {
    void process(LetterRepository repository);
}
