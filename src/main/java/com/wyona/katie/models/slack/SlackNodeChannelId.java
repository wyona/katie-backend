package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackNodeChannelId {

    private SlackNodeSelectValue selectValue;

    /**
     *
     */
    public SlackNodeChannelId() {
    }

    /**
     *
     */
    public void setSelect_id(SlackNodeSelectValue selectValue) {
        this.selectValue = selectValue;
    }

    /**
     *
     */
    public SlackNodeSelectValue getSelect_id() {
        return selectValue;
    }
}
