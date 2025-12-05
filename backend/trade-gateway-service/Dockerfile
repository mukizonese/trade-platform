# Multi-stage build for smaller final image
FROM eclipse-temurin:17-jre-jammy AS runtime

# Install only wget for health checks, then remove package manager cache
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /usr/share/doc/* /usr/share/man/*

WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    chown -R appuser:appuser /app

# Copy the JAR file (path relative to build context ./backend)
COPY --chown=appuser:appuser trade-gateway-service/target/trade-gateway-service-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

# Run with JVM optimizations for smaller memory footprint
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
