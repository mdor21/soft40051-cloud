package com.ntu.cloudgui.cloudlb;

/**
 * Request Class - Represents a file operation request
 *
 * Encapsulates file upload/download requests with metadata:
 * - Unique file identifier
 * - Operation type (UPLOAD or DOWNLOAD)
 * - File size in bytes
 * - Creation timestamp
 * - Priority for scheduling
 *
 * Thread Safety: Immutable (thread-safe)
 *
 * Usage:
 * ```
 * Request uploadReq = new Request(fileId, Request.Type.UPLOAD, fileSizeBytes, priority);
 * Request downloadReq = new Request(fileId, Request.Type.DOWNLOAD, 0, priority);
 * ```
 */
public class Request implements Comparable<Request> {

    /**
     * Request Type Enumeration
     *
     * UPLOAD: File upload operation (client → load balancer → storage)
     * DOWNLOAD: File download operation (storage → load balancer → client)
     */
    public enum Type {
        UPLOAD("Upload"),
        DOWNLOAD("Download");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Request properties
    private final String id;                    // Unique file identifier
    private final Type type;                    // UPLOAD or DOWNLOAD
    private final long sizeBytes;               // File size in bytes
    private final long createdTimeMs;           // Creation timestamp (milliseconds)
    private final int priority;                 // Priority level (0 = highest)

    /**
     * Create a new file operation request.
     *
     * @param id Unique file identifier (UUID recommended)
     * @param type Request type (UPLOAD or DOWNLOAD)
     * @param sizeBytes File size in bytes
     * @param priority Priority level (0 = highest, used for scheduling)
     */
    public Request(String id, Type type, long sizeBytes, int priority) {
        this.id = id;
        this.type = type;
        this.sizeBytes = sizeBytes;
        this.createdTimeMs = System.currentTimeMillis();
        this.priority = priority;
    }

    /**
     * FOR TESTING ONLY: Create a new file operation request with a specific creation time.
     *
     * @param id Unique file identifier (UUID recommended)
     * @param type Request type (UPLOAD or DOWNLOAD)
     * @param sizeBytes File size in bytes
     * @param priority Priority level (0 = highest, used for scheduling)
     * @param creationTimeMs The creation time in milliseconds since the epoch.
     */
    public Request(String id, Type type, long sizeBytes, int priority, long creationTimeMs) {
        this.id = id;
        this.type = type;
        this.sizeBytes = sizeBytes;
        this.createdTimeMs = creationTimeMs;
        this.priority = priority;
    }

    /**
     * Get the unique request/file identifier.
     *
     * @return File ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the type of request.
     *
     * @return UPLOAD or DOWNLOAD
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the file size in bytes.
     *
     * @return File size (bytes)
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * Get the file size in megabytes.
     * Convenience method for logging.
     *
     * @return File size (MB)
     */
    public double getSizeMB() {
        return sizeBytes / 1_000_000.0;
    }

    /**
     * Get the request creation timestamp.
     *
     * @return Milliseconds since epoch when request was created
     */
    public long getCreatedTimeMs() {
        return createdTimeMs;
    }

    /**
     * Get the time elapsed since request creation.
     *
     * @return Milliseconds elapsed
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - createdTimeMs;
    }

    /**
     * Get the request priority.
     *
     * Used by schedulers for request prioritization (aging).
     * Lower value = higher priority.
     *
     * @return Priority level
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Check if this is an upload request.
     *
     * @return true if UPLOAD, false otherwise
     */
    public boolean isUpload() {
        return type == Type.UPLOAD;
    }

    /**
     * Check if this is a download request.
     *
     * @return true if DOWNLOAD, false otherwise
     */
    public boolean isDownload() {
        return type == Type.DOWNLOAD;
    }

    /**
     * Get a string representation of the request.
     * Useful for logging and debugging.
     *
     * @return Formatted request description
     */
    @Override
    public String toString() {
        return String.format(
            "Request{id='%s', type=%s, size=%.2f MB, age=%d ms, priority=%d}",
            id, type.getDisplayName(), getSizeMB(), getAgeMs(), priority
        );
    }

    /**
     * Check equality based on request ID.
     * Two requests with same ID are considered equal.
     *
     * @param obj Object to compare
     * @return true if IDs match
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof Request)) return false;
        Request other = (Request) obj;
        return this.id.equals(other.id);
    }

    /**
     * Get hash code based on request ID.
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Compares this request with another request for priority ordering.
     *
     * The request with the lower priority score is considered "less than"
     * the other, meaning it has higher priority and should come first in
     * a priority queue.
     *
     * @param other The other request to compare to.
     * @return A negative integer, zero, or a positive integer as this request
     *         has higher, equal, or lower priority than the specified request.
     */
    @Override
    public int compareTo(Request other) {
        double thisScore = this.calculatePriorityScore();
        double otherScore = other.calculatePriorityScore();
        return Double.compare(thisScore, otherScore);
    }

    /**
     * Calculates the priority score for this request.
     * Lower score = higher priority.
     *
     * @return The priority score.
     */
    private double calculatePriorityScore() {
        // These constants should ideally be configurable
        final double AGE_SCORE_FACTOR = 0.01; // Subtract this value from score per millisecond of age

        double baseScore = this.getSizeMB();
        long ageMs = this.getAgeMs();

        // The score is the size in MB minus a factor of its age.
        // This ensures that over time, any large job will eventually
        // have a lower score (higher priority) than a new, smaller job.
        double agingPenalty = ageMs * AGE_SCORE_FACTOR;

        return baseScore - agingPenalty;
    }

    public long getCreationTime() {
        return createdTimeMs;
    }

    public String getOperation() {
        return type.getDisplayName();
    }
}
