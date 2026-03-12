package net.fosterlink.fosterlinkbackend.repositories.mappers;

import net.fosterlink.fosterlinkbackend.models.rest.AuditLogEntryResponse;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Maps repository query rows to AuditLogEntryResponse.
 * Row order: id, action, target_first_name, target_last_name, target_username,
 *            acting_first_name, acting_last_name, acting_username, created_at
 */
@Component
public class AuditLogMapper {

    public AuditLogEntryResponse mapRow(Object[] row) {
        AuditLogEntryResponse res = new AuditLogEntryResponse();
        res.setId(((Number) row[0]).intValue());
        res.setAction((String) row[1]);
        String targetUser = formatUser(str(row[2]), str(row[3]), str(row[4]));
        String actingUser = formatUser(str(row[5]), str(row[6]), str(row[7]));
        res.setCreatedAt(row[8] != null ? (Date) row[8] : null);
        res.setTargetUser(targetUser);
        res.setActingUser(actingUser);
        res.setDisplayMessage(actingUser + " " + res.getAction() + " " + targetUser);
        return res;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String formatUser(String firstName, String lastName, String username) {
        String name = (firstName + " " + lastName).trim();
        if (name.isEmpty()) name = username.isEmpty() ? "(unknown)" : username;
        return name + " (" + (username.isEmpty() ? "—" : username) + ")";
    }
}
