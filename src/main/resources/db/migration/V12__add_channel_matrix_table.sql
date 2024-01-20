/* Connection values re Channel Matrix */
create table CHANNEL_MATRIX (
    UUID_RESUBMITTED_QUESTION varchar(100) not null,
    DOMAIN_ID varchar(100),
    MATRIX_USER_ID varchar(200) not null,
    MATRIX_ROOM_ID varchar(200) not null
);
