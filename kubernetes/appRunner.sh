# Option e: Exit immediately if a command exits with a non-zero status
set -e

java -cp /opt/app/app.jar com.hypto.iam.server.MigrationHandler
java -Xlog:gc*=debug:file=/opt/app_logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/app_logs/heap-dumps -XX:MaxMetaspaceSize=398928K  -XX:InitialRAMPercentage=70.0 -XX:MaxRAMPercentage=70.0 -jar /opt/app/app.jar
