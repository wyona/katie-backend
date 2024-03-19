package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * https://opennlp.apache.org/docs/1.7.2/manual/opennlp.html#tools.doccat
 * https://blog.datumbox.com/machine-learning-tutorial-the-max-entropy-text-classifier/
 */
@Slf4j
@Component
public class MulticlassTextClassifierMaximumEntropyImpl implements MulticlassTextClassifier {

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#predictLabels(Context, String) 
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception {
        return null;
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        // TODO
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#retrain(Context)
     */
    public void retrain(Context domain) throws Exception {
        log.info("TODO: Retrain");
    }
}
