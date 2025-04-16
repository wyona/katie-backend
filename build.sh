#!/bin/bash

TOMCAT_HOME=/Users/michaelwechner/local/apache-tomcat-9.0.104
JAVA_VERSION_REQUIRED=11.0.11
#JAVA_VERSION_REQUIRED=11.0.16
JAVA_VERSION_REQUIRED=21.0.5

# ----- Check for Java version
JAVA_HOME_MACOSX=/System/Library/Frameworks/JavaVM.framework/Home
JAVA_HOME_MACOSX_YOSEMITE=/System/Library/Frameworks/JavaVM.framework/Versions/Current/Commands/java_home
JAVA_HOME_11=/Library/Java/JavaVirtualMachines/jdk-$JAVA_VERSION_REQUIRED.jdk/Contents/Home

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Your Java version: $JAVA_VERSION"

if [ $JAVA_VERSION = $JAVA_VERSION_REQUIRED ];then
    echo "Your Java version is correct :-)"
else
    echo ""
    echo "WARNING: Katie requires Java $JAVA_VERSION_REQUIRED, please make sure to install Java $JAVA_VERSION_REQUIRED and set JAVA_HOME accordingly"
    echo ""
    exit 1
fi

if [ -f $JAVA_HOME_MACOSX_YOSEMITE ]; then
  if [ -d $JAVA_HOME_11 ];then
    echo "INFO: Java is installed at $JAVA_HOME_11 :-)"
    if [ "$JAVA_HOME" = "" ];then
      echo "ERROR: No JAVA_HOME set! Please make sure to set JAVA_HOME: export JAVA_HOME=$JAVA_HOME_11"
      exit 1
    else
      echo "INFO: JAVA_HOME is set to '$JAVA_HOME'"
    fi
  else
    echo "ERROR: No such JAVA_HOME directory: $JAVA_HOME_11"
    exit 1
  fi
else
  if [ $JAVA_VERSION = $JAVA_VERSION_REQUIRED ];then
      echo "Operating system not detected, but Java version set correctly, so let us continue :-)"
  else
      echo ""
      echo "WARNING: Operating system not detected."
      echo "WARNING: Katie requires Java $JAVA_VERSION_REQUIRED, please make sure to install Java $JAVA_VERSION_REQUIRED and set JAVA_HOME accordingly"
      echo ""
      exit 1
  fi
fi

echo "INFO: Clean ..."
mvn clean

echo "INFO: Build Katie webapp ..."
mvn install -Dmaven.test.skip=true
#mvn install
#mvn -X install

if [ -d $TOMCAT_HOME ];then
echo "INFO: Deploy webapp ..."

rm -rf $TOMCAT_HOME/webapps/katie
rm -rf $TOMCAT_HOME/work/Catalina/localhost/katie
cp target/askkatie-webapp-*.war $TOMCAT_HOME/webapps/katie.war

#rm -rf $TOMCAT_HOME/webapps/ROOT
#rm -rf $TOMCAT_HOME/work/Catalina/localhost/ROOT
#cp target/askkatie-webapp-*.war $TOMCAT_HOME/webapps/ROOT.war

echo "INFO: Clean log files ..."
rm -f $TOMCAT_HOME/logs/*

    echo "INFO: Startup Tomcat '$TOMCAT_HOME' and access AskKatie 'http://127.0.0.1:8080/katie/' ...."
else
    echo "INFO: No Tomcat installed at $TOMCAT_HOME"
fi
