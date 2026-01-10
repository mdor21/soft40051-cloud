# Sync and Persistence Validation Report

This document outlines the findings of a code review validating the current implementation against the requirements for persistence, synchronization, and security.

---

## 1. Persistence Layers and Roles

**Verdict: Meets Requirements**

### Findings:
- **MySQL as Master Ledger:** **Confirmed.** The `AggService` is responsible for the master database schema. The class `com.ntu.cloudgui.aggservice.SchemaManager` explicitly creates the core tables (`User_Profiles`, `File_Metadata`, `ACL`, `System_Logs`) in MySQL. This aligns with its role as the central, authoritative data store.

- **SQLite as Local Cache:** **Confirmed.** The `cloud-gui` application uses a local SQLite database for offline capabilities. The schema is defined in `com.ntu.cloudgui.app.db.SessionCacheRepository`, which creates tables specifically for caching and offline operations.

- **Table Usage for Dual-Database Behaviour:** **Confirmed.** The following tables are actively used to manage the dual-database system:
    - `session_users`: Caches user credentials for offline login, as seen in `SessionCacheRepository.cacheUser()` and `findCachedUserByUsername()`.
    - `file_cache`: Intended for local file metadata (though not fully explored in this review).
    - `sync_queue`: This is the core of the offline functionality. The `SessionCacheRepository.queueOperation()` method adds tasks when offline, and the `com.ntu.cloudgui.app.service.SyncService` reads from this table using `getQueuedOperations()` to process pending changes when a connection to MySQL is restored.

---

## 2. Sync and Conflict Handling

**Verdict: Partially Meets Requirements**

### Findings:
- **Conflict Detection:** **Implemented.** The `SyncService` includes logic to detect conflicts. In `handleUserOperation`, it retrieves both the local and remote versions of a user record and compares their `lastModified` timestamps to identify if the remote record was changed after the local change was queued.

- **Conflict Resolution:** **Partially Implemented.**
    - **Strength:** For high-importance conflicts, like a change to a user's role, the system implements an excellent interactive resolution strategy. It uses `Platform.runLater` to show a JavaFX confirmation dialog (`showConflictResolutionDialog`) and a `CompletableFuture` to block the background sync thread until the user decides whether to overwrite the remote data. This prevents data loss for critical changes.
    - **Gap:** For standard data conflicts (e.g., any user update that isn't a role change), the current logic defaults to a simple "discard local change" (`return true; // Discard local change`), effectively implementing a "remote-wins" strategy without logging it as clearly or offering other options. For a Distinction-level implementation, this should be expanded to include explicit strategies like "last-write-wins" (comparing timestamps) or configurable defaults. The current implementation is good but not comprehensive.

---

## 3. Concurrency and Semaphores

**Verdict: Does Not Meet Requirements**

### Findings:
- **Missing Concurrency Control:** The `SyncService` runs in a dedicated background thread, which prevents it from blocking the UI. However, the synchronization operations themselves are not protected by any concurrency controls like semaphores or locks.
- **Gap:** While the `DatabaseManager`'s method for getting a connection is `synchronized`, this only protects the instantiation of the connection object, not the broader synchronization logic. The `processSyncQueue` method, which reads the queue and handles operations, is not atomic. If another thread were to interact with the `sync_queue` or the `SessionCacheRepository` while the `SyncService` is in the middle of processing, it could lead to race conditions or inconsistent state.
- **Recommendation:** A `java.util.concurrent.Semaphore` or `ReentrantLock` should be introduced within the `SyncService` to ensure that the entire process of reading the queue and applying changes is an atomic operation, preventing concurrent modification issues.

---

## 4. Security of Stored Credentials

**Verdict: Partially Meets Requirements**

### Findings:
- **MySQL Password Hashing:** **Confirmed.** The `SchemaManager` in `AggService` uses `org.mindrot.jbcrypt.BCrypt.hashpw()` to securely hash and salt the default admin password before storing it in the `User_Profiles` table in MySQL. This follows best practices.

- **SQLite Credential Security:** **Partially Implemented.**
    - **Strength:** Plaintext passwords are not stored in the `session_users` table. The `cacheUser` method in `SessionCacheRepository` calls `user.getPasswordHash()`, indicating that only the hashed value is cached.
    - **Gap:** Storing a password hash in a local SQLite file is a security risk. While better than plaintext, this file could be extracted from the user's machine, and the hash could be subjected to offline brute-force attacks. A more secure approach (required for Distinction) would be to cache a short-lived, revocable session *token* instead of the password hash. This token would be used for authentication against the local cache, and a new one would be issued by the server upon a successful online login.

---

## 5. Starvation/Aging in Sync Tasks

**Verdict: Does Not Meet Requirements**

### Findings:
- **FIFO Queue Implementation:** The `getQueuedOperations()` method in `SessionCacheRepository` retrieves pending tasks using a simple `ORDER BY created_at ASC` SQL query. This is a strict First-In, First-Out (FIFO) implementation.

- **Gap:** The current FIFO approach does not implement any aging or fairness mechanism.
    - **Under Sustained Load:** If the queue becomes very long, older tasks at the front of the queue will block all newer tasks until they are completed. If an early task repeatedly fails (e.g., due to a persistent server-side validation error), it will remain in the queue and be retried on every sync cycle, effectively blocking the entire queue and "starving" all subsequent tasks.
    - **Recommendation:** To meet Distinction requirements, an aging mechanism should be introduced. This could involve adding a `priority` and `attempts` column to the `sync_queue` table. The `SyncService` could then prioritize tasks that have been waiting longer or have failed multiple times, perhaps by moving them to a separate "dead-letter" queue after a certain number of failed attempts to prevent them from blocking fresh tasks.
