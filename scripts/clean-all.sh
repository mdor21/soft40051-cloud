#!/bin/bash
# Clean all build artifacts and Docker resources

set -e

echo "========================================="
echo "Cleaning SOFT40051 Cloud Storage System"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[CLEAN]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Parse arguments
DEEP_CLEAN=false
DOCKER_CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --deep)
            DEEP_CLEAN=true
            shift
            ;;
        --docker)
            DOCKER_CLEAN=true
            shift
            ;;
        --all)
            DEEP_CLEAN=true
            DOCKER_CLEAN=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--deep] [--docker] [--all]"
            echo "  --deep   : Also clean Maven cache and IDE files"
            echo "  --docker : Also clean Docker containers and volumes"
            echo "  --all    : Perform both deep and docker clean"
            exit 1
            ;;
    esac
done

# Clean Maven builds
print_status "Cleaning Maven build artifacts..."
mvn clean
cd AggService && mvn clean && cd ..
cd cloudlb && mvn clean && cd ..
cd cloud-gui && mvn clean && cd ..
cd hostmanager && mvn clean && cd ..
print_success "Maven build artifacts cleaned"
echo ""

# Clean IDE files
if [ "$DEEP_CLEAN" = true ]; then
    print_status "Performing deep clean..."
    
    # Remove IDE files
    find . -name "*.iml" -type f -delete
    find . -name ".idea" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name ".vscode" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name ".settings" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name ".classpath" -type f -delete 2>/dev/null || true
    find . -name ".project" -type f -delete 2>/dev/null || true
    
    # Remove OS files
    find . -name ".DS_Store" -type f -delete 2>/dev/null || true
    find . -name "Thumbs.db" -type f -delete 2>/dev/null || true
    
    print_success "Deep clean completed"
    echo ""
fi

# Clean Docker resources
if [ "$DOCKER_CLEAN" = true ]; then
    print_warning "Stopping Docker containers..."
    docker compose down -v 2>/dev/null || true
    
    print_warning "Removing Docker images..."
    docker compose down --rmi local 2>/dev/null || true
    
    print_success "Docker resources cleaned"
    echo ""
fi

echo "========================================="
print_success "Cleanup completed!"
echo "========================================="
