package uk.gov.hmcts.reform.sendletter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "servicebus.connectionString=servicebusurl",
    "servicebus.queue.name=queuename"
})
public class ApplicationTest extends FunSuite {

    @Test
    public void contextLoads() {
        // context load
    }
}
