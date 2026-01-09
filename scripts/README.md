# Utility Scripts

This directory contains utility scripts for building, testing, and managing the SOFT40051 Cloud Storage System.

## Available Scripts

### build-all.sh

Builds all services in the correct order.

**Usage:**
```bash
./scripts/build-all.sh [options]
```

**Options:**
- `--skip-tests`: Skip running tests during build
- `--clean`: Perform a clean build (removes previous artifacts)

**Examples:**
```bash
# Normal build with tests
./scripts/build-all.sh

# Clean build without tests (faster)
./scripts/build-all.sh --clean --skip-tests

# Just skip tests
./scripts/build-all.sh --skip-tests
```

### clean-all.sh

Cleans build artifacts, IDE files, and optionally Docker resources.

**Usage:**
```bash
./scripts/clean-all.sh [options]
```

**Options:**
- `--deep`: Also clean Maven cache and IDE files
- `--docker`: Also clean Docker containers and volumes
- `--all`: Perform both deep and Docker clean

**Examples:**
```bash
# Basic clean (Maven artifacts only)
./scripts/clean-all.sh

# Deep clean (includes IDE files, OS files)
./scripts/clean-all.sh --deep

# Clean Docker resources
./scripts/clean-all.sh --docker

# Clean everything
./scripts/clean-all.sh --all
```

**Warning:** The `--docker` option will stop all containers and remove volumes, which means you'll lose any data!

### run-tests.sh

Runs all tests with coverage reports.

**Usage:**
```bash
./scripts/run-tests.sh
```

This script:
1. Runs tests for all services
2. Generates JaCoCo coverage reports
3. Displays location of coverage reports

**Coverage Reports:**
After running, open the HTML reports in your browser:
- AggService: `AggService/target/site/jacoco/index.html`
- Load Balancer: `cloudlb/target/site/jacoco/index.html`
- Cloud GUI: `cloud-gui/target/site/jacoco/index.html`
- Host Manager: `hostmanager/target/site/jacoco/index.html`

## Making Scripts Executable

If scripts are not executable, run:

```bash
chmod +x scripts/*.sh
```

## Script Requirements

All scripts require:
- Bash shell (Linux/macOS/WSL on Windows)
- Maven 3.8+
- Java 20+
- Docker & Docker Compose (for clean-all.sh --docker)

## Adding New Scripts

When adding new scripts:

1. Create the script in this directory
2. Make it executable: `chmod +x scripts/your-script.sh`
3. Add documentation to this README
4. Follow the existing script structure (colors, error handling, etc.)
5. Include usage examples

## Script Template

```bash
#!/bin/bash
# Description of what the script does

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Helper functions
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
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

# Your script logic here
print_status "Doing something..."
# ... your code ...
print_success "Done!"
```

## Troubleshooting

### Permission Denied

If you get "Permission denied" errors:
```bash
chmod +x scripts/script-name.sh
```

### Script Not Found

Make sure you're in the project root directory:
```bash
cd /path/to/soft40051-cloud
./scripts/script-name.sh
```

### Maven Not Found

Ensure Maven is installed and in your PATH:
```bash
mvn --version
```

### Docker Errors

Ensure Docker daemon is running:
```bash
docker ps
```

## Contributing

When modifying scripts:
1. Test thoroughly before committing
2. Update this README if you add new scripts or options
3. Ensure scripts work on Linux, macOS, and WSL
4. Add error handling for common failure cases
5. Use consistent formatting and naming conventions
