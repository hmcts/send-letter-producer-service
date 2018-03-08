package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class SmokeTestSuite {

    @Value("${test-url:http://localhost:8485}")
    protected String testUrl;
}
