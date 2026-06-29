# ─────────────────────────────────────────────────────────────────────────
# BancoConfianza — Spring Boot Backend — Dockerfile
# Multi-stage build para optimizar tamaño
# ─────────────────────────────────────────────────────────────────────────

# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-21 AS builder

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
FROM eclipse-temurin:21-jdk-slim

WORKDIR /app

# Copiar JAR del builder
COPY --from=builder /build/target/homebanking-backend-1.0.0.jar app.jar

# Metadatos
LABEL maintainer="BancoConfianza <dev@bancoconfianza.pe>"
LABEL description="BancoConfianza Home Banking Backend"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.Main \
    -Dspring.profiles.active=prod || exit 1

# Exponer puerto
EXPOSE 8080

# Variables de entorno requeridas
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD [""]
