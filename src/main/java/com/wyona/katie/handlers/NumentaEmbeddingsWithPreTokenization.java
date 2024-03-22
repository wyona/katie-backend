package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import com.wyona.katie.models.Vector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;

/**
 * Get embeddings from Numenta https://www.numenta.com/
 */
@Slf4j
@Component
public class NumentaEmbeddingsWithPreTokenization implements EmbeddingsProvider {

    @Value("${numenta.host}")
    private String host;

    @Value("${numenta.tokenizer}")
    private String numentaTokenizerName;

    //@Value("${numenta.max_seq_len}")
    //private String max_seq_len;

    private final int maxTokensLen = 64;

    HuggingFaceTokenizer tokenizer;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) {
        log.info("Get embedding from Numenta (Model: " + model + ") for sentence '" + sentence + "' ...");

        Vector vector = null;

        try {
            boolean isBinary = true;

            int numberOfSentences = 1;
            // https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
            int maxSeqLen = 64;
            // Is this the same like maxTokensLen ?

            // https://docs.djl.ai/jupyter/pytorch/load_your_own_pytorch_bert.html
            // https://huggingface.co/transformers/v3.0.2/main_classes/tokenizer.html

            Encoding encoding = getEncoding(sentence);

            // INFO: Prepare the tokens
            InferInput inputIds = new InferInput("input_ids", new long[] {numberOfSentences, maxSeqLen}, DataType.INT32);
            //int[] inputIdsData = getMockInputIds(sentence);
            int[] inputIdsData = longToInt(encoding.getIds());
            inputIds.setData(inputIdsData, isBinary);

            // INFO: Prepare the attention mask
            InferInput inputMask = new InferInput("input_mask", new long[] {numberOfSentences, maxSeqLen}, DataType.INT32);
            //int[] inputMaskData = getMockInputMask(sentence);
            int[] inputMaskData = longToInt(encoding.getAttentionMask());
            inputMask.setData(inputMaskData, isBinary);

            // INFO: Prepare the segment IDs
            InferInput segmentIds = new InferInput("segment_ids", new long[] {numberOfSentences, maxSeqLen}, DataType.INT32);
            int[] segmentIdsData = getMockSegmentIds(sentence);
            segmentIds.setData(segmentIdsData, isBinary);

            // INFO: TODO
            List<InferInput> inputs = Lists.newArrayList(inputIds, inputMask, segmentIds);

            List<InferRequestedOutput> outputs = Lists.newArrayList(new InferRequestedOutput("encodings", isBinary));

            // INFO: POST /v2/models/numenta_optimized_bert_v3/infer
            log.info("Init inference client with endpoint '" + host + "' ...");
            InferenceServerClient client = new InferenceServerClient(host, 5000, 5000);
            log.info("Run inference with model '" + model + "' ...");
            InferResult result = client.infer(model, inputs, outputs);
            vector = new FloatVector(result.getOutputAsFloat("encodings"));
            client.close();

            //log.info(Arrays.toString(vector.getValues()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return vector;
    }

    /**
     *
     */
    private int[] longToInt(long[] longValues) {
        int[] intValues = new int[longValues.length];
        for (int i = 0; i < longValues.length; i++) {
            intValues[i] = (int) longValues[i];
        }
        return intValues;
    }

    /**
     *
     */
    private Encoding getEncoding(String sentence) throws Exception {
        HuggingFaceTokenizer tokenizer = getTokenizer(numentaTokenizerName);

        Encoding encoding = tokenizer.encode(sentence);

        String[] tokens = encoding.getTokens();
        StringBuilder tokensStr = new StringBuilder("[");
        for (int i = 0; i < tokens.length; i++) {
            tokensStr.append(tokens[i]);
            if (i < tokens.length - 1) {
                tokensStr.append(", ");
            }
        }
        tokensStr.append("]");
        log.info("Tokens: " + tokensStr);

        long[] attentionMask = encoding.getAttentionMask();
        log.info("Attention mask: " + arrayToString(attentionMask));

        long[] ids = encoding.getIds();
        log.info("IDs: " + arrayToString(ids));

        return encoding;
    }

    /**
     * https://huggingface.co/bert-base-cased
     * @param tokenizerName Tokenizer name, e.g. "bert-base-cased"
     */
    public HuggingFaceTokenizer getTokenizer(String tokenizerName) throws Exception {
        // INFO: Run tokenizer as singleton
        if (tokenizer == null) {
            String tokenizerJsonPath = "hugging-face/" + tokenizerName + "/tokenizer.json";
            log.info("Init Hugging Face tokenizer: " + tokenizerJsonPath);
            Map<String, String> options = new HashMap<String, String>();
            options.put("maxLength", "" + maxTokensLen);
            options.put("padding", "MAX_LENGTH");
            tokenizer = HuggingFaceTokenizer.newInstance(new ClassPathResource(tokenizerJsonPath).getInputStream(), options);
            //tokenizer = HuggingFaceTokenizer.builder().optTokenizerName(tokenizerName).optMaxLength(maxTokensLen).optPadToMaxLength().build();
        } else {
            log.info("Hugging Face tokenizer already initialized.");
        }
        return tokenizer;
    }

    /**
     *
     */
    private String arrayToString(long[] values) {
        StringBuilder str = new StringBuilder("[");

        for (int i = 0; i < values.length; i++) {
            str.append(values[i]);
            if (i < values.length - 1) {
                str.append(", ");
            }
        }
        str.append("]");

        return str.toString();
    }

    /**
     *
     */
    private int[] getMockInputIds(String sentence) {
        //int[] inputIdsData = new int[maxTokensLen];
        //Arrays.fill(inputIdsData, 1); // TODO: fill with some data.

        int[] inputIdsData = {101, 1731, 1169, 146, 14247, 2217, 1103, 9155, 7230, 2593, 136, 102,
                              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                              0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                              0, 0, 0, 0};

        // TODO: 28996 out of range!
        int[] inputIdsData2 = {101, 1731, 1169, 146, 28996, 1103, 9155, 7230, 2593, 136, 102, 102,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0};

        return inputIdsData;
    }

    /**
     *
     */
    private int[] getMockInputMask(String sentence) {
        //int[] inputMaskData = new int[maxTokensLen];
        //Arrays.fill(inputMaskData, 1); // TODO: fill with some data.

        // INFO: Number of 1s is the number of input Ids which are not zero (see above)
        int[] inputMaskData = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                               0, 0, 0, 0};

        int[] inputMaskData2 = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                0, 0, 0, 0};
        return inputMaskData;
    }

    /**
     *
     */
    private int[] getMockSegmentIds(String sentence) {
        int[] segmentIdsData = new int[maxTokensLen];
        Arrays.fill(segmentIdsData, 0); // TODO: fill with some data
        /*
        [0 0 0 0 0 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 0 0
         0 0 0 0 0 0 0 0 0 0 0 0
         0 0 0 0]

         */
        return segmentIdsData;
    }
}
