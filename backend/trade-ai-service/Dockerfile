# Build stage - install dependencies that need compilation
FROM python:3.11-slim AS builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python packages
COPY trade-ai-service/requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Runtime stage - minimal image
FROM python:3.11-slim

WORKDIR /app

# Install only runtime dependencies (curl for healthcheck)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Copy Python packages from builder
COPY --from=builder /root/.local /root/.local

# Copy application code
COPY trade-ai-service/src/main/python /app

# Set Python path and ensure local packages are in PATH
ENV PYTHONPATH=/app
ENV PATH=/root/.local/bin:$PATH

# Expose port
EXPOSE 8094

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD curl -f http://localhost:8094/health || exit 1

# Run the application
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8094"]

