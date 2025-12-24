# Final Sanitation and Compliance Report

This report provides a definitive list of all the discrepancies found between the current project and the detailed coursework requirements. The findings are based on a meticulous review of the `docker-compose.yml` file and the application code.

The project is **not compliant** with the coursework requirements in its current state. The following changes are mandatory to achieve full compliance.

## Summary of Required Changes

The most significant changes are required in the `docker-compose.yml` file, which is missing key services, uses incorrect images, and lacks the mandatory volumes for data persistence.

### `docker-compose.yml` Discrepancies

1.  **`ntu-vm-soft40051` (GUI) Service:**
    *   **Incorrect Image:** The service is configured to `build` a local image. It **must** be changed to use the pre-built image: `image: pedrombmachado/ntu_lubuntu:soft40051`.
    *   **Missing Volume:** The service **must** be configured to use the `docker_soft40051` volume.

2.  **`lamp-server` (Database) Service:**
    *   **Incorrect Image:** The service uses `mysql:5.7`. It **must** be changed to `image: mattrayner/lamp:latest-1804`.
    *   **Missing Port Mapping:** The service is missing the required port mapping for the web interface. It **must** have `8080:80` added to its ports.
    *   **Missing Volume:** The service **must** be configured to use the `lamp` volume.

3.  **`mqtt-broker` Service:**
    *   **Incorrect Image:** The service uses `eclipse-mosquitto`. It **must** be changed to `image: pedrombmachado/mqtt:base`.

4.  **`lb` (Load Balancer) Service:**
    *   **Inconsistent Naming:** For clarity and consistency, the `container_name` should be changed from `load-balancer` to `lb`.

5.  **Missing Services:**
    *   The `jenkins-soft40051` service is **completely missing** and must be added.
    *   The `gitea` service is **completely missing** and must be added.

6.  **Missing Volumes:**
    *   The top-level `volumes` definition is **completely missing**. All required volumes (`docker_soft40051`, `lamp`, `jenkins_soft40051`, `gitea_data`, `gitea_config`) must be defined here.

### Other Issues

*   **Hardcoded Credentials:** As noted in the previous sanitation report, the `cloud-gui`'s `MySqlConnectionManager.java` still contains hardcoded credentials. While environment variables have been added to the `docker-compose.yml`, the code should be updated to prioritize these, and the hardcoded values should be removed.

## Conclusion

The project requires a significant overhaul of the `docker-compose.yml` file to meet the coursework specifications. Once these changes are made, the system should be fully compliant and runnable in the intended containerized environment.
