#FROM openjdk:8-jre-alpine
#
#RUN mkdir /iam
#WORKDIR /iam
#ADD . /iam

# TODO: This cannot be used for prod as this uses jdk image. In prod we just need jre image which executes the pre-built jar

FROM openjdk:8-jdk-alpine
# as BUILD

RUN mkdir /iam
WORKDIR /iam
ADD . /iam
#RUN gradle wrapper
#RUN ./gradlew --no-daemon shadowJar
#WORKDIR /iam/build/libs/

#COPY ./docker/iam-web/run.sh /run.sh
#COPY ./kotlin-server.jar /kotlin-server.jar

#CMD ["java","-jar","kotlin-server.jar"]

#COPY . /kotlin-ktor-exposed-sample-api
#WORKDIR /kotlin-ktor-exposed-sample-api
#RUN ./gradlew --no-daemon shadowJar

#FROM openjdk:${VERSION}-jre-alpine
#
#COPY --from=BUILD /iam/build/libs/kotlin-ktor-exposed-sample-api-all.jar /bin/runner/run.jar
#WORKDIR /bin/runner
#
#CMD ["java","-jar","run.jar"]


#=========================

# COPY ./build/libs/kotlin-server.jar /root/kotlin-server.jar
#
# WORKDIR /iam

# CMD ["java", "-server", "-Xms4g", "-Xmx4g", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "kotlin-server.jar"]