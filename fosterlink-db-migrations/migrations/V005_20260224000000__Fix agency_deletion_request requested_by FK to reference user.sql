-- Fix agency_deletion_request.requested_by FK: baseline incorrectly referenced non-existent table `agent`.
-- It should reference `user` (id). Drop old constraint and add correct one.
ALTER TABLE agency_deletion_request DROP FOREIGN KEY agency_deletion_request_ibfk_2;
ALTER TABLE agency_deletion_request
  ADD CONSTRAINT agency_deletion_request_ibfk_2
  FOREIGN KEY (requested_by) REFERENCES `user` (id);
