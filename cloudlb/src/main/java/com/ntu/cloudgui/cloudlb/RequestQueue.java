package com.ntu.cloudgui.cloudlb;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * RequestQueue Class - Thread-safe queue for file operation requests
 * 
 * Manages a queue of file upload/download requests with:
 * - Thread-safe operations using ReentrantLock
 * - Priority-based ordering (SJN - Shortest Job Next)
 * - Aging mechanism to prevent starvation
 * - Notifications for request arrival
 * 
 * Features:
 * - FIFO ordering with SJN priority override
 * - Automatic aging: older requests get higher priority
 * - Thread-safe add/remove operations
 * - Wait/notify mechanism for consumers
 * 
 * Thread Safety: Thread-safe (ReentrantLock + Condition)
 * 
 * Usage:
 * ```
 * RequestQueue queue = new RequestQueue();
 * queue.add(request);              // Add request
 * Request req = queue.get();        // Get next request (blocking)
 * int size = queue.size();          // Get queue size
 * queue.notifyNewRequest();         // Wake up waiting threads
 * ```
 */
public class RequestQueue {

    private final PriorityQueue<Request> queue;
    private final Lock lock;
    private final Condition notEmpty;

    /**
     * Create a new RequestQueue.
     * 
     * Uses ReentrantLock for synchronization and Condition for
     * thread wait/notify mechanism.
     */
    public RequestQueue() {
        this.queue = new PriorityQueue<>();
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
    }

    /**
     * Add a request to the queue.
     * 
     * Request is inserted in order based on:
     * 1. Priority (lower = higher priority)
     * 2. File size (smaller files get priority - SJN)
     * 3. Age (older requests get boosted priority - anti-starvation)
     * 
     * Thread-safe operation.
     * 
     * @param request Request to add
     */
    public void add(Request request) {
        lock.lock();
        try {
            queue.offer(request); // Use offer for PriorityQueue
            
            System.out.printf("[Queue] Added %s request (%.2f MB) - Queue size: %d%n",
                request.getType().getDisplayName(),
                request.getSizeMB(),
                queue.size());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the next request from the queue.
     * 
     * Retrieves the request with the highest priority. The priority is
     * determined by the compareTo method in the Request class (which
     * implements SJN + aging).
     * 
     * Blocking operation: Waits if queue is empty.
     * 
     * @return Next request to process
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public Request get() throws InterruptedException {
        lock.lock();
        try {
            // Wait until queue is not empty
            while (queue.isEmpty()) {
                notEmpty.await();
            }

            // PriorityQueue automatically provides the highest priority element
            Request request = queue.poll();
            
            System.out.printf("[Queue] Retrieved %s request (%.2f MB) - Queue size: %d%n",
                request.getType().getDisplayName(),
                request.getSizeMB(),
                queue.size());
            
            return request;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the current queue size.
     * 
     * Thread-safe operation.
     * 
     * @return Number of pending requests
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if queue is empty.
     * 
     * Thread-safe operation.
     * 
     * @return true if queue is empty
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear all requests from the queue.
     * 
     * Thread-safe operation. Useful for testing and resets.
     */
    public void clear() {
        lock.lock();
        try {
            queue.clear();
            System.out.println("[Queue] Cleared");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notify waiting threads that a new request has arrived.
     * 
     * Called by producers (API Server) to wake up waiting consumers
     * (LoadBalancerWorker) that may be waiting in get().
     * 
     * Thread-safe operation.
     */
    public void notifyNewRequest() {
        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get highest priority request from queue.
     * 
     * Priority calculation:
     * 1. Smaller file size = higher priority (SJN)
     * 2. Older age = higher priority (anti-starvation)
     * 3. FIFO as tiebreaker
     * 
     * Formula: priority = fileSize * agingBoost
     * 
     * Note: Called while holding lock.
     * 
     * @return Highest priority request (removed from queue)
     */
    /**
     * Get a string representation of the queue.
     * Shows all pending requests in order.
     * 
     * @return Queue status
     */
    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("RequestQueue{size=").append(queue.size()).append(", requests=[");
            
            // Note: Iterating over a PriorityQueue does not guarantee order.
            // This is just for a snapshot/debugging view.
            int count = 0;
            for (Request req : queue) {
                if (count > 0) sb.append(", ");
                sb.append(String.format("%s(%.2f MB)", 
                    req.getType().getDisplayName(), 
                    req.getSizeMB()));
                count++;
            }
            
            sb.append("]}");
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
}