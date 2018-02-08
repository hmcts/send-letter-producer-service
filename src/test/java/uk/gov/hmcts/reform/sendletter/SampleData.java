package uk.gov.hmcts.reform.sendletter;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.sendletter.model.Document;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.util.UUID;

import static java.util.Collections.singletonList;

public final class SampleData {

    public static Letter letter() {

        Document document = new Document(
            uuid(),
            ImmutableMap.of(
                "key1", uuid(),
                "key2", uuid()
            )
        );

        return new Letter(
            singletonList(document),
            uuid()
        );
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    private SampleData() {
    }
}
