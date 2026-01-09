#!/bin/bash
# Service Health Check Script
# Verifies that all critical services are functional after build

set -e

echo "========================================="
echo "Service Health Check - Post-Merge"
echo "========================================="

SERVICES=("AggService" "cloudlb" "cloud-gui" "hostmanager")
FAILED_SERVICES=()

# Function to check if service JAR exists
check_service_jar() {
    local service=$1
    echo ""
    echo "Checking $service..."
    
    # Find JAR files in target directory (excluding javadoc and sources)
    JAR_FILES=$(find "$service/target" -name "*.jar" -not -name "*javadoc*" -not -name "*sources*" 2>/dev/null | head -1)
    
    if [ -n "$JAR_FILES" ]; then
        echo "✓ $service JAR exists: $JAR_FILES"
        # Check JAR is not empty
        SIZE=$(stat -c%s "$JAR_FILES")
        if [ "$SIZE" -gt 1000 ]; then
            echo "✓ $service JAR size is valid: $SIZE bytes"
            return 0
        else
            echo "✗ $service JAR is too small: $SIZE bytes"
            FAILED_SERVICES+=("$service")
            return 1
        fi
    else
        echo "✗ $service JAR not found in $service/target/"
        FAILED_SERVICES+=("$service")
        return 1
    fi
}

# Check all services
for service in "${SERVICES[@]}"; do
    check_service_jar "$service" || true
done

echo ""
echo "========================================="
echo "Health Check Summary"
echo "========================================="

if [ ${#FAILED_SERVICES[@]} -eq 0 ]; then
    echo "✓ All services passed health check"
    exit 0
else
    echo "✗ Failed services: ${FAILED_SERVICES[*]}"
    exit 1
fi
