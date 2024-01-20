package com.wyona.katie.models;

/**
 * Webhook Payload sent by Directus, e.g. https://repositorium.directus.app
 */
public class WebhookPayloadDirectus extends WebhookPayload {

    // INFO: Directus Payload examples

    /*
{

   "event":"items.create",

   "accountability":{

      "user":"80983515-aadc-43c2-b951-95e96313259b",

      "role":"1b8275bf-d7db-4468-9707-37308b045a4c"

   },

   "payload":{

      "Titel":"Test by Michael",

      "Typ":3,

      "Peer_Review":"nein",

      "Datei":"1e8a7e9e-2baf-4d0f-bbec-4a5220ef2b61"

   },

   "key":23,

   "collection":"Repositorium"

}

{

   "event":"items.update",

   "accountability":{

      "user":"80983515-aadc-43c2-b951-95e96313259b",

      "role":"1b8275bf-d7db-4468-9707-37308b045a4c"

   },

   "payload":{

      "ISBN":"12345"

   },

   "keys":[

      "20"

   ],

   "collection":"Repositorium"

}
     */

    private String event;
    private String key;
    private String[] keys;
    private String collection;

    /**
     *
     */
    public WebhookPayloadDirectus() {
        this.event = null;
        this.key = null;
        this.keys = null;
        this.collection = null;
    }

    /**
     *
     */
    public WebhookPayloadDirectus(String event, String key, String[] keys, String collection) {
        this.event = event;
        this.key = key;
        this.keys = keys;
        this.collection = collection;
    }

    /**
     * Set event
     * @param event Event, e.g. "items.create" or "items.update"
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     *
     */
    public String getEvent() {
        return event;
    }

    /**
     * @param key Document key, e.g. "23"
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     *
     */
    public String getKey() {
        return key;
    }

    /**
     *
     */
    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    /**
     *
     */
    public String[] getKeys() {
        return keys;
    }

    /**
     * @param collection Collection name, e.g. "Repositorium"
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     *
     */
    public String getCollection() {
        return collection;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event: " + event);
        sb.append(", Collection: " + collection);
        if (key != null) {
            sb.append(", Key: " + key);
        }
        if (keys != null && keys.length > 0) {
            sb.append(", Keys: [");
            for (int i = 0; i < keys.length; i++) {
                sb.append(keys[i]);
                if (i < keys.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
