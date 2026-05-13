CREATE TABLE IF NOT EXISTS buckets (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INTEGER DEFAULT 10,
    is_default BOOLEAN DEFAULT FALSE,
    created_by_user BOOLEAN DEFAULT FALSE
);

-- Optional: Core Seed Data
INSERT INTO buckets (id, name, description, priority, is_default, created_by_user) 
VALUES 
    ('important', 'Important', 'Requires attention', 1, true, false),
    ('can_wait', 'Can Wait', 'Not urgent', 2, true, false),
    ('auto_archive', 'Auto-Archive', 'Automatically archived', 3, true, true),
    ('newsletter', 'Newsletter', 'Newsletters and subscriptions', 4, true, false),
    ('transactional', 'Transactional', 'Receipts, orders, and security alerts', 5, true, false),
    ('personal', 'Personal', 'Personal correspondence', 6, true, false),
    ('work', 'Work / Professional', 'Work-related emails', 7, true, false),
    ('needs_review', 'Needs Review', 'Requires manual review', 8, true, false)
ON CONFLICT (id) DO NOTHING;
