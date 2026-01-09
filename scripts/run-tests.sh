#!/bin/bash
# Run all tests with coverage report

set -e

echo "========================================="
echo "Running Tests - SOFT40051 Cloud System"
echo "========================================="
echo ""

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Run tests with coverage
print_status "Running tests with coverage..."
mvn clean test jacoco:report

# Run tests for each service
print_status "Running AggService tests..."
cd AggService && mvn test && cd ..

print_status "Running Load Balancer tests..."
cd cloudlb && mvn test && cd ..

print_status "Running Cloud GUI tests..."
cd cloud-gui && mvn test && cd ..

print_status "Running Host Manager tests..."
cd hostmanager && mvn test && cd ..

echo ""
echo "========================================="
print_success "All tests completed!"
echo "========================================="
echo ""
echo "Coverage reports available at:"
echo "  - AggService:    AggService/target/site/jacoco/index.html"
echo "  - Load Balancer: cloudlb/target/site/jacoco/index.html"
echo "  - Cloud GUI:     cloud-gui/target/site/jacoco/index.html"
echo "  - Host Manager:  hostmanager/target/site/jacoco/index.html"
echo ""
