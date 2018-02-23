package uk.gov.hmcts.reform.sendletter.controllers;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class SmokeTestSuite {

    @Value("${test-url:http://localhost:8485}")
    private String testUrl;

    @Before
    public void setup() throws IOException {
        RestAssured.baseURI = testUrl;
    }
}
