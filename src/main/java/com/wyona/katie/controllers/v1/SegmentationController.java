package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.SegmentationService;
import com.wyona.katie.services.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

/**
 * Controller to chunk text into smaller pieces
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/segmentation")
public class SegmentationController {

    @Autowired
    private ContextService domainService;

    @Autowired
    private SegmentationService segmentationService;

    private static final String QUESTION = "question";
    private static final String URL = "url";
    private static final String ANSWER = "answer";

    /**
     * REST interface to chunk Akoma Ntoso XML into Katie import JSON
     * http://www.akomantoso.org/
     * https://www.fedlex.admin.ch/eli/cc/1998/892_892_892/en
     * https://www.fedlex.admin.ch/filestore/fedlex.data.admin.ch/eli/cc/1998/892_892_892/20230123/en/xml/fedlex-data-admin-ch-eli-cc-1998-892_892_892-20230123-en-xml-1.xml
     */
    @RequestMapping(value = "/akoma-ntoso", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Convert Akoma Ntoso XML into Katie import JSON")
    public ResponseEntity<?> convertAkomaNtoso(
            @Parameter(name = "domainId", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')" ,required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @Parameter(name = "source-url", description = "Source URL, e.g. 'https://www.fedlex.admin.ch/eli/cc/1998/892_892_892/en#' or 'https://www.fedlex.admin.ch/eli/cc/2022/491/de#'" ,required = true)
            @RequestParam(value = "source-url", required = true) String sourceUrl,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        if (!domainService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            log.info("Convert Akoma Ntoso XML into Katie import JSON ...");

            InputStream in = file.getInputStream();
            Document document = read(in);
            in.close();

            ArrayNode katieImportJSON = segmentationService.chunkAkomaNtoso(document, sourceUrl);

            //Utils.saveText(body.toString(), txtFile);

            return new ResponseEntity<>(katieImportJSON.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to split text into chunks using AI21 segmentation service
     * https://docs.ai21.com/docs/text-segmentation-api
     */
    @RequestMapping(value = "/ai21", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Split text into chunks using AI21 segmentation service")
    public ResponseEntity<?> getSegments(
            @Parameter(name = "domainId", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')" ,required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @Parameter(name = "source-url", description = "Source URL, e.g. https://www.fedlex.admin.ch/eli/cc/1998/892_892_892/en" ,required = true)
            @RequestParam(value = "source-url", required = true) String sourceUrl,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest requestIn) {

        if (!domainService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            InputStream in = file.getInputStream();
            String text = Utils.convertInputStreamToString(in);
            in.close();
            List<String> chunks = segmentationService.getSegmentsUsingAI21(text);

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode katieImport = mapper.createArrayNode();
            for (String chunk : chunks) {
                ObjectNode answerNode = mapper.createObjectNode();
                answerNode.put(ANSWER, chunk);
                answerNode.put(QUESTION, "TODO");
                answerNode.put(URL, sourceUrl);
                katieImport.add(answerNode);
            }

            return new ResponseEntity<>(katieImport, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to split plain text document into chunks similar to langchain character text splitter (https://python.langchain.com/en/latest/modules/indexes/text_splitters/examples/character_text_splitter.html)
     * TODO: Also consider: https://github.com/run-llama/llama-hub/tree/main/llama_hub/llama_packs/node_parser/semantic_chunking
     * TODO: Semnatic chunking https://www.youtube.com/watch?v=w_veb816Asg
     */
    @RequestMapping(value = "/character", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    //@RequestMapping(value = "/character", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="Split plain text document into chunks similar to langchain character text splitter")
    public ResponseEntity<?> getCharacterTextSplitter(
            @Parameter(name = "domainId", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')" ,required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @Parameter(name = "separator", description = "Separator, e.g. ' ' or '\\n'" ,required = false)
            @RequestParam(value = "separator", required = false) Character separator,
            @Parameter(name = "chunk-size", description = "Chunk size" ,required = true)
            @RequestParam(value = "chunk-size", required = true) Integer chunkSize,
            @Parameter(name = "chunk-overlap", description = "Chunk overlap" ,required = true)
            @RequestParam(value = "chunk-overlap", required = true) Integer chunkOverlap,
            @Parameter(description = "Plain text file to upload", required = true)
            @RequestPart(name = "file", required = true) MultipartFile file,
            HttpServletRequest requestIn) {

        if (!domainService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            InputStream in = file.getInputStream();
            String text = Utils.convertInputStreamToString(in);
            in.close();

            // TODO: Use separator from request argument
            Character _separator = '\n';
            //Character _separator = ' ';
            List<String> chunks = segmentationService.getSegments(text, _separator, chunkSize, chunkOverlap);

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode katieImport = mapper.createArrayNode();
            for (String chunk : chunks) {
                ObjectNode answerNode = mapper.createObjectNode();
                answerNode.put(ANSWER, chunk);
                answerNode.put(QUESTION, "TODO");
                katieImport.add(answerNode);
            }

            return new ResponseEntity<>(katieImport, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Read XML file
     * @param file File containing XML
     * @return DOM
     */
    /*
    private Document read(File file) throws Exception {
        log.info("Read XML from file '" + file.getAbsolutePath() + "' ...");
        FileInputStream in = new FileInputStream(file);
        Document doc = read(in);
        in.close();
        return doc;
    }
     */

    /**
     * Parse input stream containing XML
     * @return DOM
     */
    private Document read(InputStream in) throws Exception {
        log.info("Init document builder factory ...");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        log.info("Init DocumentBuilder ...");
        DocumentBuilder builder = factory.newDocumentBuilder();
        //DocumentBuilder builder = createBuilder(false);

        log.info("Parse input stream ...");
        Document doc = builder.parse(in);
        return doc;
    }
}
