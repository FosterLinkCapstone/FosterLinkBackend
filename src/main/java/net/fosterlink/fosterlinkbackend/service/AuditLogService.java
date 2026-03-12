package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.AuditLogEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * Programmatic audit logging for actions where the target user is not a direct
 * method parameter (e.g. hiding or permanently deleting another user's content).
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private @Autowired AuditLogRepository auditLogRepository;

    /**
     * Records an audit entry: acting user (from JWT) performed the given action
     * against the target user. No-op if there is no logged-in user or target user is invalid.
     *
     * @param action       short action description (e.g. "hid thread", "permanently deleted FAQ")
     * @param targetUserId database id of the user who is the target of the action (e.g. content author)
     */
    public void log(String action, int targetUserId) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) {
            log.warn("Could not audit \"{}\": no logged-in user", action);
            return;
        }
        Integer actingUserId = loggedIn.getDatabaseId();
        if (actingUserId == null) {
            log.warn("Could not audit \"{}\": logged-in user has no database id", action);
            return;
        }

        AuditLogEntity entity = new AuditLogEntity();
        entity.setAction(Objects.requireNonNull(action));
        entity.setActingUserId(actingUserId);
        entity.setTargetUserId(targetUserId);
        entity.setCreatedAt(new Date());
        auditLogRepository.save(entity);
    }
}
