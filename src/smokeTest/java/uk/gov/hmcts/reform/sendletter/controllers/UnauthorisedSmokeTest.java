package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.UUID;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class UnauthorisedSmokeTest extends SmokeTestSuite {

    private static final String LETTER_ID = UUID.randomUUID().toString();

    private String createLetterBody;

    @Before
    public void setup() throws IOException {

        createLetterBody = Resources.toString(Resources.getResource("letter.json"), Charsets.UTF_8);
    }

    @Test
    public void must_have_authorisation_header_for_all_endpoints() {
        RequestSpecification specification = RestAssured.given()
            .baseUri(this.testUrl)
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/is-failed").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/sent-to-print-at").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/printed-at").then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void should_not_authorise_with_bad_authorisation_token() {
        RequestSpecification specification = RestAssured.given()
            .baseUri(this.testUrl)
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(new Header("ServiceAuthorization", "invalid token"))
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/is-failed").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/sent-to-print-at").then().statusCode(SC_UNAUTHORIZED);
        specification.put("/letters/" + LETTER_ID + "/printed-at").then().statusCode(SC_UNAUTHORIZED);
    }
}
