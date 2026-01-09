# Post-Merge Functionality Verification Guide

## Overview

This document describes the comprehensive testing strategy implemented to verify that all services and components remain functional after pull/merge operations.

## Testing Strategy

### 1. Unit Tests

Unit tests verify individual components and classes in isolation.

#### AggService Tests
- **Location**: `AggService/src/test/java/com/ntu/cloudgui/aggservice/`
- **Tests**:
  - `EncryptionServiceTest` - Tests AES encryption/decryption functionality
  - `CrcValidationServiceTest` - Tests CRC32 checksum validation
  - `FileProcessingServiceTest` - Tests file processing logic (commented out, pending implementation)

#### cloudlb Tests
- **Location**: `cloudlb/src/test/java/com/ntu/cloudgui/cloudlb/`
- **Tests**:
  - `RoundRobinSchedulerTest` - Tests round-robin load balancing algorithm
  - `SjnSchedulerTest` - Tests shortest job next scheduling algorithm
  - `StorageNodeTest` - Tests storage node health management
  - `RequestTest` - Tests request creation, comparison, and priority calculation

#### hostmanager Tests
- **Location**: `hostmanager/src/test/java/com/ntu/cloudgui/hostmanager/`
- **Tests**:
  - `HealthCheckManagerTest` - Tests health check functionality
  - `ScalingLogicTest` - Tests auto-scaling logic
  - `MqttMessageParserTest` - Tests MQTT message parsing

### 2. Service Health Checks

Automated scripts verify that services build correctly and produce valid artifacts.

#### Service Health Check Script
- **Location**: `scripts/ci/service-health-check.sh`
- **Purpose**: Verifies that all service JAR files are built successfully
- **Checks**:
  - JAR file exists
  - JAR file is not empty (size > 1000 bytes)
  - All four services pass validation (AggService, cloudlb, cloud-gui, hostmanager)

#### Component Connectivity Test
- **Location**: `scripts/ci/component-connectivity-test.sh`
- **Purpose**: Verifies that critical classes are present in JAR files
- **Checks**:
  - Main entry point classes exist
  - Core service classes are packaged correctly
  - Dependencies are resolved

### 3. CI/CD Integration

GitHub Actions workflows automatically run all tests on every push and pull request.

#### Java CI Workflow
- **Location**: `.github/workflows/java-ci.yml`
- **Triggers**: Push to main/develop branches, Pull requests to main/develop
- **Steps**:
  1. Checkout code
  2. Set up JDK (20 and 21)
  3. Build with Maven
  4. Run parent project tests
  5. Run service-specific tests (AggService, cloudlb, hostmanager)
  6. Generate test coverage report
  7. Build all services
  8. **Post-Merge Service Health Check** - Verify JAR artifacts
  9. **Post-Merge Component Connectivity Test** - Verify class packaging
  10. Archive build artifacts
  11. Code quality checks (Checkstyle, SpotBugs, OWASP)
  12. Integration tests with Docker Compose

### 4. Docker Health Checks

Docker Compose includes health checks for critical infrastructure services.

#### MySQL (lamp-server)
- **Check**: `mysqladmin ping`
- **Interval**: 10 seconds
- **Timeout**: 5 seconds
- **Retries**: 5
- **Start Period**: 30 seconds

#### MQTT Broker
- **Check**: TCP connection to port 1883
- **Interval**: 10 seconds
- **Timeout**: 5 seconds
- **Retries**: 3
- **Start Period**: 10 seconds

## Running Tests Locally

### Run All Tests
```bash
mvn clean test
```

### Run Service-Specific Tests
```bash
# AggService
cd AggService && mvn test

# cloudlb
cd cloudlb && mvn test

# hostmanager
cd hostmanager && mvn test
```

### Run Health Checks
```bash
# Build all services first
bash scripts/build-all.sh

# Run health check
bash scripts/ci/service-health-check.sh

# Run connectivity test
bash scripts/ci/component-connectivity-test.sh
```

### Run with Docker Compose
```bash
# Start services
docker compose up -d

# Check service health
docker compose ps

# View logs
docker compose logs -f

# Stop services
docker compose down
```

## Test Coverage

Current test coverage by service:

| Service      | Unit Tests | Integration Tests | Health Checks |
|--------------|-----------|-------------------|---------------|
| AggService   | ✅ 4      | Pending          | ✅            |
| cloudlb      | ✅ 20     | Pending          | ✅            |
| cloud-gui    | Pending   | Pending          | ✅            |
| hostmanager  | ✅ 9      | Pending          | ✅            |
| **Total**    | **33**    | **0**            | **4**         |

## Continuous Improvement

### Planned Enhancements
1. Add integration tests for file upload/download flows
2. Add cloud-gui GUI component tests
3. Implement end-to-end smoke tests
4. Add performance benchmarks
5. Implement mutation testing

### Contributing Tests

When adding new features:
1. Write unit tests for new classes/methods
2. Update health check scripts if adding new services
3. Ensure all tests pass before creating pull request
4. Maintain minimum 60% code coverage

## Troubleshooting

### Tests Fail Locally
1. Ensure Java 20+ is installed: `java --version`
2. Clean build artifacts: `mvn clean`
3. Update dependencies: `mvn dependency:resolve`
4. Check for port conflicts (3306, 1883, etc.)

### CI/CD Pipeline Fails
1. Check workflow logs in GitHub Actions
2. Verify all required secrets are configured
3. Ensure .gitignore doesn't exclude test files
4. Validate docker-compose.yml syntax

### Health Checks Fail
1. Verify all services built successfully: `ls -lh */target/*.jar`
2. Check JAR file sizes (should be > 1KB)
3. Verify Java version matches build requirements
4. Review build logs for compilation errors

## References

- [Testing Guide](../docs/testing/TESTING.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Docker Compose Health Checks](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
