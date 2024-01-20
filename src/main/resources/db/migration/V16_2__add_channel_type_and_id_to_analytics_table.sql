/* add new coloumns CHANNEL_TYPE and CHANNEL_ID */
alter table ANALYTICS add CHANNEL_TYPE varchar(20) not null default 'UNDEFINED';
alter table ANALYTICS add CHANNEL_ID varchar(100);
