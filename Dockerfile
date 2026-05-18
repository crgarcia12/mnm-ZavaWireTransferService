FROM gradle:7.6-jdk11 AS build
WORKDIR /app
COPY . .
RUN gradle war --no-daemon

FROM tomcat:9-jdk11
ENV DB_HOST=sqlserver
ENV DB_PORT=1433
ENV DB_NAME=ZavaBankDB
ENV DB_USER=sa
ENV DB_PASSWORD=YourStrong!Passw0rd
ENV CURRENCY_SERVICE_BASE_URL=http://zava-currency-service:8080
ENV LEDGER_SERVICE_BASE_URL=http://zava-ledger:8080
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USER=guest
ENV RABBITMQ_PASSWORD=guest
COPY --from=build /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
RUN rm -rf /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
CMD ["catalina.sh", "run"]
