package com.wyona.katie.answers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Add to a Katie domain a QnA with an answer containing <p><ak-exec>com.wyona.askkatie.answers.OpenERZ#getCalendarPaper(ak-entity:number,'de')</ak-exec></p> or <p><ak-exec>com.wyona.askkatie.answers.OpenERZ#getCalendarCardboard(ak-entity:number,'de')</ak-exec></p>
 * IMPORTANT: Make sure that NER can recognize numbers, like for example set <ner impl="MOCK"/> inside domain configuration
 */
@Slf4j
public class OpenERZ {

    private String baseUrl = "https://openerz.metaodi.ch";

    /**
     *
     */
    public OpenERZ() {
    }

    /**
     * @param zip ZIP code, e.g. "8044"
     * @param language Language of answer, e.g. "de" or "en"
     */
    public String getCalendarPaper(String zip, String language) {
        if (zip == null) {
            return "Um die Frage beantworten zu können wird eine gültige Postleitzahl benötigt. Bitte probieren Sie es nochmals, z.B. stellen Sie die Frage 'Nächste Papiersammlung 8032?'";
        }

        List<Date> dates = getDates(zip, "paper");

        Locale locale = new Locale(language);
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("dd. MMMM yyyy", locale);

        StringBuilder answer = new StringBuilder();
        if (dates.size() > 0) {
            //answer.append("<p>Die Abholtermine für Papier an der TODO:Strasse in Zürich (Postleitzahl " + zip + ") sind wie folgt:</p>");
            answer.append("<p>Die Abholtermine für Papier in " + zip + " Zürich sind wie folgt:</p>");
            answer.append("<ul>");
            for (Date date : dates) {
                answer.append("<li>" + dateFormat.format(date) + "</li>"); // INFO: 14. Juli 2023
            }
            answer.append("</ul>");
        } else {
            answer.append("<p>Leider konnten keine Abholtermine für Papier in " + zip + " Zürich gefunden werden.</p>");
        }
        return answer.toString();
    }

    /**
     * @param zip ZIP code, e.g. "8044"
     * @param language Language of answer, e.g. "de" or "en"
     */
    public String getCalendarCardboard(String zip, String language) {
        if (zip == null) {
            return "Um die Frage beantworten zu können wird eine gültige Postleitzahl benötigt. Bitte probieren Sie es nochmals, z.B. stellen Sie die Frage 'Nächste Kartonsammlung 8032?'";
        }

        List<Date> dates = getDates(zip, "cardboard");

        Locale locale = new Locale(language);
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("dd. MMMM yyyy", locale);

        StringBuilder answer = new StringBuilder();
        if (dates.size() > 0) {
            //answer.append("<p>Die Abholtermine für Karton an der TODO:Strasse in Zürich (Postleitzahl " + zip + ") sind wie folgt:</p>");
            answer.append("<p>Die Abholtermine für Karton in " + zip + " Zürich sind wie folgt:</p>");
            answer.append("<ul>");
            for (Date date : dates) {
                answer.append("<li>" + dateFormat.format(date) + "</li>"); // INFO: 14. Juli 2023
            }
            answer.append("</ul>");
        } else {
            answer.append("<p>Leider konnten keine Abholtermine für Karton in " + zip + " Zürich gefunden werden.</p>");
        }

        return answer.toString();
    }

    /**
     * @param zip ZIP code, e.g. "8044"
     * @param type Waste type, e.g. "paper" or "cardboard"
     */
    private List<Date> getDates(String zip, String type) {
        List<Date> dates = new ArrayList<Date>();

        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");

        // INFO: See https://openerz.metaodi.ch/documentation
        Date current = new Date();
        String requestUrl = baseUrl + "/api/calendar.json?types=" + type + "&zip=" + zip + "&start=" + dateFormat.format(current) + "&sort=date&offset=0&limit=500";
        log.info("Get dates from '" + requestUrl + "' ...");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Accept", "application/json");
        //headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            JsonNode resultNode = bodyNode.get("result");
            if (resultNode.isArray()) {
                for (int i = 0; i < resultNode.size(); i++) {
                    JsonNode dateNode = resultNode.get(i);
                    String dateAsString = dateNode.get("date").asText(); // INFO: 2023-07-04"
                    try {
                        dates.add(dateFormat.parse(dateAsString));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } else {
                log.error("No dates received!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return dates;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        //headers.set("Content-Type", "application/json; charset=UTF-8");
        return headers;
    }
}
