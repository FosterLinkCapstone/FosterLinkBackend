package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Integer> {

    /**
     * Returns audit log rows with target and acting user display columns for admin listing.
     * Row: id, action, target_first_name, target_last_name, target_username,
     *      acting_first_name, acting_last_name, acting_username, created_at
     */
    @Query(value = """
            SELECT a.id, a.action, t.first_name, t.last_name, t.username,
                   COALESCE(u.first_name, ''), COALESCE(u.last_name, ''), COALESCE(u.username, 'An authorized user'), a.created_at
            FROM audit_log a
            JOIN user t ON a.target_user_id = t.id
            LEFT JOIN user u ON a.acting_user_id = u.id
            ORDER BY a.id DESC
            """,
            countQuery = "SELECT COUNT(*) FROM audit_log a",
            nativeQuery = true)
    Page<Object[]> findAllForAdminDisplay(Pageable pageable);
}
