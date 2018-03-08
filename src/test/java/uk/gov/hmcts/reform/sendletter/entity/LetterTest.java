package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LetterTest {

    @Autowired
    private LetterRepository repository;

    @Test
    public void should_successfully_save_report_in_db() {
        Letter l = new Letter("messageId", "service", "{}", "a type");
        repository.save(l);
        int count = (int) repository.count();
        assertThat(count).isEqualTo(1);
    }
}
