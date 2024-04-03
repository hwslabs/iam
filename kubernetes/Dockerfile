FROM amazoncorretto:17.0.10-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

RUN apk add --no-cache bash openssl

RUN mkdir -p /opt/app
RUN mkdir -p /opt/app_logs

WORKDIR /opt/app

ENV LOG_DEST="/opt/app_logs"

EXPOSE 8080

COPY /build/libs/hypto-iam-server-1.0.0-all.jar /opt/app/app.jar

RUN chown -R appuser:appgroup /opt/app
RUN chown -R appuser:appgroup /opt/app_logs

USER appuser

CMD java -Xlog:gc*=debug:file=/opt/app_logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/app_logs/heap-dumps -XX:MaxMetaspaceSize=398928K  -XX:InitialRAMPercentage=70.0 -XX:MaxRAMPercentage=70.0 -jar /opt/app/app.jar
