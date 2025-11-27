-- Trade Platform MySQL Schema Initialization
-- This script creates the initial database schema for the trade platform

USE trade_db;

-- Create trade table (canonical storage)
CREATE TABLE IF NOT EXISTS trade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    counter_party_id VARCHAR(255) NOT NULL,
    book_id VARCHAR(255) NOT NULL,
    maturity_date DATE NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired CHAR(1) NOT NULL DEFAULT 'N',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_id_version (trade_id, version),
    INDEX idx_trade_id (trade_id),
    INDEX idx_maturity_date_expired (maturity_date, expired)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

