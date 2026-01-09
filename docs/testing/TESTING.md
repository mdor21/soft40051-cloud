# Testing Guide

This guide covers testing strategies, frameworks, and best practices for the SOFT40051 Cloud Storage System.

## Table of Contents

1. [Testing Strategy](#testing-strategy)
2. [Test Types](#test-types)
3. [Running Tests](#running-tests)
4. [Writing Tests](#writing-tests)
5. [Test Coverage](#test-coverage)
6. [Continuous Integration](#continuous-integration)

## Testing Strategy

### Testing Pyramid

Our testing strategy follows the testing pyramid approach:

```
        ╱────────────╲
       ╱   E2E Tests  ╲      ← Few, high-level
      ╱────────────────╲
     ╱  Integration     ╲    ← Some, medium-level
    ╱      Tests         ╲
   ╱──────────────────────╲
  ╱     Unit Tests         ╲ ← Many, low-level
 ╱──────────────────────────╲
```

### Testing Principles

1. **Fast Feedback**: Unit tests run quickly
2. **Isolation**: Tests don't depend on each other
3. **Repeatability**: Same results every time
4. **Comprehensive**: Cover happy paths and edge cases
5. **Maintainable**: Easy to understand and update

## Test Types

### 1. Unit Tests

**Purpose**: Test individual components in isolation

**Location**: `src/test/java/` in each module

**Framework**: JUnit 5 + Mockito

**Coverage Areas**:
- Individual class methods
- Business logic
- Data transformations
- Validation logic

**Example**:
```java
@Test
void calculatePriorityScore_withAgingFactor_returnsCorrectScore() {
    // Arrange
    Request request = new Request("file.txt", 1024, System.currentTimeMillis());
    
    // Act
    double score = request.calculatePriorityScore();
    
    // Assert
    assertTrue(score > 0);
}
```

### 2. Integration Tests

**Purpose**: Test interactions between components

**Location**: `src/test/java/` with `IT` suffix

**Framework**: JUnit 5 + Testcontainers

**Coverage Areas**:
- Database operations
- Network communication
- File I/O
- Inter-service communication

**Example**:
```java
@Test
void uploadFile_toDatabase_successfullySavesMetadata() {
    // Arrange
    FileMetadata metadata = new FileMetadata("test.txt", 1024);
    
    // Act
    repository.save(metadata);
    
    // Assert
    Optional<FileMetadata> result = repository.findById(metadata.getId());
    assertTrue(result.isPresent());
}
```

### 3. End-to-End Tests

**Purpose**: Test complete workflows

**Location**: Separate test project or scripts

**Framework**: Selenium/TestFX for GUI, REST Assured for APIs

**Coverage Areas**:
- Complete file upload workflow
- Complete file download workflow
- User authentication flow
- Error handling scenarios

## Running Tests

### Run All Tests

```bash
# Run all unit tests
mvn test

# Run all tests including integration tests
mvn verify

# Run tests with coverage report
mvn clean test jacoco:report
```

### Run Tests for Specific Module

```bash
# AggService tests
cd AggService
mvn test

# Load Balancer tests
cd cloudlb
mvn test

# Cloud GUI tests
cd cloud-gui
mvn test

# Host Manager tests
cd hostmanager
mvn test
```

### Run Specific Test Class

```bash
# Run single test class
mvn test -Dtest=FileProcessingServiceTest

# Run specific test method
mvn test -Dtest=FileProcessingServiceTest#testFileEncryption
```

### Run Tests with Different Profiles

```bash
# Run with integration test profile
mvn verify -Pintegration-tests

# Skip tests
mvn install -DskipTests

# Run only unit tests, skip integration tests
mvn test -DskipITs
```

### Run Tests in Docker Environment

```bash
# Start test environment
docker compose -f docker-compose.test.yml up -d

# Run tests
mvn verify

# Stop test environment
docker compose -f docker-compose.test.yml down -v
```

## Writing Tests

### Unit Test Structure

Follow the AAA (Arrange-Act-Assert) pattern:

```java
@Test
void methodName_condition_expectedResult() {
    // Arrange - Set up test data and dependencies
    FileProcessor processor = new FileProcessor();
    File testFile = new File("test.txt");
    
    // Act - Execute the method under test
    Result result = processor.process(testFile);
    
    // Assert - Verify the outcome
    assertNotNull(result);
    assertEquals(expectedValue, result.getValue());
}
```

### Naming Conventions

- **Test Class**: `[ClassUnderTest]Test.java`
- **Integration Test**: `[ClassUnderTest]IT.java`
- **Test Method**: `methodName_scenario_expectedBehavior()`

### Using Mockito

```java
@ExtendWith(MockitoExtension.class)
class LoadBalancerServiceTest {
    
    @Mock
    private NodeRegistry nodeRegistry;
    
    @Mock
    private Scheduler scheduler;
    
    @InjectMocks
    private LoadBalancerService loadBalancerService;
    
    @Test
    void selectNode_whenHealthyNodesExist_returnsNode() {
        // Arrange
        Node mockNode = new Node("node1", "localhost", 9000);
        when(nodeRegistry.getHealthyNodes()).thenReturn(List.of(mockNode));
        
        // Act
        Optional<Node> result = loadBalancerService.selectNode();
        
        // Assert
        assertTrue(result.isPresent());
        verify(nodeRegistry, times(1)).getHealthyNodes();
    }
}
```

### Testing Exceptions

```java
@Test
void processFile_withInvalidFile_throwsException() {
    // Arrange
    FileProcessor processor = new FileProcessor();
    File invalidFile = null;
    
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> {
        processor.process(invalidFile);
    });
}
```

### Testing Asynchronous Code

```java
@Test
void asyncOperation_completesSuccessfully() throws Exception {
    // Arrange
    AsyncService service = new AsyncService();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Result> resultRef = new AtomicReference<>();
    
    // Act
    service.performAsync(result -> {
        resultRef.set(result);
        latch.countDown();
    });
    
    // Assert
    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertNotNull(resultRef.get());
}
```

### Parameterized Tests

```java
@ParameterizedTest
@ValueSource(ints = {1, 10, 100, 1000})
void calculateChecksum_withDifferentSizes_returnsValidChecksum(int size) {
    // Arrange
    byte[] data = new byte[size];
    new Random().nextBytes(data);
    
    // Act
    long checksum = CrcValidator.calculate(data);
    
    // Assert
    assertTrue(checksum > 0);
}
```

## Test Coverage

### Coverage Goals

- **Unit Test Coverage**: Minimum 80%
- **Integration Test Coverage**: Minimum 60%
- **Critical Path Coverage**: 100%

### Generating Coverage Reports

```bash
# Generate JaCoCo coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Report Structure

```
target/site/jacoco/
├── index.html           # Main report
├── com.ntu.cloudgui/    # Package coverage
│   ├── index.html
│   └── [Classes].html
└── jacoco.xml           # XML for CI tools
```

### Viewing Coverage in IDE

**IntelliJ IDEA**:
1. Right-click on test class
2. Select "Run with Coverage"
3. View coverage in editor gutter

**Eclipse**:
1. Install EclEmma plugin
2. Right-click on project
3. Select "Coverage As" -> "JUnit Test"

## Module-Specific Testing

### AggService Tests

**Key Test Areas**:
- File encryption/decryption
- Chunking algorithms
- CRC32 checksum validation
- Storage node communication
- Database operations

**Sample Test**:
```java
@Test
void encryptFile_withValidInput_returnsEncryptedData() {
    EncryptionService service = new EncryptionService();
    byte[] original = "test data".getBytes();
    
    byte[] encrypted = service.encrypt(original);
    byte[] decrypted = service.decrypt(encrypted);
    
    assertArrayEquals(original, decrypted);
}
```

### Load Balancer Tests

**Key Test Areas**:
- Scheduling algorithms (FCFS, SJN, Round Robin)
- Request queue management
- Health checking
- Concurrency control
- Starvation prevention

**Sample Test**:
```java
@Test
void roundRobinScheduler_distributesEvenly() {
    List<Node> nodes = createTestNodes(3);
    RoundRobinScheduler scheduler = new RoundRobinScheduler(nodes);
    
    Map<String, Integer> distribution = new HashMap<>();
    for (int i = 0; i < 30; i++) {
        Node selected = scheduler.selectNode();
        distribution.merge(selected.getId(), 1, Integer::sum);
    }
    
    // Each node should get 10 requests
    distribution.values().forEach(count -> assertEquals(10, count));
}
```

### Cloud GUI Tests

**Key Test Areas**:
- UI component rendering
- User interactions
- File selection
- Progress tracking
- Error dialogs

**Sample Test** (using TestFX):
```java
@Test
void uploadButton_whenClicked_startsUpload(FxRobot robot) {
    // Arrange
    robot.clickOn("#fileChooserButton");
    // Select file in dialog...
    
    // Act
    robot.clickOn("#uploadButton");
    
    // Assert
    robot.lookup("#progressBar").query();
}
```

## Continuous Integration

### GitHub Actions

Tests run automatically on:
- Push to main/develop branches
- Pull request creation
- Tag creation

See `.github/workflows/java-ci.yml` for configuration.

### Local CI Testing

```bash
# Simulate CI environment locally
docker run --rm -v $(pwd):/project -w /project maven:3.9-openjdk-20 \
  mvn clean verify
```

## Test Data Management

### Test Fixtures

Store test data in:
```
src/test/resources/
├── test-files/
│   ├── small.txt
│   ├── medium.txt
│   └── large.txt
├── test-data.sql
└── test-config.properties
```

### Test Databases

Use Testcontainers for integration tests:

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("test_db")
    .withUsername("test")
    .withPassword("test");
```

## Best Practices

1. **Keep Tests Fast**: Unit tests < 100ms, Integration tests < 10s
2. **Test One Thing**: Each test should verify one behavior
3. **Use Descriptive Names**: Test names should explain what they test
4. **Avoid Test Interdependence**: Tests should run in any order
5. **Clean Up Resources**: Use `@AfterEach` or try-with-resources
6. **Mock External Dependencies**: Don't depend on external services
7. **Test Edge Cases**: Null, empty, very large, very small values
8. **Document Complex Tests**: Add comments for non-obvious test logic

## Troubleshooting

### Tests Fail Intermittently

- Check for timing issues
- Ensure proper test isolation
- Verify resource cleanup

### Tests Pass Locally but Fail in CI

- Check environment differences
- Verify Docker image versions
- Review CI logs carefully

### Slow Test Execution

- Profile test execution time
- Parallelize independent tests
- Use test slicing for CI

## Additional Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TestContainers](https://www.testcontainers.org/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
