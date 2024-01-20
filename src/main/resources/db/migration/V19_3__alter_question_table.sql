-- add new columns CHANNEL_TYPE and CHANNEL_REQUEST_UUID
-- remove columns EMAIL_FROM and SLACK_CHANNEL_ID
alter table QUESTION drop EMAIL_FROM;
alter table QUESTION drop SLACK_CHANNEL_ID;
alter table QUESTION add CHANNEL_TYPE varchar(20) not null default 'UNDEFINED';
alter table QUESTION add CHANNEL_REQUEST_UUID varchar(100);
