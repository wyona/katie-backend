-- add new column QUESTIONER_USER_ID (for example of a user asking question on a Slack channel)
alter table RESUBMITTED_QUESTION add QUESTIONER_USER_ID varchar(100);
