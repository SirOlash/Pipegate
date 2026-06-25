CREATE TABLE merchants (
                           id VARCHAR(36) PRIMARY KEY,
                           business_name VARCHAR(255) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           phone_number VARCHAR(20) NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,
                           merchant_code VARCHAR(50) UNIQUE,
                           api_key VARCHAR(255) UNIQUE,
                           secret_key_hash VARCHAR(255),
                           mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
                           webhook_url VARCHAR(500),
                           allowed_ips TEXT,

                           rc_number VARCHAR(100),
                           cac_number VARCHAR(100),
                           business_address TEXT,
                           state VARCHAR(100),
                           country VARCHAR(100) DEFAULT 'Nigeria',

                           status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP NOT NULL
);

CREATE TABLE merchant_services (
                                   id VARCHAR(36) PRIMARY KEY,
                                   merchant_id VARCHAR(36) NOT NULL,
                                   service_type VARCHAR(50) NOT NULL,
                                   status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
                                   requested_at TIMESTAMP NOT NULL,
                                   reviewed_at TIMESTAMP,
                                   reviewed_by VARCHAR(255),
                                   rejection_reason TEXT,

                                   CONSTRAINT fk_merchant_services_merchant
                                       FOREIGN KEY (merchant_id)
                                           REFERENCES merchants(id),

                                   CONSTRAINT uq_merchant_service
                                       UNIQUE (merchant_id, service_type)
);

CREATE TABLE merchant_verifications (
                                        id VARCHAR(36) PRIMARY KEY,
                                        merchant_email VARCHAR(255) NOT NULL,
                                        otp_hash VARCHAR(255) NOT NULL,
                                        expires_at TIMESTAMP NOT NULL,
                                        used BOOLEAN NOT NULL DEFAULT FALSE,
                                        created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_merchants_email
    ON merchants(email);
CREATE INDEX idx_merchants_api_key
    ON merchants(api_key);
CREATE INDEX idx_merchants_merchant_code
    ON merchants(merchant_code);
CREATE INDEX idx_merchant_services_merchant_id
    ON merchant_services(merchant_id);
CREATE INDEX idx_merchant_verifications_email
    ON merchant_verifications(merchant_email);