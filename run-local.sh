#!/bin/bash

# Unified script to manage Trade Platform services locally (non-Docker)
# Usage: ./run-local.sh [build|start|stop|restart|logs] [all|gateway|ingress|trade|ui|common] [verbose]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
ENV_FILE="$PROJECT_ROOT/.env.local"
PID_FILE="$PROJECT_ROOT/trade-platform-pids.local"
LOG_DIR="$PROJECT_ROOT/logs"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend/trade-ui"

# Service names and directories
SERVICE_COMMON="trade-common-service"
SERVICE_TRADE="trade-service"
SERVICE_INGRESS="trade-ingress-service"
SERVICE_GATEWAY="trade-gateway-service"
SERVICE_UI="trade-ui"

# Load environment variables if .env.local exists (must be done BEFORE setting port variables)
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE" 2>/dev/null || true
    set +a
fi

# Service ports (with defaults if env vars not set)
PORT_TRADE=${TRADE_SERVICE_PORT:-8080}
PORT_INGRESS=${INGRESS_PORT:-8081}
PORT_GATEWAY=${GATEWAY_PORT:-8085}
PORT_UI=${FRONTEND_PORT:-3000}

# Create logs directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Function to get service directory name
get_service_dir() {
    local service=$1
    case $service in
        common) echo "$SERVICE_COMMON" ;;
        trade) echo "$SERVICE_TRADE" ;;
        ingress) echo "$SERVICE_INGRESS" ;;
        gateway) echo "$SERVICE_GATEWAY" ;;
        ui) echo "$SERVICE_UI" ;;
        *) echo "" ;;
    esac
}

# Function to get service port
get_service_port() {
    local service=$1
    case $service in
        trade) echo "$PORT_TRADE" ;;
        ingress) echo "$PORT_INGRESS" ;;
        gateway) echo "$PORT_GATEWAY" ;;
        ui) echo "$PORT_UI" ;;
        *) echo "" ;;
    esac
}

# Function to build common service
build_common() {
    echo "=========================================="
    echo "Building $SERVICE_COMMON..."
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

# Function to build a specific backend service
build_service() {
    local service=$1
    local service_name=$(get_service_dir "$service")
    
    if [ -z "$service_name" ]; then
        echo "Error: Invalid service '$service'"
        exit 1
    fi
    
    local service_dir="$BACKEND_DIR/$service_name"
    
    if [ ! -d "$service_dir" ]; then
        echo "Error: Service directory not found: $service_dir"
        exit 1
    fi
    
    echo "=========================================="
    echo "Building $service_name..."
    echo "=========================================="
    cd "$service_dir"
    mvn clean install -Ddependency-check.skip=true
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build $service_name"
        exit 1
    fi
    echo "✓ $service_name built successfully"
    echo ""
}

# Function to build frontend
build_frontend() {
    echo "=========================================="
    echo "Building $SERVICE_UI..."
    echo "=========================================="
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        echo "Error: Frontend directory not found: $FRONTEND_DIR"
        exit 1
    fi
    
    cd "$FRONTEND_DIR"
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        echo "Installing npm dependencies..."
        npm install
        if [ $? -ne 0 ]; then
            echo "Error: Failed to install npm dependencies"
            exit 1
        fi
    fi
    
    # Build the frontend
    echo "Building Next.js application..."
    npm run build
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build frontend"
        exit 1
    fi
    
    echo "✓ $SERVICE_UI built successfully"
    echo ""
}

# Function to start a specific backend service
start_service() {
    local service=$1
    local service_name=$(get_service_dir "$service")
    local port=$(get_service_port "$service")
    
    if [ -z "$service_name" ]; then
        echo "Error: Invalid service '$service'"
        exit 1
    fi
    
    if [ -z "$port" ]; then
        echo "Error: No port configured for service '$service'"
        exit 1
    fi
    
    local service_dir="$BACKEND_DIR/$service_name"
    local log_file="$LOG_DIR/${service_name}.local.log"
    
    if [ ! -d "$service_dir" ]; then
        echo "Error: Service directory not found: $service_dir"
        exit 1
    fi
    
    # Check if service is already running
    if pgrep -f "${service_name}.*spring-boot:run" > /dev/null; then
        echo "⚠ $service_name is already running. Use 'restart' command to restart it."
        return 0
    fi
    
    # Export environment variables for service routing
    if [ "$service" = "ingress" ]; then
        export TRADE_SERVICE_URL=${TRADE_SERVICE_URL:-http://localhost:$PORT_TRADE}
        export INGRESS_PORT=$PORT_INGRESS
    fi
    if [ "$service" = "gateway" ]; then
        export GATEWAY_TRADE_INGRESS_URL=${GATEWAY_TRADE_INGRESS_URL:-http://localhost:$PORT_INGRESS}
    fi
    
    echo "Starting $service_name on port ${port}..."
    cd "$service_dir"
    mvn spring-boot:run -Dspring-boot.run.profiles=local > "$log_file" 2>&1 &
    local pid=$!
    
    # Wait a moment to check if process started successfully
    sleep 2
    if ! kill -0 $pid 2>/dev/null; then
        echo "❌ Error: Failed to start $service_name"
        echo "Check logs: $log_file"
        exit 1
    fi
    
    # Save PID to file
    echo "${service}:${pid}:${port}" >> "$PID_FILE"
    echo "✓ $service_name started (PID: $pid, Port: $port)"
    echo "  Logs: tail -f $log_file"
    echo ""
}

# Function to start frontend
start_frontend() {
    local port=$(get_service_port "ui")
    local log_file="$LOG_DIR/${SERVICE_UI}.local.log"
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        echo "Error: Frontend directory not found: $FRONTEND_DIR"
        exit 1
    fi
    
    # Check if frontend is already running
    if pgrep -f "next.*dev\|next.*start" > /dev/null; then
        echo "⚠ $SERVICE_UI is already running. Use 'restart' command to restart it."
        return 0
    fi
    
    # Check if built
    if [ ! -d "$FRONTEND_DIR/.next" ]; then
        echo "Frontend not built. Building first..."
        build_frontend
    fi
    
    echo "Starting $SERVICE_UI on port ${port}..."
    cd "$FRONTEND_DIR"
    
    # For local development, always use dev mode
    PORT=$port npm run dev > "$log_file" 2>&1 &
    
    local pid=$!
    
    # Wait a moment to check if process started successfully
    sleep 2
    if ! kill -0 $pid 2>/dev/null; then
        echo "❌ Error: Failed to start $SERVICE_UI"
        echo "Check logs: $log_file"
        exit 1
    fi
    
    # Save PID to file
    echo "ui:${pid}:${port}" >> "$PID_FILE"
    echo "✓ $SERVICE_UI started (PID: $pid, Port: $port)"
    echo "  Logs: tail -f $log_file"
    echo ""
}

# Function to stop a specific service
stop_service() {
    local service=$1
    local service_name=$(get_service_dir "$service")
    
    if [ -z "$service_name" ]; then
        echo "Error: Invalid service '$service'"
        exit 1
    fi
    
    if [ "$service" = "ui" ]; then
        # Stop frontend (Next.js)
        local pids=$(pgrep -f "next.*dev\|next.*start" || true)
        if [ -z "$pids" ]; then
            echo "⚠ $SERVICE_UI is not running"
            return 0
        fi
        echo "Stopping $SERVICE_UI..."
        for pid in $pids; do
            kill $pid 2>/dev/null || true
        done
        sleep 2
        for pid in $pids; do
            if kill -0 $pid 2>/dev/null; then
                kill -9 $pid 2>/dev/null || true
            fi
        done
    else
        # Stop backend service
        local pids=$(pgrep -f "${service_name}.*spring-boot:run" || true)
        
        if [ -z "$pids" ]; then
            echo "⚠ $service_name is not running"
            return 0
        fi
        
        echo "Stopping $service_name..."
        for pid in $pids; do
            kill $pid 2>/dev/null || true
        done
        
        # Wait for graceful shutdown
        sleep 3
        
        # Force kill if still running
        for pid in $pids; do
            if kill -0 $pid 2>/dev/null; then
                kill -9 $pid 2>/dev/null || true
            fi
        done
    fi
    
    # Remove from PID file
    if [ -f "$PID_FILE" ]; then
        sed -i.bak "/^${service}:/d" "$PID_FILE" 2>/dev/null || sed -i "/^${service}:/d" "$PID_FILE"
        rm -f "${PID_FILE}.bak" 2>/dev/null || true
    fi
    
    echo "✓ $service_name stopped"
    echo ""
}

# Function to stop all services
stop_all() {
    echo "=========================================="
    echo "Stopping all Trade Platform services..."
    echo "=========================================="
    
    stop_service "ui"
    stop_service "gateway"
    stop_service "ingress"
    stop_service "trade"
    
    # Clean up PID file
    rm -f "$PID_FILE" 2>/dev/null || true
    
    echo "=========================================="
    echo "All services stopped"
    echo "=========================================="
}

# Function to show logs
show_logs() {
    local service=$1
    local filter=${2:-true}
    
    # Filter pattern for verbose messages
    local filter_pattern='(WARN|DEBUG|Connection ended|Connection to node|Bootstrap broker.*disconnected|Unable to read additional data|ConnectionCount|NETWORK.*Connection|ctx.*conn)'
    
    if [ "$service" = "all" ]; then
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            tail -f "$LOG_DIR/$SERVICE_TRADE.local.log" "$LOG_DIR/$SERVICE_INGRESS.local.log" "$LOG_DIR/$SERVICE_GATEWAY.local.log" "$LOG_DIR/$SERVICE_UI.local.log" 2>/dev/null | grep -v -E "$filter_pattern" || true
        else
            tail -f "$LOG_DIR/$SERVICE_TRADE.local.log" "$LOG_DIR/$SERVICE_INGRESS.local.log" "$LOG_DIR/$SERVICE_GATEWAY.local.log" "$LOG_DIR/$SERVICE_UI.local.log" 2>/dev/null
        fi
    elif [ "$service" = "ui" ]; then
        local log_file="$LOG_DIR/${SERVICE_UI}.local.log"
        if [ ! -f "$log_file" ]; then
            echo "Log file not found: $log_file"
            exit 1
        fi
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            tail -f "$log_file" 2>/dev/null | grep -v -E "$filter_pattern" || true
        else
            tail -f "$log_file" 2>/dev/null
        fi
    else
        local service_name=$(get_service_dir "$service")
        if [ -z "$service_name" ]; then
            echo "Error: Invalid service '$service'"
            exit 1
        fi
        local log_file="$LOG_DIR/${service_name}.local.log"
        if [ ! -f "$log_file" ]; then
            echo "Log file not found: $log_file"
            exit 1
        fi
        if [ "$filter" = "true" ] || [ "$filter" = "filter" ]; then
            tail -f "$log_file" 2>/dev/null | grep -v -E "$filter_pattern" || true
        else
            tail -f "$log_file" 2>/dev/null
        fi
    fi
}

# Function to show status
show_status() {
    echo "=========================================="
    echo "Trade Platform Services Status"
    echo "=========================================="
    
    local services=("trade" "ingress" "gateway" "ui")
    for service in "${services[@]}"; do
        local service_name=$(get_service_dir "$service")
        local port=$(get_service_port "$service")
        
        if [ "$service" = "ui" ]; then
            if pgrep -f "next.*dev\|next.*start" > /dev/null; then
                echo "✓ $service_name - Running (Port: $port)"
            else
                echo "✗ $service_name - Stopped"
            fi
        else
            if pgrep -f "${service_name}.*spring-boot:run" > /dev/null; then
                echo "✓ $service_name - Running (Port: $port)"
            else
                echo "✗ $service_name - Stopped"
            fi
        fi
    done
    echo ""
}

# Parse command line arguments
COMMAND=${1:-help}
SERVICE=${2:-all}
ARG3=${3:-}

case $COMMAND in
    build)
        case $SERVICE in
            common)
                build_common
                ;;
            trade|ingress|gateway)
                build_common
                build_service "$SERVICE"
                echo "=========================================="
                echo "Build completed successfully!"
                echo "=========================================="
                ;;
            ui)
                build_frontend
                echo "=========================================="
                echo "Build completed successfully!"
                echo "=========================================="
                ;;
            all)
                build_common
                build_service "trade"
                build_service "ingress"
                build_service "gateway"
                build_frontend
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
        # Ensure common is built first (for backend services)
        if [ "$SERVICE" != "ui" ] && [ "$SERVICE" != "all" ]; then
        if [ ! -f "$BACKEND_DIR/$SERVICE_COMMON/target/$SERVICE_COMMON-1.0.0-SNAPSHOT.jar" ]; then
            echo "Building $SERVICE_COMMON first..."
            build_common
        fi
        fi
        
        case $SERVICE in
            common)
                echo "Error: Cannot start common service (it's a library, not a runnable service)"
                exit 1
                ;;
            trade|ingress|gateway)
                start_service "$SERVICE"
                ;;
            ui)
                start_frontend
                ;;
            all)
                echo "=========================================="
                echo "Starting all Trade Platform services..."
                echo "=========================================="
                start_service "trade"
                sleep 3
                start_service "ingress"
                sleep 3
                start_service "gateway"
                sleep 2
                start_frontend
                echo "=========================================="
                echo "All services started!"
                echo "=========================================="
                echo "Trade Service:      http://localhost:$PORT_TRADE"
                echo "Ingress Service:    http://localhost:$PORT_INGRESS"
                echo "Gateway Service:    http://localhost:$PORT_GATEWAY"
                echo "Frontend UI:        http://localhost:$PORT_UI"
                echo ""
                echo "Log files:"
                echo "  Trade:    $LOG_DIR/$SERVICE_TRADE.local.log"
                echo "  Ingress:  $LOG_DIR/$SERVICE_INGRESS.local.log"
                echo "  Gateway:  $LOG_DIR/$SERVICE_GATEWAY.local.log"
                echo "  UI:       $LOG_DIR/$SERVICE_UI.local.log"
                echo ""
                echo "To view logs: $0 logs $SERVICE"
                echo "To stop all: $0 stop all"
                ;;
            *)
                echo "Error: Invalid service '$SERVICE'"
                echo "Valid services: all, gateway, ingress, trade, ui"
                exit 1
                ;;
        esac
        ;;
    
    stop)
        case $SERVICE in
            common)
                echo "Error: Cannot stop common service (it's a library, not a runnable service)"
                exit 1
                ;;
            trade|ingress|gateway|ui)
                stop_service "$SERVICE"
                ;;
            all)
                stop_all
                ;;
            *)
                echo "Error: Invalid service '$SERVICE'"
                echo "Valid services: all, gateway, ingress, trade, ui"
                exit 1
                ;;
        esac
        ;;
    
    restart)
        # Ensure common is built first (for backend services)
        if [ "$SERVICE" != "ui" ] && [ "$SERVICE" != "all" ]; then
        if [ ! -f "$BACKEND_DIR/$SERVICE_COMMON/target/$SERVICE_COMMON-1.0.0-SNAPSHOT.jar" ]; then
            echo "Building $SERVICE_COMMON first..."
            build_common
        fi
        fi
        
        case $SERVICE in
            common)
                echo "Error: Cannot restart common service (it's a library, not a runnable service)"
                exit 1
                ;;
            trade|ingress|gateway)
                echo "=========================================="
                echo "Restarting $SERVICE service..."
                echo "=========================================="
                stop_service "$SERVICE"
                sleep 2
                start_service "$SERVICE"
                echo "=========================================="
                echo "$SERVICE service restarted!"
                echo "=========================================="
                ;;
            ui)
                echo "=========================================="
                echo "Restarting $SERVICE_UI..."
                echo "=========================================="
                stop_service "ui"
                sleep 2
                start_frontend
                echo "=========================================="
                echo "$SERVICE_UI restarted!"
                echo "=========================================="
                ;;
            all)
                echo "=========================================="
                echo "Restarting all Trade Platform services..."
                echo "=========================================="
                stop_all
                sleep 3
                start_service "trade"
                sleep 3
                start_service "ingress"
                sleep 3
                start_service "gateway"
                sleep 2
                start_frontend
                echo "=========================================="
                echo "All services restarted!"
                echo "=========================================="
                echo "Trade Service:      http://localhost:$PORT_TRADE"
                echo "Ingress Service:    http://localhost:$PORT_INGRESS"
                echo "Gateway Service:    http://localhost:$PORT_GATEWAY"
                echo "Frontend UI:        http://localhost:$PORT_UI"
                echo ""
                echo "To view logs: $0 logs $SERVICE"
                echo "To stop all: $0 stop all"
                ;;
            *)
                echo "Error: Invalid service '$SERVICE'"
                echo "Valid services: all, gateway, ingress, trade, ui"
                exit 1
                ;;
        esac
        ;;
    
    logs)
        # Check if user wants unfiltered logs (3rd arg = "all" or "verbose")
        local filter="true"
        if [ "$ARG3" = "all" ] || [ "$ARG3" = "verbose" ] || [ "$ARG3" = "v" ]; then
            filter="false"
        fi
        show_logs "$SERVICE" "$filter"
        ;;
    
    status|ps)
        show_status
        ;;
    
    help|*)
        echo "Usage: $0 [build|start|stop|restart|logs|status] [all|gateway|ingress|trade|ui|common] [verbose]"
        echo ""
        echo "Commands:"
        echo "  build   - Build service(s) and dependencies"
        echo "  start   - Start service(s) (builds common if needed, skips if already running)"
        echo "  stop    - Stop running service(s)"
        echo "  restart - Stop and start service(s) (builds common if needed)"
        echo "  logs    - Show logs for service(s) (filtered by default)"
        echo "           - Add 'verbose' to show all logs including WARN/DEBUG"
        echo "  status  - Show status of all services"
        echo ""
        echo "Services:"
        echo "  all      - All services (gateway, ingress, trade, ui)"
        echo "  gateway  - Gateway service (port $PORT_GATEWAY)"
        echo "  ingress  - Ingress service (port $PORT_INGRESS)"
        echo "  trade    - Trade service (port $PORT_TRADE)"
        echo "  ui       - Frontend UI (port $PORT_UI)"
        echo "  common   - Common library (build only)"
        echo ""
        echo "Examples:"
        echo "  $0 build all          # Build all services"
        echo "  $0 build trade        # Build trade service"
        echo "  $0 build ui           # Build frontend UI"
        echo "  $0 start all          # Start all services"
        echo "  $0 start trade        # Start trade service only"
        echo "  $0 start ui           # Start frontend UI only"
        echo "  $0 stop all           # Stop all services"
        echo "  $0 stop gateway       # Stop gateway service only"
        echo "  $0 restart all        # Restart all services"
        echo "  $0 logs trade         # Show trade service logs (filtered)"
        echo "  $0 logs trade verbose # Show all logs including WARN/DEBUG"
        echo "  $0 logs all           # Show all services logs (filtered)"
        echo "  $0 status             # Show status of all services"
        exit 1
        ;;
esac
