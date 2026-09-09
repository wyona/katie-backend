package com.wyona.katie.controllers.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Index page controller
 */
@Controller
@Tag(name = "Index Page Controller v1", description = "Endpoint for root URL '/'")
@Slf4j
public class IndexPageController {

    @Value("${forward.root}")
    private String forwardRoot;

    @RequestMapping("/")
    public String getIndexPage() {
        if (forwardRoot.equals("index")) {
            log.info("Forward root to 'index.html' ...");
            return "forward:index.html";
        } else if (forwardRoot.equals("swagger-ui")) {
            log.info("Redirect root to 'swagger-ui/' ...");
            return "redirect:swagger-ui/";
        } else {
            log.error("No such forward '" + forwardRoot+ "' supported!");
            return "redirect:swagger-ui/";
        }
    }
}
