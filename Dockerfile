# ---- Frontend build ----
FROM node:24-slim AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ---- Backend build ----
FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist/frontend/browser ./src/main/resources/static

RUN mvn --batch-mode --no-transfer-progress -DskipTests package

# ---- Runtime ----
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/target/sisdent-*.jar app.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]