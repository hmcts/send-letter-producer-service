package uk.gov.hmcts.reform.sendletter.transitions;

import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;

public class XeroxCSVProcessor extends StateTransition {
    public XeroxCSVProcessor() {
        super(LetterState.Uploaded, LetterState.Posted);
    }

    @Override
    public boolean tryProcess(Letter letter) {
        return false;
    }
}
