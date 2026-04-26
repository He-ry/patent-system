CREATE TABLE IF NOT EXISTS report_record (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    conversation_id VARCHAR(64)  NOT NULL,
    message_id      VARCHAR(64),
    title           VARCHAR(255) NOT NULL,
    report_type     VARCHAR(32)  NOT NULL DEFAULT 'comprehensive',
    html_path       VARCHAR(512),
    docx_path       VARCHAR(512),
    pdf_path        VARCHAR(512),
    executive_summary TEXT,
    section_count   INT          DEFAULT 0,
    chart_count     INT          DEFAULT 0,
    total_words     INT          DEFAULT 0,
    status          VARCHAR(32)  DEFAULT 'completed',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
