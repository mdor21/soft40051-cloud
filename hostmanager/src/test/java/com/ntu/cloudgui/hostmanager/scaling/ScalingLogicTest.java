package com.ntu.cloudgui.hostmanager.scaling;

import com.ntu.cloudgui.hostmanager.container.ContainerInfo;
import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        MockitoAnnotations.openMocks(this);
        scalingLogic.setEventPublisher(eventPublisher);
    }

    private List<ContainerInfo> createContainerInfoList(String... names) {
        return Arrays.stream(names).map(ContainerInfo::new).collect(Collectors.toList());
    }

    @Test
    public void testHandleScaleUp_shouldStartOneContainer() {
        when(containerManager.getAllContainers()).thenReturn(Collections.emptyList());
        when(dockerExecutor.runContainer(anyString(), anyInt(), anyString())).thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleUp(1);

        verify(dockerExecutor, times(1)).runContainer(eq("soft40051-files-container1"), eq(4848), anyString());
        verify(containerManager, times(1)).addContainer("soft40051-files-container1");
        verify(eventPublisher, times(1)).publishScalingEvent("up", "soft40051-files-container1");
    }

    @Test
    public void testHandleScaleUp_shouldFillGap() {
        when(containerManager.getAllContainers()).thenReturn(createContainerInfoList("soft40051-files-container1", "soft40051-files-container3"));
        when(dockerExecutor.runContainer(anyString(), anyInt(), anyString())).thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleUp(1);

        verify(dockerExecutor, times(1)).runContainer(eq("soft40051-files-container2"), eq(4849), anyString());
        verify(containerManager, times(1)).addContainer("soft40051-files-container2");
        verify(eventPublisher, times(1)).publishScalingEvent("up", "soft40051-files-container2");
    }

    @Test
    public void testHandleScaleUp_shouldRespectMaxContainers() {
        when(containerManager.getAllContainers()).thenReturn(createContainerInfoList(
                "soft40051-files-container1",
                "soft40051-files-container2",
                "soft40051-files-container3",
                "soft40051-files-container4"
        ));

        scalingLogic.handleScaleUp(1);

        verify(dockerExecutor, never()).runContainer(anyString(), anyInt(), anyString());
    }

    @Test
    public void testHandleScaleDown_shouldRemoveHighestNumbered() {
        when(containerManager.getAllContainers()).thenReturn(createContainerInfoList("soft40051-files-container1", "soft40051-files-container3"));
        when(dockerExecutor.stopContainer(anyString())).thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleDown(1);

        verify(dockerExecutor, times(1)).stopContainer("soft40051-files-container3");
        verify(containerManager, times(1)).removeContainer("soft40051-files-container3");
        verify(eventPublisher, times(1)).publishScalingEvent("down", "soft40051-files-container3");
    }

    @Test
    public void testHandleScaleDown_shouldRemoveMultiple() {
        when(containerManager.getAllContainers()).thenReturn(createContainerInfoList(
                "soft40051-files-container1",
                "soft40051-files-container2",
                "soft40051-files-container4"
        ));
        when(dockerExecutor.stopContainer(anyString())).thenReturn(new ProcessResult(0, "success", ""));

        scalingLogic.handleScaleDown(2);

        verify(dockerExecutor, times(1)).stopContainer("soft40051-files-container4");
        verify(dockerExecutor, times(1)).stopContainer("soft40051-files-container2");
        verify(containerManager, times(2)).removeContainer(anyString());
        verify(eventPublisher, times(2)).publishScalingEvent(eq("down"), anyString());
    }
}
