package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.ConsentRecordEntity;
import net.fosterlink.fosterlinkbackend.repositories.ConsentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsentRecordService {

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    /**
     * Returns all consent records for the given user ID.
     */
    public List<ConsentRecordEntity> findByUserId(int userId) {
        return consentRecordRepository.findByUserId(userId);
    }

    /**
     * Appends an immutable consent record for the given user.
     * This method is insert-only: existing records are never modified or deleted,
     * preserving the full audit trail of consent changes over time.
     */
    @Transactional
    public void record(Long userId, String consentType, boolean granted,
                       String policyVersion, String mechanism, String ipAddress) {
        ConsentRecordEntity entity = new ConsentRecordEntity();
        entity.setUserId(userId.intValue());
        entity.setConsentType(consentType);
        entity.setGranted(granted);
        entity.setTimestamp(LocalDateTime.now());
        entity.setPolicyVersion(policyVersion);
        entity.setMechanism(mechanism);
        entity.setIpAddress(truncateIp(ipAddress));
        consentRecordRepository.save(entity);
    }

    private static String truncateIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            if (ip.contains(":")) {
                // IPv6: keep first 48 bits (first 3 groups), zero the rest
                String[] parts = ip.split(":");
                if (parts.length < 3) return null;
                return parts[0] + ":" + parts[1] + ":" + parts[2] + "::";
            } else {
                // IPv4: zero the last octet
                String[] parts = ip.split("\\.");
                if (parts.length != 4) return null;
                return parts[0] + "." + parts[1] + "." + parts[2] + ".0";
            }
        } catch (Exception e) {
            return null;
        }
    }

}
