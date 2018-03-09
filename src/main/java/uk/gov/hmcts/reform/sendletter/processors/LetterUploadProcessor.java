package uk.gov.hmcts.reform.sendletter.processors;

import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;

import java.util.List;

public class LetterUploadProcessor implements ILetterProcessor {

    @Override
    public void process(LetterRepository repository) {
        List<Letter> letters = repository.findByState(LetterState.Created);
        for (Letter letter : letters) {
            if (generateAndFtp(letter)) {
               repository.updateState(letter.getId(), LetterState.Uploaded);
            }
        }
    }

    private boolean generateAndFtp(Letter letter) {
        // Try to FTP the letter, return true if successful.
        return true;
    }
}
