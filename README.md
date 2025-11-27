## Getting Started

### Prerequisites

- Java 17+
- Node.js 20+
- Docker and Docker Compose
- Maven 3.8+# trade-platform

### Docker Local Setup

1. **Set up environment variables**
   cp env.docker-local.template .env.docker-local
   # Edit .env.docker-local with your configuration
   
2. **Start all docker services with Docker Compose**
   docker-compose -f docker-compose.docker-local.yml --env-file .env.docker-local --profile docker-local up -d

### Local Setup

1. **Set up environment variables**
   cp env.local.template .env.local
   # Edit .env.docker-local with your configuration
   
2. **Start the backend service** 
   cd backend/trade-service
   set -a
   source /Users/muki-dev/Projects/eclipse-workspace/trade-platform/.env.local
   set +a
   
   cd backend/trade-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   
   Backend will be available at http://localhost:8088

     
### Docker Local Setup

1. **Set up environment variables**
   cp env.docker-local.template .env.docker-local
   # Edit .env.docker-local with your configuration
   
  
2. **Start the backend service** 
   cd backend/trade-service
   set -a
   source /Users/muki-dev/Projects/eclipse-workspace/trade-platform/.env.docker-local
   set +a
   
   cd backend/trade-service
   mvn spring-boot:run -Dspring-boot.run.profiles=docker-local
   ```
   
   
