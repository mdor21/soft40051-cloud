# Changelog

All notable changes to the SOFT40051 Cloud Storage System project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Professional project documentation (CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md)
- MIT License
- EditorConfig for consistent code formatting
- GitHub Actions CI/CD workflows
- Pull request and issue templates
- Architecture documentation
- API documentation structure
- Comprehensive .gitattributes file
- Docker organization improvements

### Changed
- Improved README.md with badges and better structure
- Enhanced .gitignore with more patterns
- Reorganized documentation into logical subdirectories

### Fixed
- Repository structure aligned with professional standards

## [1.0.0] - 2024-01-09

### Added
- Initial release of SOFT40051 Cloud Storage System
- Distributed file storage with encryption and chunking
- Load balancer with FCFS, SJN, and Round Robin scheduling
- Aggregator service for file processing
- JavaFX GUI for user interaction
- MySQL database for metadata storage
- MQTT broker for system monitoring and scaling
- Four storage nodes with SSH access
- Jenkins integration for CI/CD
- Gitea for version control
- Docker Compose orchestration

### Security
- AES encryption for file chunks
- CRC32 checksum validation
- File permission system
- Audit logging
- SSH secure communication between nodes

## Version History

### Core Components

#### cloud-gui (JavaFX Application)
- User interface for file upload/download
- RDP access on port 3390
- SSH access on port 2022

#### cloudlb (Load Balancer)
- Multi-threaded request processing
- Three scheduling algorithms: FCFS, SJN, Round Robin
- Health checking of backend nodes
- MQTT integration for scaling
- Simulated network latency (1-5s)
- Starvation prevention with aging mechanism
- Semaphore-based concurrency control

#### AggService (Aggregator)
- File encryption and chunking
- CRC32 checksum generation
- Distribution to storage nodes
- File reconstruction on download
- Database integration for metadata

#### hostmanager (Host Management)
- Storage node health monitoring
- SSH connectivity verification
- Node registration and discovery

#### Storage Nodes (1-4)
- Encrypted chunk storage
- SSH access (ports 4848-4851)
- Individual volume management

#### MySQL Database
- User management
- File metadata storage
- Chunk location tracking
- System audit logs
- Performance metrics

#### MQTT Broker
- Real-time monitoring messages
- Scaling event triggers
- System health notifications

#### Jenkins
- Automated builds
- Testing pipeline
- Deployment automation

#### Gitea
- Internal version control
- Code repository hosting
- Collaboration features

---

## How to Update This Changelog

When making changes:

1. Add entries under `[Unreleased]` section
2. Use subsections: Added, Changed, Deprecated, Removed, Fixed, Security
3. Keep entries concise but descriptive
4. Link to relevant issue/PR numbers
5. Update version numbers following semantic versioning

Example:
```markdown
### Added
- New authentication mechanism (#123)
- Support for additional file types (#456)

### Fixed
- Memory leak in load balancer (#789)
- Incorrect checksum validation (#012)
```

[Unreleased]: https://github.com/mdor21/soft40051-cloud/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/mdor21/soft40051-cloud/releases/tag/v1.0.0
