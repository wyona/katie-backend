FROM openjdk:21
MAINTAINER katie.qa
VOLUME /tmp
COPY target/askkatie-webapp-*.war app.war
# TLS 1.0 and 1.1 enabled in order to be able to connect to IMAP servers which do not support more recent protocols, whereas see jdk.tls.disabledAlgorithms
COPY java.security /usr/local/openjdk-11/conf/security/.
#ENTRYPOINT ["java","-Djdk.http.auth.tunneling.disabledSchemes=","-jar","/app.war"]
ENTRYPOINT ["java","-jar","/app.war"]
