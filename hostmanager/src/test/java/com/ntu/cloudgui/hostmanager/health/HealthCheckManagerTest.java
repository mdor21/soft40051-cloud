package com.ntu.cloudgui.hostmanager.health;

import com.ntu.cloudgui.hostmanager.container.ContainerInfo;
import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import com.ntu.cloudgui.hostmanager.scaling.ScalingEventPublisher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckManagerTest {

    @Mock
    private ContainerManager containerManager;

    @Mock
    private DockerCommandExecutor dockerExecutor;

    @Mock
    private ScalingEventPublisher eventPublisher;

    @InjectMocks
    private HealthCheckManager healthCheckManager;

    @Test
    public void testPerformHealthCheckWithHealthyContainer() {
        ContainerInfo containerInfo = new ContainerInfo("test-container");
        when(containerManager.getAllContainers()).thenReturn(Collections.singletonList(containerInfo));
        when(dockerExecutor.inspectContainer("test-container"))
                .thenReturn(new ProcessResult(0, "\"Running\": true", ""));

        healthCheckManager.performHealthCheck();

        verify(containerManager).updateHealthStatus("test-container", true);
    }

    @Test
    public void testPerformHealthCheckWithUnhealthyContainer() {
        ContainerInfo containerInfo = new ContainerInfo("test-container");
        when(containerManager.getAllContainers()).thenReturn(Collections.singletonList(containerInfo));
        when(dockerExecutor.inspectContainer("test-container"))
                .thenReturn(new ProcessResult(1, "", "Error"));

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                containerInfo.setHealthy((Boolean) args[1]);
                return null;
            }
        }).when(containerManager).updateHealthStatus(anyString(), anyBoolean());


        healthCheckManager.performHealthCheck();

        assertEquals(false, containerInfo.isHealthy());
    }

    @Test
    public void testGetHealthStatus() {
        ContainerInfo container = new ContainerInfo("test-container");
        when(containerManager.getAllContainers()).thenReturn(Collections.singletonList(container));

        String healthStatus = healthCheckManager.getHealthStatus();

        assertEquals("1/1 healthy", healthStatus);
    }
}
