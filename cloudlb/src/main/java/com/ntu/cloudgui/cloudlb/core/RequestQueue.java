// com/ntu/cloudlb/core/RequestQueue.java
package com.ntu.cloudgui.cloudlb.core;

import java.util.Comparator;
import java.util.PriorityQueue;

public class RequestQueue {

    private final PriorityQueue<Request> queue;

    public RequestQueue() {
        this.queue = new PriorityQueue<>(Comparator.comparingDouble(this::priorityScore).reversed());
    }

    // Aging: effectivePriority = basePriority + k * waitSeconds - sizeFactor
    private double priorityScore(Request r) {
        long now = System.currentTimeMillis();
        double waitSeconds = (now - r.getArrivalTimeMillis()) / 1000.0;
        double aging = 0.1 * waitSeconds;          // tune k=0.1 for aging rate
        double sizePenalty = r.getSizeBytes() / 1_000_000.0; // penalise huge jobs slightly
        return r.getBasePriority() + aging - sizePenalty;
    }

    public synchronized void add(Request r) {
        queue.add(r);
    }

    public synchronized Request take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.poll();
    }

    public synchronized void notifyNewRequest() {
        notifyAll();
    }

    public synchronized int size() {
        return queue.size();
    }
}
