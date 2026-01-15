# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B


# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="GitHub SOC2 Audit Evidence Exporter"
LABEL org.opencontainers.image.description="Generate auditor-ready SOC2 evidence PDFs from GitHub"
LABEL org.opencontainers.image.vendor="jekka001"
LABEL org.opencontainers.image.source="https://github.com/jekka001/audit-evidence"

RUN apk add --no-cache fontconfig ttf-dejavu

WORKDIR /app

COPY --from=builder /app/target/audit-evidence-*.jar /app/audit-evidence.jar

ENTRYPOINT ["java", "-jar", "/app/audit-evidence.jar"]
