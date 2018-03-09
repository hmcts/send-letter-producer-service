package uk.gov.hmcts.reform.sendletter.transitions;

import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;

public abstract class StateTransition {
    public final LetterState from;
    public final LetterState to;
    public StateTransition(LetterState from, LetterState to) {
        this.from = from;
        this.to = to;
    }

    // Do the work to process the letter,
    // eg. upload it, download the report for it.
    public abstract boolean tryProcess(Letter letter);
}
