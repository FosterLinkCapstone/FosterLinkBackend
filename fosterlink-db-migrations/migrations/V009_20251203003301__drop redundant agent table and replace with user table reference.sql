SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `agency` DROP INDEX `agent`;
ALTER TABLE `agency` DROP FOREIGN KEY `fk_agency_agent1_idx`;

ALTER TABLE `agent` DROP FOREIGN KEY `assoc_user`;

ALTER TABLE `user` DROP FOREIGN KEY `FKfammntjhpwi01r8avb2qe1pqx`;
ALTER TABLE `user` DROP INDEX `UKjp3re2wg0m79we3ga90fcie8s`;
DROP TABLE `agent`;

ALTER TABLE `agency` ADD FOREIGN KEY (agent) REFERENCES user(id);

ALTER TABLE `user` DROP `agent_id`;