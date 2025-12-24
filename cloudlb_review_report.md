# CloudLB Code Review Report

This report summarizes the findings of a focused code review of the `cloudlb` service. The review verified the implementation against the detailed requirements provided by the user.

## Summary of Findings

The `cloudlb` service is partially implemented and contains significant gaps that prevent the elastic scaling and Shortest-Job-Next features from working correctly. While the core request handling and basic scheduling algorithms are in place, the service is not fully integrated.

### Gaps

*   **Elastic Scaling Not Implemented:** The `ScalingService`, which is responsible for monitoring the queue and publishing scaling requests to the MQTT broker, is never initialized or scheduled to run in `MainLb.java`. As a result, the load balancer will never trigger the `hostmanager` to scale the number of file server containers.
*   **Incorrect MQTT Payload Format:** The `ScalingService` sends a plain-text payload (e.g., `"SET,4"`) to the MQTT broker. This does not match the required JSON format (`{"action": "up", "count": 4}`) that the `hostmanager` expects, which will cause parsing errors.
*   **SJN Scheduler Not Implemented:** The `SjnScheduler` is currently implemented as a simple round-robin scheduler and does not take the job size into account. This fails to meet the "Shortest-Job-Next" requirement.

### Potential Improvements

*   **FCFS Scheduler Implementation:** The `FcfsScheduler` is implemented to always select the first available node. A more robust implementation would be to select the node that has been waiting the longest (i.e., the "least recently used").
*   **Test Coverage:** The project contains a `junit` dependency, but no actual tests were found. Adding unit tests for the schedulers, the request queue, and the scaling service would significantly improve the quality and reliability of the codebase.

## Detailed Findings

*   **Core Logic (`MainLb.java`):** The main application class correctly initializes the schedulers and the request worker, but fails to initialize the `ScalingService`.
*   **Scheduling Algorithms:** The `RoundRobinScheduler` is correctly implemented. The `FcfsScheduler` is functional but could be improved. The `SjnScheduler` is not correctly implemented.
*   **Latency Simulation:** The `LoadBalancerWorker` correctly implements the 1.0–5.0s latency simulation.
*   **Starvation Prevention:** The `RequestQueue` correctly implements an aging mechanism to prevent request starvation.
*   **Elastic Scaling:** The `ScalingService` contains the logic to publish scaling requests, but it is not used and the payload format is incorrect.
