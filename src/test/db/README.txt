
  README
  ------

javac CreateTestData.java
java CreateTestData
cp afterMigrate.sql ../src/main/resources/db/testdata/.
Restart Katie, whereas make sure that the environment variable SPRING_PROFILES_ACTIVE=dev is set, such that src/main/resources/application-dev.properties is used
