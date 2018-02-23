package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.UUID;

public class UnauthorisedSmokeTest extends SmokeTestSuite {

    private static final String LETTER_ID = UUID.randomUUID().toString();

    private String createLetterBody;

    @Before
    @Override
    public void setup() throws IOException {
        super.setup();

        createLetterBody = Resources.toString(Resources.getResource("letter.json"), Charsets.UTF_8);
    }

    @Test
    public void must_have_authorisation_header_for_all_endpoints() {
        RequestSpecification specification = RestAssured.given()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .when();
        int badRequest = HttpStatus.BAD_REQUEST.value();

        specification.get("/letters/" + LETTER_ID).then().statusCode(badRequest);
        specification.body(createLetterBody).post("/letters").then().statusCode(badRequest);
        specification.put("/letters/" + LETTER_ID + "/is-failed").then().statusCode(badRequest);
        specification.put("/letters/" + LETTER_ID + "/sent-to-print-at").then().statusCode(badRequest);
        specification.put("/letters/" + LETTER_ID + "/printed-at").then().statusCode(badRequest);
    }

    @Test
    public void should_not_authorise_with_bad_authorisation_token() {
        RequestSpecification specification = RestAssured.given()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(new Header("ServiceAuthorization", "invalid token"))
            .when();
        int unauthorised = HttpStatus.UNAUTHORIZED.value();

        specification.get("/letters/" + LETTER_ID).then().statusCode(unauthorised);
        specification.body(createLetterBody).post("/letters").then().statusCode(unauthorised);
        specification.put("/letters/" + LETTER_ID + "/is-failed").then().statusCode(unauthorised);
        specification.put("/letters/" + LETTER_ID + "/sent-to-print-at").then().statusCode(unauthorised);
        specification.put("/letters/" + LETTER_ID + "/printed-at").then().statusCode(unauthorised);
    }
}
