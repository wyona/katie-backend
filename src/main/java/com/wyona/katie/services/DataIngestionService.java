package com.wyona.katie.services;

import com.wyona.katie.models.TextSplitterImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class DataIngestionService {

    @Autowired
    SegmentationService segmentationService;

    /**
     * Split PDF into text chunks
     * @param file PDF file
     * @return text chunks
     */
    public List<String> splitPDFIntoChunks(File file, TextSplitterImpl textSplitterImpl) throws Exception {
        InputStream in = new FileInputStream(file);
        List<String> chunks = splitPDFIntoChunks(in, textSplitterImpl);
        log.info("Number of chunks extracted from PDF document '" + file.getName() + "': " + chunks.size());
        return chunks;
    }

    /**
     * Split PDF into text chunks
     * @param in PDF as input stream
     * @return text chunks
     */
    public List<String> splitPDFIntoChunks(InputStream in, TextSplitterImpl textSplitterImpl) throws Exception {
        PDDocument pdDoc = PDDocument.load(in);
        String body = new PDFTextStripper().getText(pdDoc);
        pdDoc.close();

        List<String> chunks = segmentationService.getSegments(body, '\n', 2000, 100);
        if (textSplitterImpl.equals(TextSplitterImpl.SENTENCE)) {
            chunks = segmentationService.splitBySentences(body, "en", 700, true);
        } else if (textSplitterImpl.equals(TextSplitterImpl.AI21)) {
            chunks = segmentationService.getSegmentsUsingAI21(body);
        } else if (textSplitterImpl.equals(TextSplitterImpl.FIXED_SIZE)) {
            chunks = segmentationService.getSegments(body, '\n', 2000, 100);
        } else {
            log.error("No such text splitter implementation '" + textSplitterImpl + "'! Use fixed size text splitter ...");
            chunks = segmentationService.getSegments(body, '\n', 2000, 100);
        }
        return chunks;
    }

    /**
     * Anthropic-style chunk contextualization (https://www.anthropic.com/engineering/contextual-retrieval)
     * @param chunk Original chunk text, e.g., "The company's revenue grew by 3% over the previous quarter."
     * @return Contextualized chunk text, e.g., "This chunk is from an SEC filing on ACME Corp's performance in Q2 2023. The company's revenue grew by 3% over the previous quarter.""
     */
    private String contextualizeChunk(String chunk) {
        String PROMPT = "<document_title>\n" +
                "{title}\n" +
                "</document_title>\n" +
                "\n" +
                "<document_description>\n" +
                "{document_description}\n" +
                "</document_description>\n" +
                "\n" +
                "<section_path>\n" +
                "{section_path}\n" +
                "</section_path>\n" +
                "\n" +
                "<previous_chunk>\n" +
                "{previous_chunk}\n" +
                "</previous_chunk>\n" +
                "\n" +
                "<chunk>\n" +
                "{chunk}\n" +
                "</chunk>\n" +
                "\n" +
                "<next_chunk>\n" +
                "{next_chunk}\n" +
                "</next_chunk>\n" +
                "\n" +
                "Task:\n" +
                "\n" +
                "Generate 1-3 concise sentences that help this chunk be understood when retrieved independently.\n" +
                "\n" +
                "Include:\n" +
                "- where the chunk appears in the document\n" +
                "- resolution of ambiguous references, pronouns, and entities\n" +
                "- the broader topic being discussed\n" +
                "\n" +
                "Do not summarize the chunk itself.\n" +
                "Do not repeat information already explicit in the chunk.\n" +
                "Write in the same language as the chunk.\n" +
                "\n" +
                "Output only the context text.";

        return chunk;
    }
}
