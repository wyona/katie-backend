package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackNodeRelevanturl {

    private SlackNodeInputValue inputValue;

    /**
     *
     */
    public SlackNodeRelevanturl() {
    }

    /**
     *
     */
    public void setSingle_line_input(SlackNodeInputValue inputValue) {
        this.inputValue = inputValue;
    }

    /**
     *
     */
    public SlackNodeInputValue getSingle_line_input() {
        return inputValue;
    }
}
