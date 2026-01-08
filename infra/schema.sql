CREATE TABLE IF NOT EXISTS `User_Profiles` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `encrypted_password` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`)
);

CREATE TABLE IF NOT EXISTS `File_Metadata` (
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `chunk_id` int(11) NOT NULL,
  `crc32_checksum` varchar(255) NOT NULL,
  `server_location` varchar(255) NOT NULL,
  `sequence_order` int(11) NOT NULL,
  PRIMARY KEY (`file_id`)
);

CREATE TABLE IF NOT EXISTS `ACL` (
  `acl_id` int(11) NOT NULL AUTO_INCREMENT,
  `file_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `permission` varchar(255) NOT NULL,
  PRIMARY KEY (`acl_id`)
);

CREATE TABLE IF NOT EXISTS `System_Logs` (
  `log_id` int(11) NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `event_type` varchar(255) NOT NULL,
  `user_id` int(11) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`log_id`)
);
