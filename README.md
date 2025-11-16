<h2 align="center">
<a href="https://katie.qa"><img src="https://github.com/wyona/katie-backend/blob/main/src/main/webapp/assets/img/katie_logo.svg"/></a> (Backend)
</h2>

## About

<strong>[Katie](https://katie.qa)</strong> is an Open Source AI-based question-answering platform that helps companies and organizations make their private domain knowledge accessible and useful to their employees and customers.

<strong>Katie</strong> is integrated with [Discord](https://app.katie.qa/discord.html), [Slack](https://app.katie.qa/slack.html), [MS Teams](https://app.katie.qa/ms-teams.html), [Matrix](https://app.katie.qa/matrix.html), [E-Mail](https://app.katie.qa/email-integration.html), Wordpress and [TOPdesk](https://app.katie.qa/topdesk-integration.html), and also provides a web interface for expert users.

<strong>Katie</strong> can be connected with all your applications and data repositories, like for example Websites, Confluence, SharePoint, OneNote, Outlook, Supabase, Directus, Discourse, etc.

<strong>Katie</strong> is based on state of the art AI and supports embedding and large language models of your choice.

By default, <strong>Katie</strong> uses [Apache Lucene](https://lucene.apache.org/) for full text and vector search, but it also integrates with [Weaviate](https://weaviate.io), [Elasticsearch](https://www.elastic.co), [Azure AI Search](https://learn.microsoft.com/en-us/azure/search/), etc.

## Quickstart

Pull the most recent Katie image for linux/amd64 from [Docker Hub](https://hub.docker.com/r/wyona/katie/tags) (with prefix [/katie](https://hub.docker.com/r/wyona/katie-with-prefix/tags) or for [Mac M1 or higher](https://hub.docker.com/r/wyona/katie-mac-m/tags)) and run Katie locally, please follow the steps below

* Download docker compose [file](https://github.com/wyona/katie-backend/blob/main/env/docker/run/docker-compose.yml)
* Open docker compose file and configure volume path (search for TODO_REPLACE_DIRECTORY_PATH)
* Optional: Open docker compose file and customize environment variables
    * For example outgoing mail configuration (APP_MAIL_HOST, ...)
* Run 'docker-compose up -d'
* Check log 'docker-compose logs -f'
* Katie will be available at http://localhost:8044
* Login with the following credentials U: superadmin, P: Katie1234%

## Requirements

The <strong>Katie</strong> backend webapp is based on Spring Boot and to build and run locally in a terminal you need

* JDK: 21 or higher
    * https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
* Maven version: 3.9.6 or higher (IMPORTANT: Please double check Maven .m2/settings.xml)
    * https://maven.apache.org/download.cgi
    * https://maven.apache.org/install.html

## Build and Run from Command Line

* Open file 'src/main/resources/application-dev.properties' and configure property 'volume.base.path'
* Set environment variable: export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
* Set environment variable: export PATH=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin:$PATH
* Set environment variable: export SPRING_PROFILES_ACTIVE=dev
* Configure your JDK version inside the shell script 'build.sh' (search for JAVA_VERSION_REQUIRED)
* Build Katie webapp as war file: <em>sh build.sh</em>
* Startup Katie: <em>java -jar target/askkatie-webapp-1.374.0-SNAPSHOT.war</em>
* Katie will be available at: http://localhost:8044 or https://localhost:8443 (see SSL properties inside src/main/resources/application.properties)
* Login with the following credentials: U: superadmin, P: Katie1234% (see volume/iam/users.xml)

Optionally you can run Katie with an outgoing proxy configuration enabled (https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html)

* Startup Katie with proxy configuration: <em>java -Dhttp.proxyHost=proxy.wyona.com -Dhttp.proxyPort=8044 -Dhttps.proxyHost=proxy.wyona.com -Dhttps.proxyPort=443 -Dhttp.nonProxyHosts="*aleph-alpha.com|*cohere.ai" -Dhttp.proxyUser=USERNAME -Dhttp.proxyPassword=PASSWORD -Dhttps.proxyUser=USERNAME -Dhttps.proxyPassword=PASSWORD -jar target/askkatie-webapp-1.374.0-SNAPSHOT.war</em>

## IntelliJ (Ultimate)

* Download IntelliJ IDEA Ultimate: https://www.jetbrains.com/idea/download/other.html
* Open file 'src/main/resources/application-dev.properties' and configure property 'volume.base.path'
* Open IntelliJ: File > Open (select directory "katie-backend")
* Set JDK version: File > Project Structure > Project SDK: 21
    * Also see https://www.jetbrains.com/help/idea/maven-support.html#change_jdk
* Run Maven (askkatie-webapp > Lifecycle): clean + install
    * Optional: Disable tests (Settings... > Build, Execution, Deployment > Build Tools > Maven > Runner > Skip tests)
* Click on "Add Configuration..."
    * Click "+" to add new configuration: Spring Boot
    * Set Name, e.g. "Katie"
    * Main class: com.wyona.katie.Server
    * Environment, JRE: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    * Active profiles: dev
        * Optional: Set environment variable SPRING_PROFILES_ACTIVE=dev
* Close configuration and run "Katie"
* Open http://localhost:8044 inside your browser and login with U: superadmin, P: Katie1234%

Make sure you have Lombok configured

https://www.baeldung.com/lombok-ide

In case startup fails, then delete the .idea directory and the file askkatie-webapp.iml, and reopen the project.

## Eclipse (Version: 2022-06 (4.24.0))

* Go to Help | Eclipse Marketplace...
* Search for "Spring" and install "Spring tools 4" (by VMware) https://www.eclipse.org/community/eclipse_newsletter/2018/february/springboot.php
* Import the project from intelliJ to Eclipse, inside intelliJ go to File | Export | Project to Eclipse...
* Select "Convert selected modules into Eclipse-compatible format" and "Export non-module libraries"
* In Eclipse, go to File | Import and select General->Projects from Folder or Archive
* Select the root directory katie-backend for the Import source directory. Eclipse should detect katie-backend as a Project
* Install Lombok for Slf4j by downloading lombok.jar from here https://projectlombok.org/download
* (WINDOWS) And then executing the installer in a powershell with "java -jar lombok.jar" and re-open eclipse
* Configure 'volume.base.path' and comment or disable 'server.servlet.session.cookie.secure' inside src/main/resources/application-dev.properties
*(WINDOWS) Your volume.base.path MUST be absolute like for example: "G:/katie/katie-backend/volume". If you want to start the app in any other configuration, for example prod, don't forget to change the volume.base.path from "/katie-backend/volume" to "./katie-backend/volume"
* Click on "Run | Run configurations..."
    * Click "New launch configuration" to add new configuration: Spring Boot
    * Main class: com.wyona.katie.Server
    * !!! Select Profile dev

* Apply configuration and run
* Open http://localhost:8044 inside your browser and login with U: superadmin, P: Katie1234%

## Run Katie within Tomcat

* Configure Tomcat path inside build.sh (see TOMCAT_HOME)
* Configure 'volume.base.path' and comment/disable 'server.servlet.session.cookie.secure' inside src/main/resources/application-prod.properties
* Either remove &lt;base href="/"&gt; or set prefix &lt;base href="/katie/"&gt; inside src/main/webapp/index.html
* Set i18n path to "./assets/i18n/" (1 location) and TinyMCE path to 'base_url:"./tinymce"' (7 locations) inside src/main/webapp/main.js
* Build webapp as war, run: 'sh build.sh'
* Startup Tomcat: ./bin/startup.sh
* Request in browser: http://127.0.0.1:8080/katie/
* tail -F logs/*

## Generate Katie Docker image and run Katie Docker container

* Optional: Comment/disable 'server.servlet.session.cookie.secure' inside src/main/resources/application.properties
* Set environment variable: export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
* Configure your JDK version inside the shell script 'build.sh' (search for JAVA_VERSION_REQUIRED)
* Build webapp as war, run: 'sh build.sh'
* Build image: docker build -t katie .
* Tag image:
    * docker tag katie wyona/katie:1.374.0
    * docker tag katie wyona/katie-mac-m:1.374.0
    * docker tag katie wyona/katie-with-prefix:1.374.0
* Push image to Docker Hub: docker login -u USERNAME -p PASSWORD docker.io && docker push wyona/katie:1.374.0
* Run image:
    * docker run -p 7070:8080 -v $(pwd)/volume:/ask-katie katie ("/ask-katie" is set inside application-prod.properties)
    * docker run -p 7070:8080 katie (If you do not mount a volume, then Katie creates the necessary files and directories within the docker container, which gets reset upon restart though)
* Open http://localhost:7070 inside your browser and login with U: superadmin, P: Katie1234%
* REST interfaces: http://127.0.0.1:7070/swagger-ui/index.html
* Health check endpoint: http://127.0.0.1:7070/actuator/health
    * Other actuator endpoints: http://127.0.0.1:7070/actuator (whereas set management.endpoints.web.exposure.include=* inside application(-dev).properties)

Or as another alternative run:

sh pull-down-up.sh

whereas make sure to configure VOLUME_KATIE inside the script accordingly.

## Docker using Tomcat

* Either remove &lt;base href="/"&gt; or set prefix &lt;base href="/katie/"&gt; inside src/main/webapp/index.html
* Set i18n path to "./assets/i18n/" (1 location) and TinyMCE path to 'base_url:"./tinymce"' (7 locations) inside src/main/webapp/main.js
* sh build.sh
* Check available Tomcat version at https://dlcdn.apache.org/tomcat/tomcat-9/ and update inside Dockerfile_Java21_Tomcat
* Build image containing Tomcat: docker build -t katie-tomcat -f Dockerfile_Java21_Tomcat .
* docker run -p 7070:8080 -v $(pwd)/volume:/ask-katie katie-tomcat
* http://localhost:7070/katie/

## Connect Katie Docker with an Embedding Service Docker

* Setup Katie Docker and Embedding Service Docker (e.g. https://github.com/Repositorium-ch/embedding-service)
* In order to connect these two Docker containers locally one can create a Docker network: ```docker network create --subnet 172.20.0.0/16 --ip-range 172.20.240.0/20 multi-host-network```
* In order to connect the two Docker containers with this network, get the container IDs ```docker ps -a```

```
CONTAINER ID   IMAGE                 COMMAND                  CREATED          STATUS          PORTS                                       NAMES
5b0cddb97d32   embedding_server      "docker-entrypoint.s…"   17 minutes ago   Up 17 minutes   0.0.0.0:3000->3000/tcp, :::3000->3000/tcp   bold_ride
29ef347da530   wyona/katie:1.365.0   "java -jar /app.war"     20 minutes ago   Up 20 minutes   0.0.0.0:8044->8080/tcp, :::8044->8080/tcp   katie
```

* Add containers to network
    * ```docker network connect --ip 172.20.128.2 multi-host-network 5b0cddb97d32```
    * ```docker network connect --ip 172.20.128.3 multi-host-network 29ef347da530```
* Connect Katie with Embedding Service, for example by manually editing the Katie domain configuration file ```config.xml```
    * ```<sbert-lucene api-token="23a4c91ff5ddf949847859389cc45c0da301028104b0ca7c4103b9821d89c697" embeddings-endpoint="http://172.20.128.2:3000/v1/embeddings" embeddings-impl="OPENAI_COMPATIBLE" similarity-metric="COSINE" value-type="float32"/>```

## API and Testing API

* https://app.katie.qa/swagger-ui/index.html
* Postman: env/postman/AskKatie.postman_collection.json
* Private and public keys to generate JWTs: https://github.com/wyona/katie-backend/blob/main/volume/config/jwt/README.md

## Database / Flyway

Create / migrate Database on startup of Katie web app

* pom.xml (flyway dependency)
* src/main/resources/application.properties (flyway and h2 configuration, volume/askkatie-h2-v2.mv.db)
* src/main/java/com/wyona/katie/config/DataSourceConfig.java
* SQL Scripts: src/main/resources/db/migration
* Migration script from v1 to v2: migrate-h2database-to-version2.sql

When running Katie as Docker

* Mount volume to access h2 database (volume/askkatie-h2-v2.mv.db) from outside of docker container: -v /Users/michaelwechner/src/katie-backend/volume:/ask-katie

Documentation

* https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto-execute-flyway-database-migrations-on-startup
* https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto-data-access

Backup and restore h2 database:

* http://www.h2database.com/html/tutorial.html#upgrade_backup_restore
* https://stackoverflow.com/questions/2036117/how-to-back-up-the-embedded-h2-database-engine-while-it-is-running

Access database from command line (WARN: Might not work properly when database is already in use by server, either stop server or make a copy and connect with copy)

* Run: java -cp /Users/michaelwechner/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar org.h2.tools.Shell
* Enter:
    * jdbc:h2:file:/Users/michaelwechner/src/katie-backend/volume/askkatie-h2-v2
    * org.h2.Driver
    * Username and Password see secret-keys.properties (db.username, db.password)
    * show tables;
    * show columns from QUESTION;
    * select * from public."flyway_schema_history";
    * select * from REMEMBERME;
    * quit

When you encounter an error like "Migration checksum mismatch for migration version 28.3", then the reason is that the migration script src/main/resources/db/migration/V28_3__alter_question_table.sql has been modified, for example because the script comment at the top got updated.
If this modification is not relevant, then you can fix the database by replacing the previous checksum (Applied to database) by the new checksum (Resolved locally)

* select * from "flyway_schema_history";
* update "flyway_schema_history" set "checksum" = '392673978' where "version" = '19.4';
* update "flyway_schema_history" set "checksum" = '-2012601311' where "version" = '20.3';
* update "flyway_schema_history" set "checksum" = '951373641' where "version" = '26.1';
* update "flyway_schema_history" set "checksum" = '-1714955084' where "version" = '28.3';

and the startup should work again.

## Update Angular Frontend

* git clone git@github.com:wyona/katie-expert-frontend-angular.git
* cd katie-expert-frontend-angular
* npm install
* ng build
* ../katie-backend
* cp -r ../katie-expert-frontend-angular/dist/admin-backend/* src/main/webapp/.
* sh build.sh

### Enable Google Analytics

* Uncomment Google Analytics script lines inside src/index.html in order to load gtag script and replace TAG ID with your own TAG ID
* Enable sending events to Google Analytics inside src/app/app.component.ts and replace TAG ID with your own TAG ID
* Test retrieval of events at https://analytics.google.com/ (and https://tagassistant.google.com/)

## Create Release

Update the version in the following files:

* pom.xml
* src/main/resources/application.properties
* env/docker/run/docker-compose.yml
* README.md

## Elasticsearch 6.6.1

Basic configuration: src/main/resources/application.properties

Implementation: src/main/java/com/wyona/katie/handlers/ElasticsearchQuestionAnswerImpl.java

List all indices https://elasticsearch-vt.wyona.com/_cat/indices
Get all hits of a particular index: E.g. https://elasticsearch-vt.wyona.com/askkatie_1b3805a8-84db-452d-ae00-b13755686d30/_search

## Slack App

You can add the official Katie Slack App to your Slack workspace by clicking on the button "Add to Slack" on the page https://app.katie.qa/slack.html

The official Katie Slack App is configured at https://api.slack.com/apps/A0184KMLJJE (Workspace https://wyonaworkspace.slack.com)

If you want to create and use your own custom Slack App in order to connect with the Katie backend, then follow the instructions below:

* Login to Slack
* Create new App: https://api.slack.com/apps

Update configuration parameters inside volume/config/slack-apps.json

Config values of your App

* SLACK_SIGNATURE (slack.signature) (see Basic Information: Signing Secret)
* SLACK_CLIENT_ID (slack.client.id) (see Basic Information: Client ID)
* SLACK_CLIENT_SECRET (slack.client.secret) (see Basic Information: Client Secret)

Update Slack App configuration

* Config "Request URL" (Menu: Interactivity & Shortcuts, Manifest: interactivity)
    * E.g. https://app.katie.qa/api/v1/slack/interactivity or https://MY.DOMAIN/api/v1/slack/interactivity

* Config command (Menu: Slash Commands, Manifest: slash_commands)
    * Command: /katie
    * Request URL: https://app.katie.qa/api/v1/slack/command/katie or https://MY.DOMAIN/api/v1/slack/command/katie
    * Short Description: Get help on how to use Katie

* Add New Redirect URL (Menu: OAuth & Permissions, Manifest: redirect_urls)
    * E.g. https://app.katie.qa/api/v1/slack/oauth2-callback/SLACK_CLIENT_ID or https://MY.DOMAIN/api/v1/slack/oauth2-callback/SLACK_CLIENT_ID
    * Bot Token scopes
        * channels:history
        * chat:write
        * commands
        * im:history
        * incoming-webhook
        * team:read
    * User Token scopes
        * im:history (Permits direct communication with Katie)

* Config Event Subscriptions (Menu: Event Subscriptions, Manifest: event_subscriptions)
    * E.g. https://app.katie.qa/api/v1/slack/events or https://MY.DOMAIN/api/v1/slack/events
    * Bot Events scopes
        * message.channels
        * message.im
    * User Events scopes
        * message.im

* Activate / Manage Distribution
    * E.g. https://slack.com/oauth/v2/authorize?client_id=1276290213363.2089707188626&scope=channels:history,commands,im:history,chat:write,incoming-webhook,team:read&user_scope=im:history

Slack Katie App Manifest (https://app.slack.com/app-settings/T01848J69AP/A0184KMLJJE/app-manifest):

<pre>
_metadata:
  major_version: 1
  minor_version: 1
display_information:
  name: Katie
  description: Katie is a question answering bot, continuously improving, self-learning and trained by humans.
  background_color: "#000000"
  long_description: Katie answers questions using artificial and natural intelligence, whereas Katie is currently not intended for conversations beyond asking one question at a time. Natural conversations are much more complex than just detecting duplicated/similar questions. Simple dialogs with clear intents, such as for example a restaurant reservation or initiate a phone call, work quite well already, but more complex conversations are much more difficult and users become frustrated and will eventually stop trying to have more complex conversations.
features:
  app_home:
    home_tab_enabled: true
    messages_tab_enabled: false
    messages_tab_read_only_enabled: false
  bot_user:
    display_name: katie
    always_online: false
  slash_commands:
    - command: /katie
      url: https://app.katie.qa/api/v1/slack/command/katie
      description: Get help on how to use Katie
      should_escape: false
oauth_config:
  redirect_urls:
    - https://app.katie.qa/api/v1/slack/oauth2-callback/SLACK_CLIENT_ID
  scopes:
    user:
      - im:history
    bot:
      - channels:history
      - chat:write
      - commands
      - im:history
      - incoming-webhook
      - team:read
settings:
  event_subscriptions:
    request_url: https://app.katie.qa/api/v1/slack/events
    user_events:
      - message.im
    bot_events:
      - message.channels
      - message.im
  interactivity:
    is_enabled: true
    request_url: https://app.katie.qa/api/v1/slack/interactivity
    message_menu_options_url: https://app.katie.qa/api/v1/slack/options-load
  org_deploy_enabled: false
  socket_mode_enabled: false
  token_rotation_enabled: false
</pre>

## MS Teams App

Also see https://app.katie.qa/ms-teams.html

Open Developer Portal https://dev.teams.microsoft.com/apps

- Click on the "Apps" menu item
- Click on "New App"
- App details
  - Short name: Katie
  - Full name: Katie
  - App ID: Generate App ID, e.g. "a2ae51d0-19a4-416e-ba74-1812533d5be8"
  - Package Name: com.wyona.teams.devapp
  - Version: 1.0.0
  - Short description: Reliable Answers for your Employees and Customers
  - Full description: Reliable Answers for your Employees
  - Developer/Company Name: Wyona AG
  - Website: https://katie.qa
  - Privacy Statement: https://wyona.com/privacy-policy
  - Terms of use: https://katie.qa
  - Application (client) ID, e.g. "e9d6ff18-084d-4891-97ed-8a7667db3d7a"
- Branding
  - Upload logo 192x192 and 32x32 (see src/main/webapp/ms-teams-app)
- App features
  - Click on "Bot"
  - Click on "Existing bot"
  - Connect to a different bot id: e9d6ff18-084d-4891-97ed-8a7667db3d7a
    - Login to Azure Portal, click on katie7, click on Configuration, Copy "Microsoft App ID"
  - Scope
    - Personal
    - Team
    - Group Chat
- Publish
  - Click on "App package"
  - Click on "Download app package"
