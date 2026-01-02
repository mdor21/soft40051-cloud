package com.ntu.cloudgui.hostmanager.scaling;

import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScalingLogicTest {

    @Mock
    private ContainerManager containerManager;

    @Mock
    private DockerCommandExecutor dockerExecutor;

    @Mock
    private ScalingEventPublisher eventPublisher;

    @InjectMocks
    private ScalingLogic scalingLogic;

    @Before
    public void setUp() {
        scalingLogic.setEventPublisher(eventPublisher);
    }

    @Test
    public void testHandleScaleUp() {
        when(dockerExecutor.containerExists(anyString())).thenReturn(false);
        when(dockerExecutor.runContainer(anyString(), anyInt(), anyString()))
                .thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleUp(2);

        verify(dockerExecutor, times(2)).runContainer(anyString(), anyInt(), anyString());
        verify(containerManager, times(2)).addContainer(anyString());
        verify(eventPublisher, times(2)).publishScalingEvent(anyString(), anyString());
    }

    @Test
    public void testHandleScaleUpWithExistingContainer() {
        when(dockerExecutor.containerExists("soft40051-files-container1")).thenReturn(true);
        when(dockerExecutor.containerExists("soft40051-files-container2")).thenReturn(false);
        when(dockerExecutor.runContainer("soft40051-files-container2", 4849, "pedrombmachado/simple-ssh-container:base"))
                .thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleUp(2);

        verify(dockerExecutor, times(1)).runContainer(anyString(), anyInt(), anyString());
        verify(containerManager, times(1)).addContainer(anyString());
        verify(eventPublisher, times(1)).publishScalingEvent(anyString(), anyString());
    }

    @Test
    public void testHandleScaleDown() {
        when(dockerExecutor.containerExists(anyString())).thenReturn(true);
        when(dockerExecutor.stopContainer(anyString())).thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleDown(2);

        verify(dockerExecutor, times(2)).stopContainer(anyString());
        verify(containerManager, times(2)).removeContainer(anyString());
        verify(eventPublisher, times(2)).publishScalingEvent(anyString(), anyString());
    }

    @Test
    public void testHandleScaleDownWithNonExistingContainer() {
        when(dockerExecutor.containerExists("soft40051-files-container1")).thenReturn(true);
        when(dockerExecutor.containerExists("soft40051-files-container2")).thenReturn(false);
        when(dockerExecutor.stopContainer("soft40051-files-container1"))
                .thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleDown(2);

        verify(dockerExecutor, times(1)).stopContainer(anyString());
        verify(containerManager, times(1)).removeContainer(anyString());
        verify(eventPublisher, times(1)).publishScalingEvent(anyString(), anyString());
    }

    @Test
    public void testHandleScaleUpFailure() {
        when(dockerExecutor.containerExists(anyString())).thenReturn(false);
        when(dockerExecutor.runContainer(anyString(), anyInt(), anyString()))
                .thenReturn(new ProcessResult(1, "", "error"));

        scalingLogic.handleScaleUp(1);

        verify(dockerExecutor, times(1)).runContainer(anyString(), anyInt(), anyString());
        verify(containerManager, never()).addContainer(anyString());
        verify(eventPublisher, never()).publishScalingEvent(anyString(), anyString());
    }

    @Test
    public void testHandleScaleDownFailure() {
        when(dockerExecutor.containerExists(anyString())).thenReturn(true);
        when(dockerExecutor.stopContainer(anyString())).thenReturn(new ProcessResult(1, "", "error"));

        scalingLogic.handleScaleDown(1);

        verify(dockerExecutor, times(1)).stopContainer(anyString());
        verify(containerManager, never()).removeContainer(anyString());
        verify(eventPublisher, never()).publishScalingEvent(anyString(), anyString());
    }
}
