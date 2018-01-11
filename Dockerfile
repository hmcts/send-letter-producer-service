FROM openjdk:8-jre

COPY build/install/send-letter-service /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8485/health

EXPOSE 8485

ENTRYPOINT ["/opt/app/bin/send-letter-service"]
