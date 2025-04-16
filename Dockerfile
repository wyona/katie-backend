FROM openjdk:21
MAINTAINER katie.qa
VOLUME /tmp
COPY target/askkatie-webapp-*.war app.war

# TLS 1.0 and 1.1 (TLSv1, TLSv1.1) enabled in order to be able to connect to IMAP servers which do not support more recent protocols, whereas see property jdk.tls.disabledAlgorithms inside file java.security
# For more details see src/main/webapp/email-integration.html
COPY java.security /usr/java/openjdk-21/conf/security/.

#ENTRYPOINT ["java","-Djdk.http.auth.tunneling.disabledSchemes=","-jar","/app.war"]
ENTRYPOINT ["java","-jar","/app.war"]
