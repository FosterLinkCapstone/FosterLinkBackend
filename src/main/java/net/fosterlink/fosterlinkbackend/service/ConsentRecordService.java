package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.ConsentRecordEntity;
import net.fosterlink.fosterlinkbackend.repositories.ConsentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class ConsentRecordService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    @Value("${app.emailHashPepper}")
    private String emailHashPepper;

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
        entity.setIpAddress(hmacIp(ipAddress));
        consentRecordRepository.save(entity);
    }

    /**
     * Returns a keyed HMAC-SHA256 hex digest of the raw IP address using the shared
     * EMAIL_HASH_PEPPER. Storing a keyed hash rather than the raw IP minimises PII exposure
     * in the consent_record table while still allowing within-table correlation (the same IP
     * always produces the same hash under the same key).
     */
    private String hmacIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    emailHashPepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to hash IP address for consent record", e);
        }
    }

}
