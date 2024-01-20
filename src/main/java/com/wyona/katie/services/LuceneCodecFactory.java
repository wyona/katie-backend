package com.wyona.katie.services;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.KnnVectorsReader;
import org.apache.lucene.codecs.KnnVectorsWriter;
//import org.apache.lucene.backward_codecs.lucene95.Lucene95Codec;
//import org.apache.lucene.backward_codecs.lucene95.Lucene95HnswVectorsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99Codec;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Component
public class LuceneCodecFactory {

    private final int maxDimensions = 16384; // TODO: Make configurable

    /**
     *
     */
    public Codec getCodec() {
        //return Lucene95Codec.getDefault();

        log.info("Get codec ...");
        Codec codec = new Lucene99Codec() {
            @Override
            public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
                var delegate = new Lucene99HnswVectorsFormat();
                log.info("Maximum Vector Dimension: " + maxDimensions);
                return new DelegatingKnnVectorsFormat(delegate, maxDimensions);
            }
        };

        return codec;
    }
}

/**
 * This class exists because Lucene95HnswVectorsFormat's getMaxDimensions method is final and we
 * need to workaround that constraint to allow more than the default number of dimensions
 */
@Slf4j
class DelegatingKnnVectorsFormat extends KnnVectorsFormat {
    private final KnnVectorsFormat delegate;
    private final int maxDimensions;

    public DelegatingKnnVectorsFormat(KnnVectorsFormat delegate, int maxDimensions) {
        super(delegate.getName());
        this.delegate = delegate;
        this.maxDimensions = maxDimensions;
    }

    @Override
    public KnnVectorsWriter fieldsWriter(SegmentWriteState state) throws IOException {
        return delegate.fieldsWriter(state);
    }

    @Override
    public KnnVectorsReader fieldsReader(SegmentReadState state) throws IOException {
        return delegate.fieldsReader(state);
    }

    @Override
    public int getMaxDimensions(String fieldName) {
        log.info("Maximum vector dimension: " + maxDimensions);
        return maxDimensions;
    }
}
