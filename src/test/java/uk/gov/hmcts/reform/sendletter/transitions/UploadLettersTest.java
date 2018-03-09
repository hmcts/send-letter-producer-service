package uk.gov.hmcts.reform.sendletter.transitions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.entity.LetterTest;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class UploadLettersTest {

    @Autowired
    private LetterRepository repository;

    @Before
    public void setup() {
        repository.save(LetterTest.testLetter);
    }

    @Test
    public void finds_created_letters() {
        int created = repository.findByState(LetterState.Created).size();
        assertThat(created).isEqualTo(1);
        int uploaded = repository.findByState(LetterState.Uploaded).size();
        assertThat(uploaded).isEqualTo(0);
    }
}
