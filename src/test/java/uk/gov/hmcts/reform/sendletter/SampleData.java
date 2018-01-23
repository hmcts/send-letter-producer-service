package uk.gov.hmcts.reform.sendletter;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.util.UUID;

public final class SampleData {

    public static Letter letter() {
        return new Letter(
            uuid(),
            ImmutableMap.of(
                "key1", uuid(),
                "key2", uuid()
            ),
            uuid()
        );
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    private SampleData() {
    }
}
