#!/bin/bash

# Unified script to manage Trade Platform services with docker-local profile using Docker Compose
# Usage: ./run-docker-local.sh [build|start|stop|restart|scale|logs] [all|gateway|ingress|trade|ui|common] [replicas]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.docker-local"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.docker-local.services.yml"
BACKEND_DIR="$SCRIPT_DIR/backend"

# Service names
SERVICE_COMMON="trade-common-service"
SERVICE_TRADE="trade-service"
SERVICE_INGRESS="trade-ingress-service"
SERVICE_GATEWAY="trade-gateway-service"
SERVICE_UI="trade-ui"
SERVICE_NGINX="nginx-trade-service"

# Check if .env.docker-local exists
if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env.docker-local file not found at $ENV_FILE"
    echo "Please copy env.docker-local.template to .env.docker-local and configure it"
    exit 1
fi

# Check if docker-compose file exists
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "Error: docker-compose.docker-local.yml file not found at $COMPOSE_FILE"
    exit 1
fi

# Docker Compose command with profile
COMPOSE_CMD="docker compose -f $COMPOSE_FILE --profile docker-local --env-file $ENV_FILE"

# Function to build common service (Maven)
build_common() {
    echo "=========================================="
    echo "Building $SERVICE_COMMON (Maven)..."
    echo "=========================================="
    cd "$BACKEND_DIR/$SERVICE_COMMON"
    mvn clean install -DskipTests -Ddependency-check.skip=true
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build $SERVICE_COMMON"
        exit 1
    fi
    echo "✓ $SERVICE_COMMON built successfully"
    echo ""
}

# Function to build backend services (Maven + Docker)
build_backend_service() {
    local service=$1
    local service_name=""
    
    case $service in
        trade) service_name="$SERVICE_TRADE" ;;
        ingress) service_name="$SERVICE_INGRESS" ;;
        gateway) service_name="$SERVICE_GATEWAY" ;;
        *) echo "Error: Invalid backend service '$service'"; exit 1 ;;
    esac
    
    echo "=========================================="
    echo "Building $service_name (Maven)..."
    echo "=========================================="
    cd "$BACKEND_DIR/$service_name"
    mvn clean install -Ddependency-check.skip=true
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build $service_name"
        exit 1
    fi
    echo "✓ $service_name built successfully (Maven)"
    echo ""
}

# Function to build frontend (Docker only, npm build happens in Dockerfile)
build_frontend() {
    echo "=========================================="
    echo "Frontend will be built in Docker..."
    echo "=========================================="
}

# Function to build Docker images
build_docker() {
    local service=$1
    
    if [ "$service" = "all" ]; then
        echo "=========================================="
        echo "Building all Docker images..."
        echo "=========================================="
        $COMPOSE_CMD build
    elif [ "$service" = "ui" ]; then
        echo "=========================================="
        echo "Building trade-ui Docker image..."
        echo "=========================================="
        $COMPOSE_CMD build trade-ui
    elif [ "$service" = "nginx" ]; then
        echo "=========================================="
        echo "Nginx doesn't need building (uses nginx:alpine image)..."
        echo "=========================================="
    else
        local compose_service=""
        case $service in
            trade) compose_service="trade-service" ;;
            ingress) compose_service="trade-ingress-service" ;;
            gateway) compose_service="trade-gateway-service" ;;
            *) echo "Error: Invalid service '$service'"; exit 1 ;;
        esac
        
        echo "=========================================="
        echo "Building $compose_service Docker image..."
        echo "=========================================="
        $COMPOSE_CMD build "$compose_service"
    fi
    
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build Docker image(s)"
        exit 1
    fi
    echo "✓ Docker image(s) built successfully"
    echo ""
}

# Function to start services
start_services() {
    local service=$1
    
    if [ "$service" = "all" ]; then
        echo "=========================================="
        echo "Starting all Trade Platform services..."
        echo "=========================================="
        $COMPOSE_CMD up -d
    elif [ "$service" = "ui" ]; then
        echo "=========================================="
        echo "Starting trade-ui..."
        echo "=========================================="
        $COMPOSE_CMD up -d trade-ui
    elif [ "$service" = "nginx" ]; then
        echo "=========================================="
        echo "Starting nginx-trade-service..."
        echo "=========================================="
        $COMPOSE_CMD up -d nginx-trade-service
    else
        local compose_service=""
        case $service in
            trade) compose_service="trade-service" ;;
            ingress) compose_service="trade-ingress-service" ;;
            gateway) compose_service="trade-gateway-service" ;;
            *) echo "Error: Invalid service '$service'"; exit 1 ;;
        esac
        
        echo "=========================================="
        echo "Starting $compose_service..."
        echo "=========================================="
        $COMPOSE_CMD up -d "$compose_service"
    fi
    
    if [ $? -eq 0 ]; then
        echo "✓ Service(s) started successfully"
        echo ""
        echo "To view logs: $0 logs $service"
        echo "To scale: $0 scale $service <replicas>"
    else
        echo "Error: Failed to start service(s)"
        exit 1
    fi
}

# Function to stop services
stop_services() {
    local service=$1
    
    if [ "$service" = "all" ]; then
        echo "=========================================="
        echo "Stopping all Trade Platform services..."
        echo "=========================================="
        $COMPOSE_CMD stop
    elif [ "$service" = "ui" ]; then
        echo "=========================================="
        echo "Stopping trade-ui..."
        echo "=========================================="
        $COMPOSE_CMD stop trade-ui
    elif [ "$service" = "nginx" ]; then
        echo "=========================================="
        echo "Stopping nginx-trade-service..."
        echo "=========================================="
        $COMPOSE_CMD stop nginx-trade-service
    else
        local compose_service=""
        case $service in
            trade) compose_service="trade-service" ;;
            ingress) compose_service="trade-ingress-service" ;;
            gateway) compose_service="trade-gateway-service" ;;
            *) echo "Error: Invalid service '$service'"; exit 1 ;;
        esac
        
        echo "=========================================="
        echo "Stopping $compose_service..."
        echo "=========================================="
        $COMPOSE_CMD stop "$compose_service"
    fi
    
    echo "✓ Service(s) stopped"
    echo ""
}

# Function to restart services
restart_services() {
    local service=$1
    
    echo "=========================================="
    echo "Restarting service(s)..."
    echo "=========================================="
    stop_services "$service"
    sleep 2
    start_services "$service"
    echo "✓ Service(s) restarted"
    echo ""
}

# Function to scale services
scale_service() {
    local service=$1
    local replicas=${2:-1}
    
    if [ -z "$service" ] || [ "$service" = "all" ]; then
        echo "Error: Cannot scale 'all'. Please specify a service: trade, ingress, gateway"
        exit 1
    fi
    
    local compose_service=""
    case $service in
        trade) compose_service="trade-service" ;;
        ingress) compose_service="trade-ingress-service" ;;
        gateway) compose_service="trade-gateway-service" ;;
        ui|nginx)
            echo "Error: $service service cannot be scaled (single instance only)"
            exit 1
            ;;
        *)
            echo "Error: Invalid service '$service'"
            exit 1
            ;;
    esac
    
    echo "=========================================="
    echo "Scaling $compose_service to $replicas replica(s)..."
    echo "=========================================="
    
    $COMPOSE_CMD up -d --scale "$compose_service=$replicas" --no-recreate "$compose_service"
    
    if [ $? -eq 0 ]; then
        echo "✓ $compose_service scaled to $replicas replica(s)"
        echo ""
        echo "Current status:"
        $COMPOSE_CMD ps "$compose_service"
    else
        echo "Error: Failed to scale $compose_service"
        exit 1
    fi
}

# Function to show logs
show_logs() {
    local service=$1
    local filter=${2:-true}
    
    if [ "$service" = "all" ]; then
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            # Filter out verbose messages: WARN, DEBUG, connection messages, etc.
            $COMPOSE_CMD logs -f 2>&1 | grep -v -E '(WARN|DEBUG|Connection ended|Connection to node|Bootstrap broker.*disconnected|Unable to read additional data|ConnectionCount|NETWORK.*Connection|ctx.*conn)' || true
        else
            $COMPOSE_CMD logs -f
        fi
    elif [ "$service" = "ui" ]; then
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            $COMPOSE_CMD logs -f trade-ui 2>&1 | grep -v -E '(WARN|DEBUG|Connection ended|Connection to node|Bootstrap broker.*disconnected|Unable to read additional data|ConnectionCount|NETWORK.*Connection|ctx.*conn)' || true
        else
            $COMPOSE_CMD logs -f trade-ui
        fi
    elif [ "$service" = "nginx" ]; then
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            $COMPOSE_CMD logs -f nginx-trade-service 2>&1 | grep -v -E '(WARN|DEBUG|Connection ended|Connection to node|Bootstrap broker.*disconnected|Unable to read additional data|ConnectionCount|NETWORK.*Connection|ctx.*conn)' || true
        else
            $COMPOSE_CMD logs -f nginx-trade-service
        fi
    else
        local compose_service=""
        case $service in
            trade) compose_service="trade-service" ;;
            ingress) compose_service="trade-ingress-service" ;;
            gateway) compose_service="trade-gateway-service" ;;
            *) echo "Error: Invalid service '$service'"; exit 1 ;;
        esac
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            $COMPOSE_CMD logs -f "$compose_service" 2>&1 | grep -v -E '(WARN|DEBUG|Connection ended|Connection to node|Bootstrap broker.*disconnected|Unable to read additional data|ConnectionCount|NETWORK.*Connection|ctx.*conn)' || true
        else
            $COMPOSE_CMD logs -f "$compose_service"
        fi
    fi
}

# Function to show status
show_status() {
    echo "=========================================="
    echo "Trade Platform Services Status"
    echo "=========================================="
    $COMPOSE_CMD ps
    echo ""
}

# Parse command line arguments
COMMAND=${1:-help}
SERVICE=${2:-all}
REPLICAS=${3:-1}

case $COMMAND in
    build)
        case $SERVICE in
            common)
                build_common
                ;;
            trade|ingress|gateway)
                build_common
                build_backend_service "$SERVICE"
                build_docker "$SERVICE"
                echo "=========================================="
                echo "Build completed successfully!"
                echo "=========================================="
                ;;
            ui)
                build_frontend
                build_docker "ui"
                echo "=========================================="
                echo "Build completed successfully!"
                echo "=========================================="
                ;;
            nginx)
                echo "=========================================="
                echo "Nginx doesn't need building (uses nginx:alpine image)..."
                echo "=========================================="
                ;;
            all)
                build_common
                build_backend_service "trade"
                build_backend_service "ingress"
                build_backend_service "gateway"
                build_frontend
                build_docker "all"
                echo "=========================================="
                echo "All services built successfully!"
                echo "=========================================="
                ;;
            *)
                echo "Error: Invalid service '$SERVICE'"
                echo "Valid services: all, gateway, ingress, trade, ui, common"
                exit 1
                ;;
        esac
        ;;
    
    start)
        start_services "$SERVICE"
        if [ "$SERVICE" = "all" ]; then
            sleep 3
            show_status
        fi
        ;;
    
    stop)
        stop_services "$SERVICE"
        ;;
    
    restart)
        restart_services "$SERVICE"
        if [ "$SERVICE" = "all" ]; then
            sleep 3
            show_status
        fi
        ;;
    
    scale)
        if [ "$SERVICE" = "all" ]; then
            echo "Error: Cannot scale 'all'. Please specify a service: trade, ingress, gateway"
            exit 1
        fi
        scale_service "$SERVICE" "$REPLICAS"
        ;;
    
    logs)
        # Check if user wants unfiltered logs (3rd arg = "all" or "verbose")
        local filter="true"
        if [ "$3" = "all" ] || [ "$3" = "verbose" ] || [ "$3" = "v" ]; then
            filter="false"
        fi
        show_logs "$SERVICE" "$filter"
        ;;
    
    status|ps)
        show_status
        ;;
    
    help|*)
        echo "Usage: $0 [build|start|stop|restart|scale|logs|status] [all|gateway|ingress|trade|ui|nginx|common] [replicas]"
        echo ""
        echo "Commands:"
        echo "  build   - Build service(s) (Maven + Docker)"
        echo "  start   - Start service(s) in Docker"
        echo "  stop    - Stop service(s)"
        echo "  restart - Restart service(s)"
        echo "  scale   - Scale a service to N replicas (trade, ingress, gateway only)"
        echo "  logs    - Show logs for service(s) (filtered by default)"
        echo "           - Add 'all' or 'verbose' to show all logs including WARN/DEBUG"
        echo "  status  - Show status of all services"
        echo ""
        echo "Services:"
        echo "  all      - All services"
        echo "  gateway  - Gateway service"
        echo "  ingress  - Ingress service"
        echo "  trade    - Trade service"
        echo "  ui       - Frontend UI"
        echo "  nginx    - Nginx load balancer for trade-service"
        echo "  common   - Common library (build only)"
        echo ""
        echo "Examples:"
        echo "  $0 build all              # Build all services"
        echo "  $0 build trade           # Build trade service"
        echo "  $0 start all             # Start all services"
        echo "  $0 start trade           # Start trade service only"
        echo "  $0 start nginx           # Start nginx load balancer"
        echo "  $0 stop all               # Stop all services"
        echo "  $0 restart trade          # Restart trade service"
        echo "  $0 restart nginx          # Restart nginx load balancer"
        echo "  $0 scale trade 3          # Scale trade service to 3 replicas"
        echo "  $0 logs trade             # Show trade service logs (filtered)"
        echo "  $0 logs nginx             # Show nginx logs (filtered)"
        echo "  $0 logs trade verbose     # Show all logs including WARN/DEBUG"
        echo "  $0 logs all               # Show all services logs (filtered)"
        echo "  $0 status                 # Show status of all services"
        exit 1
        ;;
esac

