package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackViewState {

    private SlackViewStateValues values;

    /**
     *
     */
    public SlackViewState() {
    }

    /**
     *
     */
    public void setValues(SlackViewStateValues values) {
        this.values = values;
    }

    /**
     *
     */
    public SlackViewStateValues getValues() {
        return values;
    }
}
