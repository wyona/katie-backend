package com.wyona.katie.models.squad;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Answer implements Serializable {

    private String text;
    private int answer_start;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Answer() {
    }

    /**
     * @param text Answer to question, e.g. "Polish and French"
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     * @param answer_start Position where answers starts inside paragraph, e.g. 182
     */
    public void setAnswer_start(int answer_start) {
        this.answer_start = answer_start;
    }

    /**
     *
     */
    public int getAnswer_start() {
        return answer_start;
    }
}
