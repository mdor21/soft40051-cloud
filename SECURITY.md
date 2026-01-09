# Security Policy

## Supported Versions

We take security seriously in the SOFT40051 Cloud Storage System. Currently, the following versions are receiving security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please follow these steps:

### 1. **DO NOT** Open a Public Issue

Security vulnerabilities should be reported privately to prevent exploitation before a fix is available.

### 2. Report via Email

Send a detailed report to the project maintainers. Include:

- Description of the vulnerability
- Steps to reproduce the issue
- Potential impact assessment
- Suggested fix (if any)
- Your contact information

### 3. Wait for Acknowledgment

You should receive an acknowledgment within 48 hours. We will:

- Confirm receipt of your report
- Assess the vulnerability
- Provide an estimated timeline for a fix
- Keep you informed of our progress

## Security Best Practices

### For Users

1. **Environment Variables**
   - Never commit `.env` files to version control
   - Use strong, unique passwords for all services
   - Rotate credentials regularly

2. **Docker Security**
   - Keep Docker and Docker Compose up to date
   - Review container permissions
   - Use Docker secrets for sensitive data in production
   - Scan images for vulnerabilities regularly

3. **Network Security**
   - Use firewall rules to restrict access
   - Enable SSL/TLS for production deployments
   - Isolate sensitive services in private networks
   - Implement proper authentication and authorization

4. **Database Security**
   - Use strong MySQL passwords
   - Limit database user privileges
   - Enable audit logging
   - Regularly backup encrypted data

### For Developers

1. **Code Security**
   - Never hardcode credentials or secrets
   - Validate and sanitize all user inputs
   - Use parameterized queries to prevent SQL injection
   - Implement proper error handling (avoid exposing stack traces)

2. **Dependency Management**
   - Keep dependencies up to date
   - Review security advisories for used libraries
   - Use Maven Dependency Check plugin
   - Audit third-party dependencies regularly

3. **Encryption**
   - Use industry-standard encryption algorithms
   - Never implement custom cryptography
   - Protect encryption keys properly
   - Implement secure key rotation

4. **Testing**
   - Include security test cases
   - Test authentication and authorization
   - Perform input validation testing
   - Test for common vulnerabilities (OWASP Top 10)

## Known Security Considerations

This is an academic project simulating a distributed file storage system. The following security features are implemented:

### Implemented Security Features

- **File Encryption**: AES encryption for file chunks
- **Checksum Validation**: CRC32 for data integrity
- **Access Control**: File permission system
- **Audit Logging**: System activity tracking
- **Secure Communication**: SSH for inter-node communication

### Areas Requiring Additional Security (Production Use)

If deploying in a production environment, consider:

- Implementing HTTPS/TLS for all web interfaces
- Adding rate limiting and DDoS protection
- Implementing multi-factor authentication
- Using hardware security modules (HSM) for key storage
- Implementing intrusion detection systems
- Regular security audits and penetration testing
- GDPR compliance measures for data handling

## Vulnerability Response Process

1. **Assessment** (1-2 days)
   - Validate the vulnerability
   - Determine severity level
   - Identify affected versions

2. **Development** (1-7 days depending on severity)
   - Create fix in private repository
   - Write security tests
   - Test fix thoroughly

3. **Release** (ASAP for critical issues)
   - Prepare security advisory
   - Release patched version
   - Notify users via GitHub Security Advisories

4. **Disclosure** (After fix is available)
   - Publish security advisory
   - Credit reporter (if desired)
   - Update security documentation

## Security Checklist for Contributors

Before submitting code, ensure:

- [ ] No hardcoded credentials or secrets
- [ ] Input validation implemented
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (proper output encoding)
- [ ] CSRF protection (where applicable)
- [ ] Proper error handling (no information leakage)
- [ ] Dependencies are up to date
- [ ] Security-focused unit tests included
- [ ] Documentation updated for security-relevant changes

## Compliance

This project is designed for educational purposes as part of SOFT40051 coursework. For production deployments, ensure compliance with:

- GDPR (General Data Protection Regulation)
- HIPAA (if handling health data)
- PCI DSS (if handling payment data)
- ISO 27001 (Information Security Management)
- Local data protection laws

## Additional Resources

- [OWASP Top Ten](https://owasp.org/www-project-top-ten/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [Java Security Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)

## Contact

For security-related questions or concerns, please contact the project maintainers.

---

**Remember**: Security is everyone's responsibility. When in doubt, ask for guidance.
