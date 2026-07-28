-- V17 Reporting & Analytics Migration
CREATE TABLE report_snapshots (
    id BIGSERIAL PRIMARY KEY,
    report_number VARCHAR(50) NOT NULL UNIQUE,
    report_type VARCHAR(50) NOT NULL,
    generated_by VARCHAR(150) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parameters JSONB,
    file_path VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_report_snapshots_type ON report_snapshots(report_type);
CREATE INDEX idx_report_snapshots_number ON report_snapshots(report_number);
CREATE INDEX idx_report_snapshots_generated_by ON report_snapshots(generated_by);
