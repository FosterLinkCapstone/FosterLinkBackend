ALTER TABLE `thread_reply` ADD `thread_id` INT NOT NULL AFTER `posted_by`;
ALTER TABLE thread_reply
    ADD CONSTRAINT fk_thread_reply_thread_id
    FOREIGN KEY (thread_id)
    REFERENCES thread(id); 