package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
@TestPropertySource("classpath:application.properties")
public abstract class SmokeTestSuite {

    @Value("${test-url}")
    protected String testUrl;

    @Value("${s2s-url}")
    protected String s2sUrl;

    @Value("${s2s-name}")
    protected String s2sName;

    @Value("${s2s-secret}")
    protected String s2sSecret;

    /**
     * Sign in to s2s.
     * @return s2s JWT token.
     */
    protected String signIn() {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", this.s2sName,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(this.s2sSecret)
        );

        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.s2sUrl)
            .body(params)
            .post("/lease")
            .andReturn()
            .getBody()
            .print();
    }

    protected String sampleLetterJson() throws JSONException {
        return new JSONObject()
            .put("type", "smoke_test")
            .put("documents", new JSONArray()
                .put(new JSONObject()
                    .put("template", "some_template")
                    .put("values", new JSONObject()
                        .put("a", "b")
                        .put("uuid", UUID.randomUUID().toString()) // so that we don't send the same letter twice
                    )
                )
            ).toString();
    }
}
