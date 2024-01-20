package com.wyona.katie.services;

import com.wyona.katie.models.Answer;
import com.wyona.katie.models.ContentType;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.QnA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/*
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import java.io.FileInputStream;
 */

/**
 * Mock implementation of QnAs from web page service
 */
@Slf4j
@Component
public class QnAsFromWebpageServiceMockImpl implements QnAsFromWebpageService {

    @Autowired
    private UtilsService utilsService;

    /**
     * @see QnAsFromWebpageService#getQnAs(URI, Context)
     */
    public QnA[] getQnAs(URI url, Context domain) {
        try {
            File file = utilsService.dumpContent(domain, url, null);

            return extractQnAs(file);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract QnAs
     * @param file File which contains downloaded data
     * @return extracted QnAs
     */
    private QnA[] extractQnAs(File file) throws Exception {
        // TODO: Consider https://github.com/lintool/wikiclean

        // WARNING: Including tika-parsers-standard-package inside pom.xml file creates various runtime issues
        /*
        log.info("TODO: Uncomment Tika parser standard package.");

        log.info("Extract content from downloaded data from file '" + file.getAbsolutePath() + "' ...");
        FileInputStream in = new FileInputStream(file);
        int writeLimit = -1; // INFO: Disable default limit of 100000 characters
        BodyContentHandler handler = new BodyContentHandler(writeLimit);
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        parser.parse(in, handler, metadata, context);
        in.close();

        //log.info("Body: " + handler.toString());

        // TODO: Reconsider adding suffix -tika.txt
        File fileTika = new File(file.getParentFile(), file.getName() + "-tika.txt");
        Utils.saveText(handler.toString().trim(), fileTika, false);
        */

        // TODO: Get paragraphs / QnAs from file instead mock data
        // TODO: https://docs.ai21.com/reference/text-segmentation-api-ref
        List<String> paras = new ArrayList<String>();
        paras.add("Brazil (Portuguese: Brasil; Brazilian Portuguese: [bɾaˈziw]),[nt 1] officially the Federative Republic of Brazil (Portuguese: República Federativa do Brasil),[9] is the largest country in both South America and Latin America. At 8.5 million square kilometers (3,300,000 sq mi)[10] and with over 217 million people, Brazil is the world's fifth-largest country by area and the seventh most populous. Its capital is Brasília, and its most populous city is São Paulo. The federation is composed of the union of the 26 states and the Federal District. It is the largest country to have Portuguese as an official language and the only one in the Americas;[11][12] one of the most multicultural and ethnically diverse nations, due to over a century of mass immigration from around the world;[13] and the most populous Roman Catholic-majority country.");
        paras.add("Bounded by the Atlantic Ocean on the east, Brazil has a coastline of 7,491 kilometers (4,655 mi).[14] It borders all other countries and territories in South America except Ecuador and Chile and covers roughly half of the continent's land area.[15] Its Amazon basin includes a vast tropical forest, home to diverse wildlife, a variety of ecological systems, and extensive natural resources spanning numerous protected habitats.[14] This unique environmental heritage makes Brazil one of 17 megadiverse countries, and is the subject of significant global interest, as environmental degradation through processes like deforestation has direct impacts on global issues like climate change and biodiversity loss.");

        List<QnA> qnas = new ArrayList<QnA>();
        if (paras != null) {
            for (String para: paras) {
                String question = "What is the capital of Brazil"; // WARN: Question must be set, otherwise QnA parser will fail
                Answer answer = new Answer(null, para, ContentType.TEXT_PLAIN, null, null, null, null, null, null, null, null, null, question, null,false, null, false, null);
                qnas.add(new QnA(answer));
            }
            return qnas.toArray(new QnA[0]);
        } else {
            log.warn("No paragraphs found within data '" + file.getAbsolutePath() + "'.");
            return null;
        }
    }
}
