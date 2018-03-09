package uk.gov.hmcts.reform.sendletter.transitions;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

public interface ITransition {
    boolean transition(Letter letter);
}
