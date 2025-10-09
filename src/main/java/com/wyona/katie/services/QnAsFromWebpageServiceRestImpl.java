package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.Answer;
import com.wyona.katie.models.ContentType;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.QnA;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * REST implementation of QnAs from web page service
 */
@Slf4j
@Component
public class QnAsFromWebpageServiceRestImpl implements QnAsFromWebpageService {

    @Autowired
    private MailerService mailerService;

    @Value("${qnasfwp.rest.impl.host}")
    private String host;

    private final int connectTimeout = 3000; // INFO: in milliseconds
    private final int readTimeout = 240000; // INFO: in milliseconds
    //private final int readTimeout = 5000; // INFO: in milliseconds

    /**
     * @see QnAsFromWebpageService#getQnAs(URI, Context)
     */
    public QnA[] getQnAs(URI url, Context domain) {
        log.info("Get QnAs from web page '" + url + "' using REST implementation '" + host + "' ...");

        StringBuilder body = new StringBuilder("{\"webpage-url\": "+ "\""+ url + "\"}");

        log.info("Request body: " + body);

        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String requestUrl = host + "/api/v1/find/html";
        log.info("Try to get QnAs from web page: " + requestUrl);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            /*
{
  "qnas": [
    {
      "answer": [
        "<p>There is no legal obligation to allow employees to have a home office. If it is possible to carry out activities well from your home office, you should fulfil your duty of care as an employer and encourage your employees to work from home.</p>"
      ],
      "question": "<h3>My employee refuses to come to work because he is afraid of infection with the coronavirus. He does not belong to a risk group and is single. Is he allowed to demand home office from me if I can guarantee the FOPH's hygiene and distance regulations in the office?</h3>"
    },
    {
      "answer": [
        "<p>The Federal Council has decided that notice of termination is only permitted after 90 days, not 30 days as was previously the case. However, this only applies if the rent and incidental expenses are due between 13 March and 31 May and you are in arrears due to measures ordered by the authorities.</p>"
      ],
      "question": "<h3>My landlord wants to terminate my business premises because I cannot pay the rent. Is he allowed to do that?</h3>"
    },
    {
      "answer": [
        "<p>Yes. The bank has two options Covid-19 or Covid-19 Plus. The federal government provides the guarantee for these types of credit. The Covid-19 loan is up to CHF 500,000, there are no fees, the interest rate is 0.0% and it runs for five years. The maximum contribution of the credit amount is 10% of the turnover of the last year. Covid-19 Plus is available for higher amounts from CHF 500,000 to a maximum of CHF 19.5 million. The verification and the credit application are somewhat more complex.<br/>\r\nThe Confederation bears 85% of the risk, the remaining 15% is borne by the bank. The same conditions apply except for interest rates. The interest rate is 0.5% for the bank's guarantee, while the bank sets the interest rate for the 15%. The forms for applying for a Covid-19 or Covid-19 Plus loan and further information on the loans are available on the website of the Federal Department of Finance at Bridge loans for companies.</p>"
      ],
      "question": "<h3>I cannot pay my supplier because I lack liquid funds. Can I get a reduced credit from my house bank to pay the supplier?</h3>"
    },
    {
      "answer": [
        "<p>In view of the current situation, some simplifications have been made in registering for short-time working so that employers can be helped quickly and easily.</p>",
        "<ol>\n<li>The employees' consent can be obtained via the employer, i.e. if the employer confirms that the employees agree, this is sufficient.</li>\n<li>The pre-notification period for short-time work has been lifted for pre-notifications submitted by the end of May. However, the pre-notification must still be submitted to the competent cantonal office.</li>\n<li>The justification for short-time work can be kept shorter in the pre-notification as long as it is credible.</li>\n<li>The settlement of short-time work compensation is simplified. Only one form with five details is required.</li>\n<li>Existing overtime does not have to be reduced before short-time work compensation is paid.</li>\n</ol>"
      ],
      "question": "<h3>How much effort does my company need to register short-time work?</h3>"
    },
    {
      "answer": [
        "<p>With an income of CHF 5000 gross salary, the employee receives CHF 2500 from the employer for the 50% workload. The lost hours of 50% are also covered by the short-time working compensation at 80%, which corresponds to CHF 2000. This means that the employee receives CHF 4500 of his salary. SECO has provided more detailed <a href=\"https://www.arbeit.swiss/secoalv/de/home/service/publikationen/broschueren.html\">brochures and flyers on this topic.</a></p>"
      ],
      "question": "<h3>Due to the crisis, I have to introduce short-time working for my company. I have to reduce the workload of my employees to 50%. How high is the share of short-time work compensation?</h3>"
    }
  ],
  "version": "1.0",
  "website_string": "https://www.myright.ch/en/business/legal-tips/corona-companies/corona-effects-sme"
}
             */

            List<QnA> qnas = new ArrayList<QnA>();
            for (JsonNode qna: bodyNode.get("qnas")) {
                String question = Utils.stripHTML(qna.get("question").asText(), false, false);
                StringBuilder answer = new StringBuilder();
                for (JsonNode para: qna.get("answer")) {
                    answer.append(para.asText());
                }
                log.info("QnA Question: " + question);
                ContentType contentType = null;
                Answer _answer = new Answer(null, answer.toString(), contentType, null, null, null, null, null, null, null, null, null, question, null,false, null, false, null);
                qnas.add(new QnA(_answer));
            }

            if (qnas.size() == 0) {
                return null;
            } else {
                return qnas.toArray(new QnA[0]);
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else if (e.getRawStatusCode() == 400) {
                log.error(e.getMessage(), e);
            } else {
                log.error(e.getMessage(), e);
            }
            mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            //return Optional.empty();
            return null;
        } catch(ResourceAccessException e) {
            // INFO: Both timeout and not reachable exceptions are handled as ResourceAccessException by restTemplate
            log.error(e.getMessage(), e);
            if (e.getMessage().contains("Read timed out")) {
                log.info("Configured connect timeout in milliseconds: " + connectTimeout);
                log.info("Configured read timeout in milliseconds: " + readTimeout);
                mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body + "\n\nConfigured read timeout in milliseconds:\n\n" + readTimeout, null, false);
            } else {
                mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            }
            // TODO: The method isAlive(String) should also detect when the Question Classifier is down
            //return Optional.empty();
            return null;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            mailerService.notifyAdministrator("ERROR: Analyzing message by QuestionClassifierRestImpl failed ", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            //return Optional.empty();
            return null;
        }
    }

    /**
     * To configure a request timeout when querying a web service
     * @return clientHttpRequestFactory with timeout of 3000ms
     */
    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();

        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);

        return factory;
    }

    /**
     * Check whether Answer from Text service is alive
     * @param endpoint Health endpoint, e.g. "/api/v1/health"
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = host + endpoint;
            log.info("Check whether Answer from Text service is alive: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            if (bodyNode.get("status").asText().equals("UP")) {
                return true;
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        log.warn("Answer from Text service '" + host + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The Answer from Text Service at '" + host + "' seems to be DOWN", null, null, false);
        return false;
    }

    /**
     * Get http headers
     * @return HttpHeaders
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return headers;
    }
}
