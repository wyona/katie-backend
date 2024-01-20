package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class OneNotePage {

    private String title;
    private String contentURL;
    private String webURL;

    private String parentTitle;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public OneNotePage() {
        this.title = null;
        this.parentTitle = null;
        this.contentURL = null;
        this.webURL = null;
    }

    /**
     *
     */
    public OneNotePage(String title, String contentURL, String webURL, String parentTitle) {
        this.title = title;
        this.parentTitle = parentTitle;
        this.contentURL = contentURL;
        this.webURL = webURL;
    }

    /**
     * @param title Page title, e.g. "Alles was Du wissen musst"
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return page title, e.g. "Alles was Du wissen musst"
     */
    public String getTitle() {
        return this.title;
    }

    /**
     *
     */
    public void setContentURL(String url) {
        this.contentURL = url;
    }

    /**
     * @return page content URL, e.g. "https://graph.microsoft.com/v1.0/users/michael.wechner@wyona.com/onenote/pages/0-357ba634ee03324c9116e3599fdf6d60!1-8D3F909A0DAE592D!649/content"
     */
    public String getContentURL() {
        return this.contentURL;
    }

    /**
     *
     */
    public void setWebURL(String url) {
        this.webURL = url;
    }

    /**
     * @return page web URL, e.g. "https://szhglobal.sharepoint.com/sites/MSGR-00000778/Shared%20Documents/General/WIKI%20Energieberatung?wd=target%28Heizsysteme%20und%20Geb%C3%A4udeh%C3%BClle.one%7C7efe4e04-b4d1-4904-aebc-4f5fb6ab753c%2FAussenheizungen%7C5dffd339-8ee0-4303-bac2-8be6cc12e41c%2F%29"
     */
    public String getWebURL() {
        return this.webURL;
    }

    /**
     *
     */
    public void setParentTitle(String title) {
        this.parentTitle = title;
    }

    /**
     *
     */
    public String getParentTitle() {
        return parentTitle;
    }
}
