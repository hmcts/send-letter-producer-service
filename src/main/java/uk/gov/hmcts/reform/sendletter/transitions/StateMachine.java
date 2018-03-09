package uk.gov.hmcts.reform.sendletter.transitions;

import com.google.common.collect.ImmutableList;

public class StateMachine {

    private final ImmutableList<StateTransition> transitions = ImmutableList.of(
        new UploadLetter(),
        new XeroxCSVProcessor()
    );

}
