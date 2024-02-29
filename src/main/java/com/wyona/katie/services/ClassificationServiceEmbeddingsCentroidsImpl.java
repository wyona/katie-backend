package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.TextItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

/**
 * https://medium.com/@juanc.olamendy/unlocking-the-power-of-text-classification-with-embeddings-7bcbb5912790
 */
@Slf4j
@Component
public class ClassificationServiceEmbeddingsCentroidsImpl implements ClassificationService {

    /**
     * @see com.wyona.katie.services.ClassificationService#predictLabels(Context, String) 
     */
    public String[] predictLabels(Context domain, String text) throws Exception {
        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        indexReader.close();

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

    /**
     * @see com.wyona.katie.services.ClassificationService#train(TextItem[])
     */
    public void train(TextItem[] samples) {
        for (TextItem sample : samples) {
            log.info("TODO: Train Sample: Text: " + sample.getText() + ", Label: " + sample.getLabel());
        }
    }

    /**
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        File indexDir = new File(domain.getContextDirectory(), "lucene-classifications");
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        String indexPath = indexDir.getAbsolutePath();
        return FSDirectory.open(Paths.get(indexPath));
    }
}
