package com.wyona.katie.models.faq;
  
import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Icon implements Serializable {

    private String code;
    private String name;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Icon() {
    }

    /**
     *
     */
    public Icon(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     *
     */
    public String getCode() {
        return code;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }
}
