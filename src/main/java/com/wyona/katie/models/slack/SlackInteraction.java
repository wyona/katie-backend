package com.wyona.katie.models.slack;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/*
{
   "type":"block_actions",
   "user":{
      "id":"U018A7XUWSY",
      "username":"michael.wechner",
      "name":"michael.wechner",
      "team_id":"T01848J69AP"
   },
   "api_app_id":"A0184KMLJJE",
   "token":"hoMub0zJ8kMXIxgqFgrDIc3x",
   "container":{
      "type":"message",
      "message_ts":"1600724836.000800",
      "channel_id":"C017PA1MC1M",
      "is_ephemeral":false
   },
   "trigger_id":"1364028095735.1276290213363.94dfc599c4ba788c5f2b9e8ab51e5e88",
   "team":{
      "id":"T01848J69AP",
      "domain":"wyonaworkspace"
   },
   "channel":{
      "id":"C017PA1MC1M",
      "name":"allgemein"
   },
   "message":{
      "bot_id":"B018B5MRHFW",
      "type":"message",
      "text":"The+current+time+is+2020\/09\/21+21:47:16,+whereas+also+see+<https:\/\/www.timeanddate.com\/>",
      "user":"U018505QFA6",
      "ts":"1600724836.000800",
      "team":"T01848J69AP",
      "blocks":[
         {
            "type":"section",
            "block_id":"1qH\/",
            "text":{
               "type":"plain_text",
               "text":"The+current+time+is+2020\/09\/21+21:47:16,+whereas+also+see+https:\/\/www.timeanddate.com\/",
               "emoji":true
            }
         },
         {
            "type":"actions",
            "block_id":"yqELu",
            "elements":[
               {
                  "type":"button",
                  "action_id":"bVdPR",
                  "text":{
                     "type":"plain_text",
                     "text":"Send+question+to+an+expert+...",
                     "emoji":false
                  }
               }
            ]
         }
      ]
   },
   "response_url":"https:\/\/hooks.slack.com\/actions\/T00000000\/B00000000\/XXXXXXXXXXXXXXXXXXXXXXXX",
   "actions":[
      {
         "action_id":"bVdPR",
         "block_id":"yqELu",
         "text":{
            "type":"plain_text",
            "text":"Send+question+to+an+expert+...",
            "emoji":false
         },
         "type":"button",
         "action_ts":"1600760697.097898"
      }
   ]
}
*/

/**
 * See https://api.slack.com/reference/interaction-payloads
 */
@Slf4j
public class SlackInteraction {

    private String token;
    private String apiAppId;
    private String type;
    private String response_url;

    private SlackUser user;
    private SlackChannel channel;
    private List<SlackAction> actions;

    private String triggerId;

    private SlackView view;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackInteraction() {
    }

    /**
     *
     */
    public void setActions(List<SlackAction> actions) {
        this.actions = actions;
    }

    /**
     *
     */
    public List<SlackAction> getActions() {
        return actions;
    }

    /**
     *
     */
    public void setChannel(SlackChannel channel) {
        this.channel = channel;
    }

    /**
     *
     */
    public SlackChannel getChannel() {
        return channel;
    }

    /**
     *
     */
    public void setUser(SlackUser user) {
        this.user = user;
    }

    /**
     *
     */
    public SlackUser getUser() {
        return user;
    }

    /**
     *
     */
    public void setApi_app_id(String apiAppId) {
        this.apiAppId = apiAppId;
    }

    /**
     * @return for example 'A0184KMLJJE'
     */
    public String getApi_app_id() {
        return apiAppId;
    }

    /**
     * @param token Slack verification token, see src/main/resources/application.properties
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     *
     */
    public String getToken() {
        return token;
    }

    /**
     * @param type Event wrapper type, e.g. "event_callback"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     */
    public String getType() {
        return type;
    }

    /**
     * @param response_url Slack response URL, e.g. "https://hooks.slack.com/actions/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"
     */
    public void setResponse_url(String response_url) {
        this.response_url = response_url;
    }

    /**
     *
     */
    public String getResponse_url() {
        return response_url;
    }

    /**
     *
     */
    public void setTrigger_id(String triggerId) {
        this.triggerId = triggerId;
    }

    /**
     *
     */
    public String getTrigger_id() {
        return triggerId;
    }

    /**
     *
     */
    public void setView(SlackView view) {
        this.view = view;
    }

    /**
     *
     */
    public SlackView getView() {
        return view;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Token: " + token + ", Type: " + type + ", Team Id: " + user.getTeam_id());
        return s.toString();
    }
}
