package com.wyona.katie.models.squad;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SQuAD implements Serializable {

    private String version;
    private ArrayList<Topic> data;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SQuAD() {
        data = new ArrayList<Topic>();
    }

    /**
     * @return version of SQuAD format, e.g. "v1.1" or "v2.0"
     */
    public String getVersion() {
        return version;
    }

    /**
     *
     */
    public Topic[] getData() {
        return data.toArray(new Topic[0]);
    }
}
