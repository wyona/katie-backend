package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.ai.models.FloatVector;
import com.wyona.katie.ai.models.TextEmbedding;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * https://opennlp.apache.org/docs/1.7.2/manual/opennlp.html#tools.doccat
 * https://blog.datumbox.com/machine-learning-tutorial-the-max-entropy-text-classifier/
 */
@Slf4j
@Component
public class ClassificationServiceMaximumEntropyImpl implements ClassificationService {

    /**
     * @see com.wyona.katie.services.ClassificationService#predictLabels(Context, String) 
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception {
        return null;
    }

    /**
     * @see com.wyona.katie.services.ClassificationService#getDataset(Context, int, int)
     */
    public ClassificationDataset getDataset(Context domain, int offset, int limit) throws Exception {
        return null;
    }

    /**
     * @see com.wyona.katie.services.ClassificationService#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        // TODO
    }
}
