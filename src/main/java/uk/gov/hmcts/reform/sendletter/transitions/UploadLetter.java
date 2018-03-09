package uk.gov.hmcts.reform.sendletter.transitions;

import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;

public class UploadLetter extends StateTransition {
    public UploadLetter() {
        super(LetterState.Created, LetterState.Uploaded);
    }

    @Override
    public boolean tryProcess(Letter letter) {
        // Try and convert and FTP the letter.
        return false;
    }
}
