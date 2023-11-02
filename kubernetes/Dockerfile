FROM amazoncorretto:11.0.20-alpine

RUN apk add --no-cache bash openssl

RUN mkdir -p /opt/app
RUN mkdir -p /opt/app_logs

WORKDIR /opt/app

ENV LOG_DEST="/opt/app_logs"

EXPOSE 8080

COPY /build/libs/hypto-iam-server-1.0.0-all.jar /opt/app/app.jar

CMD java -Xlog:gc*=debug:file=/opt/app_logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/app_logs/heap-dumps -XX:MaxMetaspaceSize=398928K -Xms1000000K -Xmx1000000K -jar /opt/app/app.jar > /dev/null
