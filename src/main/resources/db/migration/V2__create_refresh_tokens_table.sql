CREATE TABLE refresh_tokens (
                                id VARCHAR(36) PRIMARY KEY,
                                merchant_id VARCHAR(36) NOT NULL,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                expires_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP NOT NULL,

                                CONSTRAINT fk_refresh_tokens_merchant
                                    FOREIGN KEY (merchant_id)
                                        REFERENCES merchants(id)
);

CREATE INDEX idx_refresh_tokens_merchant_id
    ON refresh_tokens(merchant_id);
CREATE INDEX idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);