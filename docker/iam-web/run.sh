#!/bin/bash
pwd
ls
java -jar kotlin-server.jar
#java -jar /iam/libs/kotlin-server.jar

#java -server -Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -jar kotlin-server.jar

#CMD ["java", "-server", "-Xms4g", "-Xmx4g", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "kotlin-server.jar"]