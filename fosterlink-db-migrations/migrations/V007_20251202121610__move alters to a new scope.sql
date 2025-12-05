ALTER TABLE faq_approval ADD FOREIGN KEY (faq_id) REFERENCES faq(id);
ALTER TABLE faq_approval ADD FOREIGN KEY (approved_by_id) REFERENCES user(id);
ALTER TABLE faq_request ADD CONSTRAINT fk_requested_by_id FOREIGN KEY (requested_by) REFERENCES user(id);
