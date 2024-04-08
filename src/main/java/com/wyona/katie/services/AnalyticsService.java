package com.wyona.katie.services;

import com.wyona.katie.models.ChannelType;
import com.wyona.katie.models.insights.Event;
import com.wyona.katie.models.ChannelType;
import com.wyona.katie.models.insights.Event;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Slf4j
@Component
public class AnalyticsService {

    @Autowired
    private DataRepositoryService dataRepositoryService;

    private static final String EVENT_TYPE_GET_FAQ = "get_faq";
    private static final String EVENT_TYPE_MESSAGE_RECEIVED = "msg_rec";

    private static final String EVENT_TYPE_ANSWER_APPROVED = "answer_approved";
    private static final String EVENT_TYPE_ANSWER_DISCARDED = "answer_discarded";
    private static final String EVENT_TYPE_ANSWER_CORRECTED = "answer_corrected";
    private static final String EVENT_TYPE_ANSWER_IGNORED = "answer_ignored";

    private static final String EVENT_TYPE_QUESTION_SENT_TO_EXPERT = "q_sent2expert";
    private static final String EVENT_TYPE_FEEDBACK_10 = "feedb_10";
    private static final String EVENT_TYPE_FEEDBACK_1 = "feedb_1";

    private static final String EVENT_TYPE_FEEDBACK_PREDICTED_LABEL_POSITIVE = "feedb_label_pos";
    private static final String EVENT_TYPE_FEEDBACK_PREDICTED_LABEL_NEGATIVE = "feedb_label_neg";

    /**
     * Log that a message was reveived
     *
     * @param domainId Katie ddmain Id
     * @param channelType Channel type, e.g. EMAIL, SLACK, ...
     * @param channelId Channel Id, e.g. Slack or MS Teams channel Id
     */
    public void logMessageReceived(String domainId, ChannelType channelType, String channelId) {
        Date current = new Date();
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_MESSAGE_RECEIVED, null, channelType, channelId, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log that answer got approved
     *
     * @param domainId Katie domain Id
     * @param channelType Channel type, e.g. DISCORD, SLACK, ...
     */
    public void logAnswerApproved(String domainId, ChannelType channelType) {
        Date current = new Date();
        String channelId = null;
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_ANSWER_APPROVED, null, channelType, channelId, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log that answer got discarded
     *
     * @param domainId Katie domain Id
     * @param channelType Channel type, e.g. DISCORD, SLACK, ...
     */
    public void logAnswerDiscarded(String domainId, ChannelType channelType) {
        Date current = new Date();
        String channelId = null;
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_ANSWER_DISCARDED, null, channelType, channelId, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log that answer got corrected
     *
     * @param domainId Katie domain Id
     * @param channelType Channel type, e.g. DISCORD, SLACK, ...
     */
    public void logAnswerCorrected(String domainId, ChannelType channelType) {
        Date current = new Date();
        String channelId = null;
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_ANSWER_CORRECTED, null, channelType, channelId, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log that answer got ignored
     *
     * @param domainId Katie domain Id
     * @param channelType Channel type, e.g. DISCORD, SLACK, ...
     */
    public void logAnswerIgnored(String domainId, ChannelType channelType) {
        Date current = new Date();
        String channelId = null;
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_ANSWER_IGNORED, null, channelType, channelId, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log FAQ request
     */
    public void logFAQRequest(String domainId, String language) {
        Date current = new Date();
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_GET_FAQ, language, ChannelType.UNDEFINED, null, agent, remoteAddress, null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log question sent to expert
     */
    public void logQuestionSentToExpert(String domainId, ChannelType channelType, String email) {
        Date current = new Date();
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_QUESTION_SENT_TO_EXPERT, null, channelType, null, agent, remoteAddress, email);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log postive / negative feedback re answer
     */
    public void logFeedbackReAnswer(String domainId, int rating, String email) {
        Date current = new Date();
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            if (rating < 6) {
                dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_FEEDBACK_1, null, ChannelType.UNDEFINED, null, agent, remoteAddress, email);
            } else {
                dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_FEEDBACK_10, null, ChannelType.UNDEFINED, null, agent, remoteAddress, email);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Log postive / negative feedback re predicted label
     */
    public void logFeedbackRePredictedLabel(String domainId, int rank, String email) {
        Date current = new Date();
        String agent = "TODO";
        String remoteAddress = "TODO";
        try {
            if (rank == 0) {
                dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_FEEDBACK_PREDICTED_LABEL_POSITIVE, null, ChannelType.UNDEFINED, null, agent, remoteAddress, email);
            } else {
                dataRepositoryService.logAnalyticsEvent(domainId, EVENT_TYPE_FEEDBACK_PREDICTED_LABEL_NEGATIVE, null, ChannelType.UNDEFINED, null, agent, remoteAddress, email);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Total number of feedbacks (either positive or negative) in the specified time period
     */
    public Event[] getFeedbackEvents(int rating, String domainId, Date start, Date end) throws Exception {
        if (rating < 6) {
            return dataRepositoryService.getAnalysticsEvents(EVENT_TYPE_FEEDBACK_1, domainId, start, end);
        } else {
            return dataRepositoryService.getAnalysticsEvents(EVENT_TYPE_FEEDBACK_10, domainId, start, end);
        }
    }

    /**
     *
     */
    public Event[] getQuestionsSentToExpertEvents(String domainId, Date start, Date end) throws Exception {
        return dataRepositoryService.getAnalysticsEvents(EVENT_TYPE_QUESTION_SENT_TO_EXPERT, domainId, start, end);
    }

    /**
     *
     */
    public Event[] getMessageEvents(String domainId, Date start, Date end) throws Exception {
        return dataRepositoryService.getAnalysticsEvents(EVENT_TYPE_MESSAGE_RECEIVED, domainId, start, end);
    }

    /**
     *
     */
    public Event[] getFaqPageEvents(String domainId, Date start, Date end) throws Exception {
        return dataRepositoryService.getAnalysticsEvents(EVENT_TYPE_GET_FAQ, domainId, start, end);
    }

    /**
     * Total number of FAQ pageviews in the specified time period
     */
    public int getFAQPageviews(String domainId, String language, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_GET_FAQ, language, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     *
     */
    public int getNumberOfReceivedMessages(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_MESSAGE_RECEIVED, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     *
     */
    public int getNumberOfApprovedAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_ANSWER_APPROVED, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     *
     */
    public int getNumberOfDiscardedAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_ANSWER_DISCARDED, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     *
     */
    public int getNumberOfCorrectedAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_ANSWER_CORRECTED, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     *
     */
    public int getNumberOfIgnoredAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_ANSWER_IGNORED, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of asked questions in the specified time period
     */
    public int getNumberOfAskedQuestions(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfQuestions(domainId, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of next best answer (when user clicked 'see more answers ...') in the specified time period
     */
    public int getNumberOfNextBestAnswer(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfNextBestAnswer(domainId, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of answered questions in the specified time period
     */
    public int getNumberOfAnsweredQuestions(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfAnsweredQuestions(domainId, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of questions sent to expert in the specified time period
     */
    public int getNumberOfQuestionsSentToExpert(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_QUESTION_SENT_TO_EXPERT, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of positive feedbacks re answers in the specified time period
     */
    public int getNumberOfPositiveFeedbacksReAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_FEEDBACK_10, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Total number of negative feedbacks re answers in the specified time period
     */
    public int getNumberOfNegativeFeedbacksReAnswers(String domainId, Date start, Date end) {
        try {
            return dataRepositoryService.getNumberOfEvents(domainId, EVENT_TYPE_FEEDBACK_1, null, start, end);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }
}
