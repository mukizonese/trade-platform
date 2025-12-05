FROM eclipse-temurin:17-jre

# Install wget for health checks
RUN apt-get update && apt-get install -y --no-install-recommends wget curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the JAR file (path relative to build context ./backend)
COPY trade-ingress-service/target/trade-ingress-service-*.jar app.jar

# Expose port
EXPOSE 8091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8091/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

