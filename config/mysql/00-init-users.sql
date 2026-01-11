CREATE DATABASE IF NOT EXISTS dbtutorial;

CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'admin';
GRANT ALL PRIVILEGES ON dbtutorial.* TO 'admin'@'%';

CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'admin';
GRANT ALL PRIVILEGES ON dbtutorial.* TO 'admin'@'localhost';

FLUSH PRIVILEGES;
