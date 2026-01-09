#!/bin/bash
# Build all services in the correct order

set -e  # Exit on error

echo "========================================="
echo "Building SOFT40051 Cloud Storage System"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[BUILD]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Parse command line arguments
SKIP_TESTS=false
CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--skip-tests] [--clean]"
            exit 1
            ;;
    esac
done

# Build parent project
print_status "Building parent project..."
if [ "$CLEAN" = true ]; then
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean install -DskipTests
    else
        mvn clean install
    fi
else
    if [ "$SKIP_TESTS" = true ]; then
        mvn install -DskipTests
    else
        mvn install
    fi
fi
print_success "Parent project built successfully"
echo ""

# Build AggService
print_status "Building AggService..."
cd AggService
if [ "$CLEAN" = true ]; then
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean package -DskipTests
    else
        mvn clean package
    fi
else
    if [ "$SKIP_TESTS" = true ]; then
        mvn package -DskipTests
    else
        mvn package
    fi
fi
cd ..
print_success "AggService built successfully"
echo ""

# Build cloudlb
print_status "Building Load Balancer..."
cd cloudlb
if [ "$CLEAN" = true ]; then
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean package -DskipTests
    else
        mvn clean package
    fi
else
    if [ "$SKIP_TESTS" = true ]; then
        mvn package -DskipTests
    else
        mvn package
    fi
fi
cd ..
print_success "Load Balancer built successfully"
echo ""

# Build cloud-gui
print_status "Building Cloud GUI..."
cd cloud-gui
if [ "$CLEAN" = true ]; then
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean package -DskipTests
    else
        mvn clean package
    fi
else
    if [ "$SKIP_TESTS" = true ]; then
        mvn package -DskipTests
    else
        mvn package
    fi
fi
cd ..
print_success "Cloud GUI built successfully"
echo ""

# Build hostmanager
print_status "Building Host Manager..."
cd hostmanager
if [ "$CLEAN" = true ]; then
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean package -DskipTests
    else
        mvn clean package
    fi
else
    if [ "$SKIP_TESTS" = true ]; then
        mvn package -DskipTests
    else
        mvn package
    fi
fi
cd ..
print_success "Host Manager built successfully"
echo ""

echo ""
echo "========================================="
print_success "All services built successfully!"
echo "========================================="
echo ""
echo "Built artifacts:"
echo "  - AggService:    AggService/target/*.jar"
echo "  - Load Balancer: cloudlb/target/*.jar"
echo "  - Cloud GUI:     cloud-gui/target/*.jar"
echo "  - Host Manager:  hostmanager/target/*.jar"
echo ""
