/* Add new columns re connection approval */
alter table SLACK_TEAM_KATIE_DOMAIN add STATUS varchar(25) not null default 'APPROVED';
alter table SLACK_TEAM_KATIE_DOMAIN add APPROVAL_TOKEN varchar(100);
