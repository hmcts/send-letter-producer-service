package uk.gov.hmcts.reform.sendletter.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class WithServiceName<T> {

    @JsonUnwrapped
    public final T obj;

    public final String service;

    public WithServiceName(T obj, String service) {
        this.obj = obj;
        this.service = service;
    }
}
