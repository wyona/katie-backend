package com.wyona.katie.services;

import com.wyona.katie.models.EmbeddingValueType;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.KnnVectorsReader;
import org.apache.lucene.codecs.KnnVectorsWriter;
import org.apache.lucene.backward_codecs.lucene99.Lucene99Codec;
import org.apache.lucene.codecs.lucene101.Lucene101Codec;
import org.apache.lucene.codecs.lucene102.Lucene102HnswBinaryQuantizedVectorsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99HnswScalarQuantizedVectorsFormat;
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
    public Codec getCodec(EmbeddingValueType valueType) {
        //return Lucene95Codec.getDefault();

        log.info("Get codec ...");

        if (valueType == EmbeddingValueType.int8) {
            Codec codecInt8 = new Lucene99Codec() {
                @Override
                public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
                    var delegate = new Lucene99HnswScalarQuantizedVectorsFormat();
                    log.info("Vector Value Type: int8, Maximum Vector Dimension: " + maxDimensions);
                    return new DelegatingKnnVectorsFormat(delegate, maxDimensions);
                }
            };
            return codecInt8;
        } else {
            Codec codecFloat32 = new Lucene101Codec() {
                @Override
                public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
                    var delegate = new Lucene102HnswBinaryQuantizedVectorsFormat();
                    log.info("Vector Value Type: float32, Maximum Vector Dimension: " + maxDimensions);
                    return new DelegatingKnnVectorsFormat(delegate, maxDimensions);
                }
            };
            return codecFloat32;
        }
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
