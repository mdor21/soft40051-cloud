-- Use the dbtutorial database
USE dbtutorial;

-- Create User_Profiles table for the Aggregator service
CREATE TABLE IF NOT EXISTS `User_Profiles` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL UNIQUE,
  `encrypted_password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
);

-- Create a separate users table for the GUI service
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL UNIQUE,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- Create File_Metadata table
CREATE TABLE IF NOT EXISTS `File_Metadata` (
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size_bytes` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`)
);

-- Create ACL table
CREATE TABLE IF NOT EXISTS `ACL` (
  `acl_id` int(11) NOT NULL AUTO_INCREMENT,
  `file_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `permission` varchar(50) NOT NULL,
  PRIMARY KEY (`acl_id`),
  FOREIGN KEY (`file_id`) REFERENCES `File_Metadata`(`file_id`),
  FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`)
);

-- Create System_Logs table
CREATE TABLE IF NOT EXISTS `System_Logs` (
  `log_id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `event_type` varchar(255) NOT NULL,
  `user_id` int(11) NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`log_id`)
);

-- Insert the default admin user for the Aggregator service
INSERT IGNORE INTO `User_Profiles` (`username`, `encrypted_password`, `role`)
VALUES ('admin', '$2a$12$4b22e.V.0a.C5/8.t.0rGuM5.E/j5.R3.Z5.K/d.P/q.W/i.O/k.Y', 'admin');

-- Insert the default admin user for the GUI service
INSERT IGNORE INTO `users` (`username`, `password`, `role`)
VALUES ('admin', '$2a$12$4b22e.V.0a.C5/8.t.0rGuM5.E/j5.R3.Z5.K/d.P/q.W/i.O/k.Y', 'ADMIN');
