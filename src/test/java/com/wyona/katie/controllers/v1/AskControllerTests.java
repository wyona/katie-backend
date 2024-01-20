package com.wyona.katie.controllers.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.Test;
//import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * Testing AskController
 */
@WebMvcTest
@Slf4j
public class AskControllerTests {

    @Autowired
    MockMvc mvc;

    /**
     *
     */
    @Test
    public void ask() throws Exception {
        log.info("Test answering questions ...");
        //mvc.perform(get("/api/v1/ask?question=Test")).andExpect(status().isOk());
        /*
            .andExpect(status().isOk())
            .andExpect(content().string(
                is(equalTo("2019-07-29 14:10:53"))));

         */
    }
}
