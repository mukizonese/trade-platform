-- Trade Platform MySQL User Initialization
-- This script creates the database and user
-- MySQL 8.4 uses caching_sha2_password by default (compatible with modern JDBC drivers)

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS trade_db;

-- Create user if not exists
-- MySQL 8.4 will use caching_sha2_password (default, compatible with JDBC)
CREATE USER IF NOT EXISTS 'trade_user'@'%' IDENTIFIED BY 'trade_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON trade_db.* TO 'trade_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

