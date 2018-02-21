package uk.gov.hmcts.reform.sendletter;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

import java.util.UUID;

import static java.util.Collections.singletonList;

public final class SampleData {

    public static Letter letter() {

        return new Letter(
            singletonList(new Document(
                uuid(),
                ImmutableMap.of(
                    "key1", uuid(),
                    "key2", uuid()
                )
            )),
            uuid(),
            ImmutableMap.of(
                "caseId", "12345",
                "documentType", "counter-claim"
            )
        );
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    private SampleData() {
    }
}
