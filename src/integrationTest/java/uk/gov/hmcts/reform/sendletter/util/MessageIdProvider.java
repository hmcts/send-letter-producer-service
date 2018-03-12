package uk.gov.hmcts.reform.sendletter.util;

import java.util.UUID;

@SuppressWarnings({"squid:S1118", "HideUtilityClassConstructor"})
public final class MessageIdProvider {

    public static String randomMessageId() {
        return UUID.randomUUID().toString();
    }
}
