CREATE TABLE merchants (
                           id VARCHAR(36) PRIMARY KEY,
                           business_name VARCHAR(255) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           api_key VARCHAR(255) UNIQUE,
                           secret_key_hash VARCHAR(255),
                           mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
                           integration_type VARCHAR(50) NOT NULL,
                           webhook_url VARCHAR(500),
                           allowed_ips TEXT,
                           status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP NOT NULL
);

CREATE TABLE merchant_verifications (
                                        id VARCHAR(36) PRIMARY KEY,
                                        merchant_email VARCHAR(255) NOT NULL,
                                        otp_hash VARCHAR(255) NOT NULL,
                                        expires_at TIMESTAMP NOT NULL,
                                        used BOOLEAN NOT NULL DEFAULT FALSE,
                                        created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_api_key ON merchants(api_key);
CREATE INDEX idx_merchant_verifications_email ON merchant_verifications(merchant_email);