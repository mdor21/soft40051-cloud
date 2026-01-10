# Database Schema Creation

This document outlines the database schema creation process for the cloud application.

## Schema Creation

The `AggService` module is responsible for creating the MySQL database schema. The schema creation logic is located in the `AggService/src/main/java/com/ntu/cloudgui/aggservice/SchemaManager.java` file.

The `resetDatabaseSchema` method in this class is called every time the `aggregator` service starts, ensuring a clean and consistent database state. This method drops all existing tables and recreates them.

## Key Tables

The `cloud-gui` application relies on the following tables, which are created by the `AggService`:

*   `User_Profiles`: Stores user information, including usernames, encrypted passwords, and roles.
*   `File_Metadata`: Contains metadata about the files stored in the system, such as file names, sizes, and timestamps.
*   `ACL`: Manages access control lists for files, defining user permissions.
*   `System_Logs`: Records system events and user actions for auditing and debugging purposes.
