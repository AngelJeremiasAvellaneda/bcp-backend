# ─────────────────────────────────────────────────────────────────────────
# BancoConfianza — Spring Boot Backend — Dockerfile
# Multi-stage build para optimizar tamaño
# ─────────────────────────────────────────────────────────────────────────

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar pom.xml y descargar dependencias (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

# Copiar código fuente
COPY src ./src

# Build aplicación
RUN mvn clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────────

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR del builder
COPY --from=builder /build/target/homebanking-backend-1.0.0.jar app.jar

# Metadatos
LABEL maintainer="BancoConfianza <dev@bancoconfianza.pe>"
LABEL description="BancoConfianza Home Banking Backend"

# Exponer puerto
EXPOSE 8080

# Variables de entorno requeridas
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"
ENV PORT=8080

# Comando de inicio - FUERZA el profile prod
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
CMD [""]
