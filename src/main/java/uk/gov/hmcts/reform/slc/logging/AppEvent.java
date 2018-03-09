package uk.gov.hmcts.reform.slc.logging;

final class AppEvent {

    /**
     * Message received from service bus queue.
     */
    static final String MESSAGE_RECEIVED = "MessageReceived";

    /**
     * Successful message handler process.
     */
    static final String MESSAGE_HANDLED_SUCCESSFULLY = "MessageHandleSuccess";

    /**
     * Failed message handler process.
     */
    static final String MESSAGE_HANDLED_UNSUCCESSFULLY = "MessageHandleFailure";

    /**
     * Successful message map.
     */
    static final String MESSAGE_MAPPED_SUCCESSFULLY = "MessageMapSuccess";

    /**
     * Attempt to map an empty message.
     */
    static final String MESSAGE_MAPPED_EMPTY = "MessageMapEmpty";

    /**
     * Attempt to map an invalid message..
     */
    static final String MESSAGE_MAPPED_INVALID = "MessageMapInvalid";

    /**
     * Failed message map.
     */
    static final String MESSAGE_MAPPED_UNSUCCESSFULLY = "MessageMapFailure";

    private AppEvent() {
        // utility class constructor
    }
}
