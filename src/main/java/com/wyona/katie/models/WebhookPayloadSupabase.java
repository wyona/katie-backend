package com.wyona.katie.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Webhook Payload sent by Supabase, see https://supabase.com/docs/guides/database/webhooks#creating-a-webhook
 * 
 */
public class WebhookPayloadSupabase extends WebhookPayload {

    // INFO: Supabase Payload examples

    /*
    {
        "type":"INSERT",
            "table":"repo",
            "record":{
        "id":19,
                "doi":null,
                "typ":null,
                "isbn":null,
                "tags":null,
                "titel":"Blub",
                "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
                "public":false,
                "verlag":null,
                "content":"Super fancy content",
                "license":null,
                "sprache":"DE",
                "abstract":"Abstract Bli Bla Blub",
                "coauthors":null,
                "datei_url":null,
                "created_at":"2023-11-06T14:17:51+00:00",
                "custom_tags":null,
                "peer_review":null,
                "date_created":"2023-11-06T15:18:42",
                "date_updated":"2023-11-06T15:18:45",
                "anzahl_seiten":5000,
                "erschienen_am":"2023-11-06",
                "erschienen_in":null,
                "erscheinungsort":null,
                "zitiervorschlag":null,
                "link_zur_originalpublikation":null
    },
        "schema":"public",
            "old_record":null
    }

    {
   "type":"UPDATE",
   "table":"repo",
   "record":{
      "id":18,
      "doi":"",
      "typ":"Aufsatz",
      "isbn":"",
      "tags":["Rechtssoziologie","Grundlagen des Rechts","Straf- und Strafprozessrecht"],
      "titel":"Das ist ein Test f..r Katie #2",
      "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
      "public":true,
      "verlag":"",
      "content":"Super fancy content, now updated",
      "license":null,
      "sprache":"DE",
      "abstract":"Eine kurze Zusammenfassung",
      "coauthors":"",
      "datei_url":"a543f62b-4874-4fc9-b1ed-3d406d1fa969/public/qtc6840ri7i.pdf",
      "created_at":"2023-09-16T19:55:31.346862+00:00",
      "custom_tags":"",
      "peer_review":"nein",
      "date_created":null,
      "date_updated":null,
      "anzahl_seiten":5000,
      "erschienen_am":"1900-01-01",
      "erschienen_in":"",
      "erscheinungsort":"",
      "zitiervorschlag":"",
      "link_zur_originalpublikation":""
   },
   "schema":"public",
   "old_record":{
      "id":18,
      "doi":"",
      "typ":"Aufsatz",
      "isbn":"",
      "tags":["Rechtssoziologie","Grundlagen des Rechts","Straf- und Strafprozessrecht"],
      "titel":"Das ist ein Test f..r Katie",
      "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
      "public":true,
      "verlag":"",
      "license":null,
      "sprache":"DE",
      "abstract":"Eine kurze Zusammenfassung",
      "coauthors":"",
      "datei_url":"a543f62b-4874-4fc9-b1ed-3d406d1fa969/public/qtc6840ri7i.pdf",
      "created_at":"2023-09-16T19:55:31.346862+00:00",
      "custom_tags":"",
      "peer_review":"nein",
      "date_created":null,
      "date_updated":null,
      "anzahl_seiten":5000,
      "erschienen_am":"1900-01-01",
      "erschienen_in":"",
      "erscheinungsort":"",
      "zitiervorschlag":"",
      "link_zur_originalpublikation":""
   }
}

{
   "type":"UPDATE",
   "table":"repo",
   "record":{
      "id":26,
      "doi":"",
      "typ":"Arbeitspapier",
      "isbn":"",
      "tags":[
         "Straf- und Strafprozessrecht"
      ],
      "text":"THE SUPER AMAZING FULL TEXT!!! UPDATE 4",
      "titel":"Long text test #UPDATED",
      "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
      "public":true,
      "verlag":"",
      "content":null,
      "license":null,
      "sprache":"DE",
      "abstract":"testing frontend long text update",
      "coauthors":[

      ],
      "datei_url":"a543f62b-4874-4fc9-b1ed-3d406d1fa969/public/s307lnksvj.pdf",
      "created_at":"2024-01-10T17:48:25.982173+00:00",
      "custom_tags":[
         "Max Muster",
         "maxmuster"
      ],
      "peer_review":"nein",
      "date_created":null,
      "date_updated":null,
      "anzahl_seiten":1,
      "erschienen_am":"2029-01-01",
      "erschienen_in":"",
      "erscheinungsort":"",
      "zitiervorschlag":"",
      "link_zur_originalpublikation":""
   },
   "schema":"public",
   "old_record":{
      "id":26,
      "doi":"",
      "typ":"Arbeitspapier",
      "isbn":"",
      "tags":[
         "Straf- und Strafprozessrecht"
      ],
      "text":"THE SUPER AMAZING FULL TEXT!!! UPDATE 4",
      "titel":"Long text test",
      "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
      "public":true,
      "verlag":"",
      "content":null,
      "license":null,
      "sprache":"DE",
      "abstract":"testing frontend long text update",
      "coauthors":[

      ],
      "datei_url":"a543f62b-4874-4fc9-b1ed-3d406d1fa969/public/s307lnksvj.pdf",
      "created_at":"2024-01-10T17:48:25.982173+00:00",
      "custom_tags":[
         "Max Muster",
         "maxmuster"
      ],
      "peer_review":"nein",
      "date_created":null,
      "date_updated":null,
      "anzahl_seiten":1,
      "erschienen_am":"2029-01-01",
      "erschienen_in":"",
      "erscheinungsort":"",
      "zitiervorschlag":"",
      "link_zur_originalpublikation":""
   }
}

{
   "type":"DELETE",
   "table":"repo",
   "record":null,
   "schema":"public",
   "old_record":{
      "id":19,
      "doi":null,
      "typ":null,
      "isbn":null,
      "tags":null,
      "titel":"Blub",
      "author":"a543f62b-4874-4fc9-b1ed-3d406d1fa969",
      "public":false,
      "verlag":null,
      "license":null,
      "sprache":"DE",
      "abstract":"Abstract Bli Bla Blub",
      "coauthors":null,
      "datei_url":null,
      "created_at":"2023-11-06T14:17:51+00:00",
      "custom_tags":null,
      "peer_review":null,
      "date_created":"2023-11-06T15:18:42",
      "date_updated":"2023-11-06T15:18:45",
      "anzahl_seiten":5000,
      "erschienen_am":"2023-11-06",
      "erschienen_in":null,
      "erscheinungsort":null,
      "zitiervorschlag":null,
      "link_zur_originalpublikation":null
   }
}

     */

    private String type;
    private ObjectNode record;
    private ObjectNode oldRecord;

    /**
     *
     */
    public WebhookPayloadSupabase() {
        this.type = null;
    }

    /**
     * @param type Payload type, e.g. "INSERT", "UPDATE", "DELETE"
     */
    /*
    public WebhookPayloadSupabase(String type) {
        this.type = type;
    }

     */

    /**
     * Set type
     * @param type Payload type, e.g. "INSERT" or "UPDATE" or "DELETE"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return Payload type, e.g. "INSERT" or "UPDATE" or "DELETE"
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public void setRecord(ObjectNode record) {
        this.record = record;
    }

    /**
     *
     */
    public ObjectNode getRecord() {
        return record;
    }

    /**
     *
     */
    public void setOld_record(ObjectNode oldRecord) {
        this.oldRecord = oldRecord;
    }

    /**
     *
     */
    public ObjectNode getOld_record() {
        return this.oldRecord;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: " + type);
        return sb.toString();
    }
}
