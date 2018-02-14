package uk.gov.hmcts.reform.sendletter.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.UUID;

public class WithServiceNameAndId<T> {

    @JsonUnwrapped
    public final T obj;

    public final String service;

    public final UUID id;

    public WithServiceNameAndId(T obj, String service, UUID id) {
        this.obj = obj;
        this.service = service;
        this.id = id;
    }
}
