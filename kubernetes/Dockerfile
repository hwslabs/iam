FROM amazoncorretto:17.0.10-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

RUN apk add --no-cache bash openssl

RUN mkdir -p /opt/app
RUN mkdir -p /opt/app_logs

WORKDIR /opt/app

ENV LOG_DEST="/opt/app_logs"

EXPOSE 8080

COPY /build/libs/hypto-iam-server-1.0.0-all.jar /opt/app/app.jar

COPY /kubernetes/appRunner.sh /opt/app/appRunner.sh

RUN chown -R appuser:appgroup /opt/app
RUN chown -R appuser:appgroup /opt/app_logs

USER appuser

CMD sh -c /opt/app/appRunner.sh
