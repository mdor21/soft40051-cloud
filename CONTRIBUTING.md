# Contributing to SOFT40051 Cloud Storage System

Thank you for your interest in contributing to the SOFT40051 Cloud-Based Distributed File Storage System! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 20 or higher
- Apache Maven 3.8+
- Docker Engine 20.10+
- Docker Compose 2.0+
- Git 2.30+
- NetBeans IDE (recommended) or IntelliJ IDEA

### Setting Up Development Environment

1. **Fork and clone the repository:**
   ```bash
   git clone https://github.com/your-username/soft40051-cloud.git
   cd soft40051-cloud
   ```

2. **Set up environment variables:**
   ```bash
   cp .env.example .env
   # Edit .env with your local configuration
   ```

3. **Build the project:**
   ```bash
   mvn clean install
   ```

4. **Start the development environment:**
   ```bash
   docker compose up -d
   ```

## Development Workflow

1. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following our coding standards

3. **Run tests:**
   ```bash
   mvn test
   ```

4. **Build and verify:**
   ```bash
   mvn clean package
   ```

5. **Commit your changes** with meaningful commit messages

6. **Push to your fork and create a pull request**

## Coding Standards

### Java Code Style

- **Formatting:** Follow Google Java Style Guide
- **Indentation:** Use 4 spaces (no tabs)
- **Line Length:** Maximum 120 characters
- **Naming Conventions:**
  - Classes: PascalCase (e.g., `LoadBalancerService`)
  - Methods: camelCase (e.g., `processRequest`)
  - Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
  - Variables: camelCase (e.g., `fileName`)

### Documentation

- **JavaDoc:** All public classes and methods must have JavaDoc comments
- **Inline Comments:** Use sparingly, only for complex logic
- **TODO Comments:** Include your name and date: `// TODO(username, 2024-01-09): Description`

### Code Organization

- Keep methods small and focused (< 50 lines preferred)
- One class per file
- Group related functionality in packages
- Minimize dependencies between modules

## Testing Guidelines

### Unit Tests

- Write unit tests for all new functionality
- Aim for 80% code coverage minimum
- Use JUnit 5 for test framework
- Use Mockito for mocking dependencies

### Test Naming Convention

```java
@Test
void methodName_scenarioUnderTest_expectedBehavior() {
    // Test implementation
}
```

### Integration Tests

- Place integration tests in separate test classes (suffix: `IT`)
- Use test containers for database and MQTT broker testing
- Ensure tests can run in isolation

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=LoadBalancerServiceTest

# Run with coverage report
mvn clean test jacoco:report
```

## Commit Message Guidelines

Follow the Conventional Commits specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat:** New feature
- **fix:** Bug fix
- **docs:** Documentation changes
- **style:** Code style changes (formatting, no logic change)
- **refactor:** Code refactoring
- **test:** Adding or updating tests
- **chore:** Maintenance tasks

### Examples

```
feat(cloudlb): add round-robin load balancing algorithm

Implemented a round-robin scheduler for distributing requests
evenly across storage nodes.

Closes #123
```

```
fix(aggregator): prevent race condition in file upload

Added semaphore-based locking to prevent concurrent writes
to the same file.

Fixes #456
```

## Pull Request Process

1. **Update documentation** if you're changing functionality

2. **Ensure all tests pass:**
   ```bash
   mvn clean verify
   ```

3. **Update CHANGELOG.md** with your changes

4. **Create Pull Request** with:
   - Clear title describing the change
   - Detailed description of what and why
   - Link to related issues
   - Screenshots for UI changes

5. **Code Review:**
   - Address reviewer feedback promptly
   - Keep discussions professional and constructive
   - Be open to suggestions

6. **Merge Requirements:**
   - All CI checks must pass
   - At least one approval from maintainer
   - No unresolved conversations
   - Branch up to date with main

## Project-Specific Guidelines

### Service Components

When working on specific services:

- **cloud-gui:** JavaFX UI components, follow Scene Builder conventions
- **cloudlb:** Load balancer, ensure thread safety
- **AggService:** Aggregator service, validate encryption/chunking
- **hostmanager:** Host management, verify SSH connectivity

### Database Changes

- Create migration scripts in `db/migrations/`
- Update `SchemaManager.java` accordingly
- Document schema changes in `docs/database/`

### Docker Changes

- Test locally with `docker compose build`
- Verify multi-architecture support if applicable
- Update docker-compose.yml documentation

### Security Considerations

- Never commit sensitive data (passwords, keys)
- Use environment variables for configuration
- Follow OWASP security guidelines
- Report security issues privately (see SECURITY.md)

## Getting Help

- **Documentation:** Check the [docs/](docs/) directory
- **Issues:** Search existing issues or create a new one
- **Discussions:** Use GitHub Discussions for questions
- **Contact:** Reach out to maintainers via project channels

## Recognition

Contributors will be recognized in:
- Project README.md
- CHANGELOG.md
- GitHub contributors page

Thank you for contributing to SOFT40051 Cloud Storage System! 🚀
