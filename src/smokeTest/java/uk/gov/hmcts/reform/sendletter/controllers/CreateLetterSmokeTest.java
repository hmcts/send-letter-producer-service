package uk.gov.hmcts.reform.sendletter.controllers;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class CreateLetterSmokeTest extends SmokeTestSuite {

    @Test
    public void should_create_letter_successfully() throws Exception {

        String jwt = signIn();

        String id = givenJwt(jwt)
            .and()
            .body(sampleLetterJson())
            .when()
            .post("/letters")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath()
            .get("letter_id");

        givenJwt(jwt)
            .when()
            .get("/letters/" + id)
            .then()
            .statusCode(200);
    }

    private RequestSpecification givenJwt(String jwt) {
        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", "Bearer " + jwt);
    }
}
