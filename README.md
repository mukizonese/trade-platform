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
   docker-compose -f docker-compose.docker-local.core.yml --env-file .env.docker-local --profile docker-local up -d

### Local Setup

1. **Set up environment variables**
   cp env.local.template .env.local
   # Edit .env.docker-local with your configuration
   
2. **Start the service** 
   # cd to root directory
   ./run-local.sh [build | start | stop | restart| logs] [gateway | ingress | trade | ui]
   
   
     
### Docker Local Setup

1. **Set up environment variables**
   cp env.docker-local.template .env.docker-local
   # Edit .env.docker-local with your configuration
   
  
2. **Start the service** 
   # cd to root directory
   ./run-docker-local.sh [build | start | stop | restart| logs] [gateway | ingress | trade | nginx | ui]
   
   
