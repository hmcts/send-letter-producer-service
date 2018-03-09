package uk.gov.hmcts.reform.sendletter.tasks;

import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.processors.FailedLetterProcessor;
import uk.gov.hmcts.reform.sendletter.processors.ILetterProcessor;
import uk.gov.hmcts.reform.sendletter.processors.LetterUploadProcessor;
import uk.gov.hmcts.reform.sendletter.processors.XeroxCSVProcessor;

public class ProcessLettersTask {

    private final ILetterProcessor[] processors = new ILetterProcessor[] {
        new LetterUploadProcessor(),
        new XeroxCSVProcessor(),
        new FailedLetterProcessor()
    };

    private LetterRepository repository;

    public ProcessLettersTask(LetterRepository repository) {
        this.repository = repository;
    }

    // Executed regularly by a scheduled task.
    public void run() {
        for (ILetterProcessor processor : processors) {
            try {
                processor.process(repository);
            } catch (RuntimeException r) {
                // Lots of logging.
                // RuntimeException is caught because we don't want a failure
                // in one processor stopping the others running.
            }
        }
    }
}
