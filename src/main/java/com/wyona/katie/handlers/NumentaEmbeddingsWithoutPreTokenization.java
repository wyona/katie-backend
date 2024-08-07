package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import com.wyona.katie.models.Vector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// https://github.com/triton-inference-server/client/tree/main/src/java
// git clone https://github.com/triton-inference-server/client.git
// cd client/src/java
// mvn clean install -Ddir=examples
// ls /Users/michaelwechner/.m2/repository/triton/client/java-api/0.0.1/java-api-0.0.1.jar
// See pom.xml
import triton.client.InferInput;
import triton.client.InferRequestedOutput;
import triton.client.InferResult;
import triton.client.InferenceServerClient;
import triton.client.pojo.DataType;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Get embeddings from Numenta https://www.numenta.com/
 */
@Slf4j
@Component
public class NumentaEmbeddingsWithoutPreTokenization implements EmbeddingsProvider {

    @Value("${numenta.host}")
    private String host;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) {
        log.info("Get embedding from Numenta (Model: " + model + ") for sentence '" + sentence + "' ...");

        Vector vector = null;

        try {
            int numberOfSentences = 1;

            InferInput inputText = new InferInput("TEXT", new long[] {numberOfSentences}, DataType.BYTES);
            String[] inputData = new String[1];
            inputData[0] = sentence;
            inputText.setData(inputData, true); // TODO: true or false?

            List<InferInput> inputs = Lists.newArrayList(inputText);

            List<InferRequestedOutput> outputs = Lists.newArrayList(new InferRequestedOutput("encodings", true)); // TODO: true or false?

            // INFO: POST /v2/models/numenta_optimized_bert_v3/infer
            log.info("Init inference client with endpoint '" + host + "' ...");
            InferenceServerClient client = new InferenceServerClient(host, 5000, 5000);
            log.info("Run inference with model '" + model + "' ...");
            InferResult result = client.infer(model, inputs, outputs);
            vector = new FloatVector(result.getOutputAsFloat("encodings"));
            client.close();

            //log.info(vector.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return vector;
    }
}
