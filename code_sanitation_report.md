# Code Sanitation Report

This report summarizes the findings of a code sanitation and quality review of the `soft40051-cloud` project. The review focused on identifying security vulnerabilities, robustness issues, and areas for code quality improvement.

## Critical Security Vulnerability

*   **Hardcoded Credentials in `cloud-gui`:**
    *   **Finding:** The file `cloud-gui/src/main/java/com/ntu/cloudgui/app/db/MySqlConnectionManager.java` contains a hardcoded database username and password (`USER = "admin"; PASS = "om2YVGGmQAnE";`).
    *   **Impact:** This is a **critical security vulnerability**. Storing credentials in source code makes them accessible to anyone with access to the repository and makes it very difficult to rotate the credentials.
    *   **Recommendation:** These credentials **must** be externalized. The application should be modified to read the username and password from environment variables at runtime. These environment variables should then be set in the `docker-compose.yml` file for the `ntu-vm-soft40051` service.

## Robustness and Code Quality Improvements

### `AggService`

*   **Overall Robustness:** The `AggService` is generally robust. It correctly uses `try-with-resources` for file streams and has a good error handling and rollback mechanism for file uploads.
*   **Potential Improvement:** The `rollbackFileUpload` method is a "best-effort" process. For a production-grade system, failed cleanup operations could be logged to a separate system or a "dead-letter queue" for later attention.

### `cloudlb`

*   **Code Duplication in `LoadBalancerWorker`:**
    *   **Finding:** The `forwardUpload` and `forwardDownload` methods in `LoadBalancerWorker.java` contain duplicated logic for creating and configuring `HttpURLConnection` objects.
    *   **Recommendation:** This logic should be refactored into a single, private helper method to reduce redundancy and improve maintainability.

*   **Inefficient Priority Queue:**
    *   **Finding:** The `RequestQueue.java` class uses a `LinkedList` and iterates through the entire list every time `getHighestPriorityRequest` is called.
    *   **Recommendation:** For better performance, especially with a large number of pending requests, this `LinkedList` should be replaced with a `java.util.PriorityQueue`. A `PriorityQueue` would automatically maintain the requests in priority order, making the retrieval of the highest-priority item much more efficient.

## Conclusion

The most critical issue that must be addressed is the hardcoded database password in the `cloud-gui` service. The other recommendations are for improving the long-term quality and performance of the codebase.
