package com.wyona.katie.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Webhook Payload sent by Discourse
 * 
 */
public class WebhookPayloadDiscourse extends WebhookPayload {

    // INFO: Discourse Payload examples

    /*
    {
   "post":{
      "id":2053,
      "name":"Michael Wechner",
      "username":"michael.wechner",
      "avatar_template":"/user_avatar/forum.weaviate.io/michael.wechner/{size}/60_2.png",
      "created_at":"2023-11-27T23:11:36.831Z",
      "cooked":"<p>IIUC when upgrading, then I will have to set</p>\n<pre><code class=\"lang-auto\">\"model\": \"embed-multilingual-v2.0\"\n</code></pre>\n<p>to stay backwards compatible if I had not set the model parameter before, right?</p>",
      "post_number":5,
      "post_type":1,
      "updated_at":"2023-11-27T23:11:36.831Z",
      "reply_count":0,
      "reply_to_post_number":4,
      "quote_count":0,
      "incoming_link_count":0,
      "reads":0,
      "score":0,
      "topic_id":916,
      "topic_slug":"cohere-embed-v3",
      "topic_title":"Cohere embed v3",
      "category_id":4,
      "display_username":"Michael Wechner",
      "primary_group_name":null,
      "flair_name":null,
      "flair_group_id":null,
      "version":1,
      "user_title":null,
      "reply_to_user":{
         "username":"DudaNogueira",
         "name":"Duda Nogueira",
         "avatar_template":"/user_avatar/forum.weaviate.io/dudanogueira/{size}/337_2.png"
      },
      "bookmarked":false,
      "raw":"IIUC when upgrading, then I will have to set\n\n```\n\"model\": \"embed-multilingual-v2.0\"\n```\nto stay backwards compatible if I had not set the model parameter before, right?",
      "moderator":false,
      "admin":false,
      "staff":false,
      "user_id":23,
      "hidden":false,
      "trust_level":1,
      "deleted_at":null,
      "user_deleted":false,
      "edit_reason":null,
      "wiki":false,
      "reviewable_id":null,
      "reviewable_score_count":0,
      "reviewable_score_pending_count":0,
      "topic_posts_count":5,
      "topic_filtered_posts_count":5,
      "topic_archetype":"regular",
      "category_slug":"general",
      "akismet_state":null,
      "user_cakedate":"2023-05-25"
   }
}
     */

    private ObjectNode post;

    /**
     *
     */
    public void setPost(ObjectNode post) {
        this.post = post;
    }

    /**
     *
     */
    public ObjectNode getPost() {
        return post;
    }

    /**
     *
     */
    @Override
    public String toString() {
        if (getPost() != null) {
            return "Discourse Post Id: " + getPost().get("id").asText();
        } else {
            return "TODO: Discourse Payload";
        }
    }
}
