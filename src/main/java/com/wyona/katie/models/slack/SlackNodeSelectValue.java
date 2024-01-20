package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackNodeSelectValue {

    private String value;

    /**
     *
     */
    public SlackNodeSelectValue() {
    }

    /**
     *
     */
    public void setSelected_channel(String value) {
        this.value = value;
    }

    /**
     *
     */
    public String getSelected_channel() {
        return value;
    }
}
