ALTER TABLE faq_approval ADD hidden_by VARCHAR(50) NULL;
ALTER TABLE faq_approval ADD hidden_by_author BOOLEAN NOT NULL DEFAULT FALSE;