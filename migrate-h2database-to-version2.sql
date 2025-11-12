#!/bin/sh

USERNAME=uuu
PASSWORD=ppp

SQL_FILE=backup.sql
MAVEN_REPO_H2=/Users/michaelwechner/.m2/repository/com/h2database/h2
KATIE_VOLUME=/Users/michaelwechner/src/wyona/public/katie-backend/volume

echo "Dump database as SQL file ..."
java -cp $MAVEN_REPO_H2/1.4.197/h2-1.4.197.jar org.h2.tools.Script -url jdbc:h2:$KATIE_VOLUME/askkatie-h2 -user $USERNAME -password $PASSWORD -script $SQL_FILE

echo "Recrate database version 2 from SQL file ..."
java -cp $MAVEN_REPO_H2/2.4.240/h2-2.4.240.jar org.h2.tools.RunScript -url jdbc:h2:$KATIE_VOLUME/askkatie-h2-v2 -user $USERNAME -password $PASSWORD -script $SQL_FILE
