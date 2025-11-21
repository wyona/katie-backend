package com.wyona.katie.services;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.wyona.html2mrkdwn.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class Utils {

    /**
     * @param secret Secret, e.g. "3202FGgKalMnVTLOX6AldOcFlmtWdsiUV4Fo2iNuPAx"
     * @return obfuscated secrect, e.g. "3***x"
     */
    public static String obfuscateSecret(String secret) {
        if (secret != null && secret.length() > 0) {
            return secret.charAt(0) + "***" + secret.charAt(secret.length() - 1);
        } else {
            log.warn("Secret is either null or an empty string!");
            return secret;
        }
    }

    /**
     * Convert input stream to string
     * @param in Input stream
     * @return input stream as string
     */
    public static String convertInputStreamToString(InputStream in) {
        try {
            InputStreamReader streamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder strBuilder = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                strBuilder.append(line);
                line = reader.readLine();
                if (line != null) {
                    strBuilder.append("\n");
                }
            }

            reader.close();
            streamReader.close();
            in.close();

            return strBuilder.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Save text to file
     * @param append When true and file exists, then append text to content of file, otherwise replace existing content by text
     */
    public static void saveText(String text, File file, boolean append) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
        writer.write(text);
        writer.close();
    }

    /**
     * @param language Language code, e.g. "de-CH"
     * @return two-letter language code, e.g. "de"
     */
    public static Locale getLocale(String language) {
        return new Locale(language.substring(0, 2)); // TODO: Re-use getTwoLetterCode(String)
    }

    /**
     * @param language Language code, e.g. "de-CH"
     * @return two-letter language code, e.g. "de"
     */
    public static String getTwoLetterCode(String language) {
        if (language.length() > 2) {
            log.info("Try to map language '" + language + "' to two-letter code ...");
        }
        return language.substring(0, 2);
    }

    /**
     * @return true when language is valid and false otherwise
     */
    public static boolean isLanguageValid(String language) {
        if (language.length() == 2) { // TODO: Also check that language is according to https://de.wikipedia.org/wiki/Liste_der_ISO-639-1-Codes
            return true;
        } else {
            return false;
        }
    }

    /**
     * Escape all double quotes
     * @param s Some string cointaining double quotes, for example 'Who is "Arsène Lupin"?'
     * @return string with escaped double quotes, for example 'Who is \"Arsène Lupin\"?'
     */
    public static String escapeDoubleQuotes(String s) {
        return s.replaceAll("\"", "\\\\\"");
    }

    /**
     * Escape all backslashes
     */
    public static String escapeBackslashes(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\");
    }

    /**
     * Remove double quotes
     * @param s Some string cointaining double quotes, for example 'Who is "Arsène Lupin"?'
     * @return string without double quotes, for example 'Who is Arsène Lupin?'
     */
    public static String removeDoubleQuotes(String s) {
        return s.replaceAll("\"", "");
    }

    /**
     * Remove all backslashes
     * @param s Some string containing backslashes, for example {"action":"startup","error":"init modules: init module 1 (\"backup-filesystem\"): init backup backend: relative backup path provided","level":"fatal","msg":"modules didn't initialize","time":"2022-09-29T13:53:57Z"}
     * @return string without backslashes, for example ... ("backup-filesystem") ...
     */
    public static String removeBackslashes(String s) {
        return s.replaceAll("\\\\", "");
    }

    /**
     * Escape various characters
     * INFO: https://www.baeldung.com/java-json-escaping https://jsonlint.com/
     * @param value String containing various characters, which will be replaced / escaped
     */
    public static String escape(String value) {
        value = value.replace("\"", "\\\"");

        // INFO: https://github.com/mixmark-io/turndown#escaping-markdown-characters
        value = value.replace("\\.", "\\\\.");
        
        value = value.replace("\\[", "\\\\[");
        value = value.replace("\\]", "\\\\]");

        value = value.replace("\t", " ");

        return value;
    }

    /**
     * @return escaped text, such that JSON will be valid
     */
    public static String escapeForJSON(String text) {
        // TODO: Consider classes using this method to user Jackson ObjectMapper instead StringBuilder
        return replaceNewLines(escape(escapeBackslashes(text)), "\\\\n");
    }

    /**
     * Replace new lines by something else
     * @param text Text containing new lines
     * @param replacement Replacement, e.g. a simple space or an escaped new line
     * @return text where new lines have been replaced by spaces
     */
    public static String replaceNewLines(String text, String replacement) {
        if (text != null) {
            return text.trim().replaceAll("\\r\\n|\\r|\\n", replacement);
        } else {
            log.warn("Provided text is null");
            return text;
        }
    }

    /**
     *
     */
    public static String convertHtmlToPlainText(String html) {
        //log.debug("Convert HTML text '" + html + "' to plain text ...");
        // TODO: Check which one is better!
        //return stripHTML(html, false, false);
        return org.jsoup.Jsoup.parse(html).text(); // TODO: Does seem to remove all line breaks!
    }

    /**
     *
     */
    public static String convertHtmlToTOPdeskHtml(String html) {
        String plainText = stripHTML(html, true, true);
        //String plainText = convertHtmlToPlainText(html);

        // TODO: According to C.Mueller@topdesk.comv the following HTML tags are supported: <i> , <em>, <b>, <strong>, <u>, <img>, <br>, <h5>, <h6>

        String topDeskHtml = plainText.replaceAll("\n", "<br>");
        return topDeskHtml;
    }

    /**
     * See https://api.slack.com/reference/surfaces/formatting re "Slack mrkdwn"
     * @param text Text either as plain text or HTML, e.g. "<p>See <a href='https://faq.ukatie.com'>FAQ</a></p>"
     * @return text as Slack mrkdwn, e.g. "See <http://faq.ukatie.com|FAQ>"
     */
    public static String convertToSlackMrkdwn(String text) {
        // INFO: Modified version of https://github.com/furstenheim/copy-down
        OptionsBuilder optionsBuilder = OptionsBuilder.anOptions();
        Options options = optionsBuilder.build();
        //Options options = optionsBuilder.withStrongDelimiter("**").build();
        //Options options = optionsBuilder.withStrongDelimiter("**").withLinkStyle(LinkStyle.REFERENCED).withLinkReferenceStyle(LinkReferenceStyle.DEFAULT).build();
        CopyDown converter = new CopyDown(options);

        // CopyDown is a Java port of https://github.com/mixmark-io/turndown https://github.com/mixmark-io/turndown#escaping-markdown-characters
        String mrkdwn = converter.convert(text);

        // TODO: Handle case when URL contains underscore, e.g. https://en.wikipedia.org/wiki/Self-sovereign_identity, whereas see https://github.com/furstenheim/copy-down/issues/10
        //mrkdwn = mrkdwn.replace("TEST-LINK-UNDERSCORE"," test underscore https://en.wikipedia.org/wiki/Self-sovereign_identity");

        // INFO: Do not log text when text is a protected answer!
        //log.info("Try to convert text '" + text + "' to Mrkdwn ...");
        //log.info("Converted text as Mrkdwn: " + mrkdwn);

        return mrkdwn;
    }

    /**
     * See https://github.com/furstenheim/copy-down and https://www.markdownguide.org/basic-syntax/
     * @param text Text either as plain text or HTML, e.g. "<p>See <a href='https://faq.ukatie.com'>FAQ</a></p>"
     * @return text as Markdown, e.g. "See [FAQ](http://faq.ukatie.com)"
     */
    public static String convertToMarkdown(String text) {
        io.github.furstenheim.OptionsBuilder optionsBuilder = io.github.furstenheim.OptionsBuilder.anOptions();
        io.github.furstenheim.Options options = optionsBuilder.build();
        //io.github.furstenheim.ioOptions options = optionsBuilder.withStrongDelimiter("**").build();
        //io.github.furstenheim.Options options = optionsBuilder.withStrongDelimiter("**").withLinkStyle(LinkStyle.REFERENCED).withLinkReferenceStyle(LinkReferenceStyle.DEFAULT).build();
        io.github.furstenheim.CopyDown converter = new io.github.furstenheim.CopyDown(options);

        String markdown = converter.convert(text);

        // TODO: Handle case when URL contains underscore, e.g. https://en.wikipedia.org/wiki/Self-sovereign_identity, whereas see https://github.com/furstenheim/copy-down/issues/10
        //markdown = markdown.replace("TEST-LINK-UNDERSCORE"," test underscore https://en.wikipedia.org/wiki/Self-sovereign_identity");


        // INFO: Do not log text when text is a protected answer!
        //log.info("Try to convert text '" + text + "' to Markdown ...");
        //log.info("Converted text as Markdown: " + markdown);

        return markdown;
    }

    /**
     * Remove HTML tags from text
     * @param text Text containing HTML tags, e.g. "<p>Test answer &amp; question</p> <ul> <li>One</li> <li>Two</li> <li><a href=\"https://ukatie.com\">Link</a></li> </ul>"
     * @return text with HTML tags, e.g. "Test answer & question One Two https://ukatie.com Link"
     */
    public static String stripHTML(String text, boolean removeTabsAndDoubleSpaces, boolean removeMultipleLinebreaks) {
        StringReader strReader = new StringReader(text);
        HTMLStripCharFilter htmlReader = new HTMLStripCharFilter(strReader);
        int ch = 0;
        StringBuilder builder = new StringBuilder();
        try {
            while ((ch = htmlReader.read()) != -1) {
                builder.append((char) ch);
            }
            htmlReader.close();

            String strippedText = builder.toString();

            // INFO: Only single spaces remain
            // https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/StringUtils.java
            //strippedText = StringUtils.normalizeSpace(strippedText);

            strippedText = strippedText.trim();
            if (removeTabsAndDoubleSpaces) {
                strippedText = removeTabsAndDoubleSpaces(strippedText);
            }
            if (removeMultipleLinebreaks) {
                log.info("Remove multiple linebreaks ...");
                strippedText = strippedText.replaceAll("[\n ]{2,}", "\n\n");
                //strippedText = strippedText.replaceAll("\r", " ");
                //strippedText = strippedText.replaceAll("\f", " ");
            }

            log.debug("Stripped text: " + strippedText);
            return strippedText;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return text;
        }
    }

    /**
     *
     */
    public static String removeTabsAndDoubleSpaces(String text) {
        return text.replaceAll("\t+", " ").replaceAll(" +", " ");
    }

    /**
     * Find files with a particular name recursively
     * @param directory Directory containing sub-directories and files
     * @param fileName File name, e.g. "meta.xml"
     */
    public static File[] searchFiles(File directory, String fileName) {
        List<File> foundFiles = new ArrayList<File>();

        if (directory.isDirectory()) {
            foundFiles = searchFiles(directory, fileName, foundFiles);
        }

        return foundFiles.toArray(new File[0]);
    }

    /**
     *
     */
    private static List<File> searchFiles(File directory, String fileName, List<File> foundFiles) {
        File[] children = directory.listFiles();
        for (File child: children) {
            if (child.isFile() && child.getName().equals(fileName)) {
                foundFiles.add(child);
            }
            if (child.isDirectory()) {
                foundFiles = searchFiles(child, fileName, foundFiles);
            }
        }
        return foundFiles;
    }
}
