package uk.gov.hmcts.reform.sendletter.transitions;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

public class UploadLetter implements ITransition {

    public void process(Letter letter) {

    }

    @Override
    public boolean transition(Letter letter) {
        return false;
    }
}
