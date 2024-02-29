package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service to manage domain specific classifications
 */
@Slf4j
@Component
public class ClassificationServiceEmbeddingsCentroidsImpl implements ClassificationService {

    /**
     * @see com.wyona.katie.services.ClassificationService#predictLabels(Context, String) 
     */
    public String[] predictLabels(Context domain, String text) throws Exception {
        String[] labels = new String[1];
        labels[0] = "not_implemented_yet";
        return labels;
    }

    /**
     * @see com.wyona.katie.services.ClassificationService#getLabels(Context, int, int) 
     */
    public String[] getLabels(Context domain, int offset, int limit) throws Exception {
        return null;
    }
}
