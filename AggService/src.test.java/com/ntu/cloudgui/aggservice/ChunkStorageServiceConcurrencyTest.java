package com.ntu.cloudgui.aggservice;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.Mockito;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TaskResult {
    long startTime;
    long endTime;
    String host;

    TaskResult(long startTime, long endTime, String host) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.host = host;
    }
}

public class ChunkStorageServiceConcurrencyTest {

    private ChunkStorageService chunkStorageService;
    private Configuration mockConfig;

    @BeforeEach
    public void setUp() {
        mockConfig = Mockito.mock(Configuration.class);
        List<String> hosts = Arrays.asList("host1", "host2");
        when(mockConfig.getFileServerHosts()).thenReturn(hosts);
        chunkStorageService = Mockito.spy(new ChunkStorageService(mockConfig));
    }

    @RepeatedTest(5) // Run multiple times to increase chance of catching race conditions
    public void testConcurrentAccess_EnsuresPerNodeSerializationAndParallelism() throws InterruptedException, JSchException, SftpException {
        // Mock the actual SFTP upload to avoid network calls and introduce a predictable delay
        doAnswer(invocation -> {
            Thread.sleep(200); // Simulate network latency
            return null;
        }).when(chunkStorageService).upload(anyString(), anyString(), anyString(), anyInt(), any(InputStream.class), anyString());

        int numTasks = 6;
        ExecutorService executor = Executors.newFixedThreadPool(numTasks);
        List<Future<TaskResult>> futures = new ArrayList<>();
        long fileId = 123;
        byte[] chunkData = "test data".getBytes();

        // Submit multiple tasks to the executor
        for (int i = 0; i < numTasks; i++) {
            int chunkIndex = i;
            futures.add(executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                String host = chunkStorageService.storeChunk(chunkData, fileId, chunkIndex);
                long endTime = System.currentTimeMillis();
                return new TaskResult(startTime, endTime, host);
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        // Collect results
        List<TaskResult> results = new ArrayList<>();
        for (Future<TaskResult> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // Group results by host
        Map<String, List<TaskResult>> resultsByHost = results.stream()
                .collect(Collectors.groupingBy(r -> r.host));

        // --- Verify Serialization for each host ---
        for (Map.Entry<String, List<TaskResult>> entry : resultsByHost.entrySet()) {
            List<TaskResult> hostResults = entry.getValue();
            hostResults.sort((r1, r2) -> Long.compare(r1.startTime, r2.startTime));
            for (int i = 0; i < hostResults.size() - 1; i++) {
                TaskResult current = hostResults.get(i);
                TaskResult next = hostResults.get(i + 1);
                assertTrue(next.startTime >= current.endTime,
                        "Tasks on the same host (" + entry.getKey() + ") should be serialized. Task " + (i+1) + " started before task " + i + " finished.");
            }
        }

        // --- Verify Parallelism between different hosts ---
        List<TaskResult> host1Results = resultsByHost.get("host1");
        List<TaskResult> host2Results = resultsByHost.get("host2");

        if (host1Results != null && !host1Results.isEmpty() && host2Results != null && !host2Results.isEmpty()) {
            TaskResult taskOnHost1 = host1Results.get(0);
            TaskResult taskOnHost2 = host2Results.get(0);
            // Check for overlap in execution windows
            boolean overlap = taskOnHost1.startTime < taskOnHost2.endTime && taskOnHost2.startTime < taskOnHost1.endTime;
            assertTrue(overlap, "Tasks on different hosts should run in parallel (execution windows should overlap).");
        }
    }
}
