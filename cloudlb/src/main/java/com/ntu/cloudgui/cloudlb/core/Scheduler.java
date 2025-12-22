// com/ntu/cloudlb/core/Scheduler.java
package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import com.ntu.cloudgui.cloudlb.core.StorageNode;
import java.util.List;

public interface Scheduler {
    StorageNode selectNode(List<StorageNode> healthyNodes, Request req);
    String getName();
}
