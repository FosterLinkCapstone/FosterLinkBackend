package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.tokenauth.TokenAuth;
import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.mail.service.AdminUserMailService;
import net.fosterlink.fosterlinkbackend.repositories.TokenAuthRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.service.BanStatusService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/v1/token/")
public class TokenAuthController {

    private @Autowired UserRepository userRepository;
    private @Autowired BanStatusService banStatusService;
    private @Autowired AdminUserMailService adminUserMailService;
    private @Autowired TokenAuthRepository tokenAuthRepository;
    private @Autowired TokenAuthService tokenAuthService;

    @RateLimit(requests = 30)
    @PostMapping("/assignAdmin")
    @TokenAuth(endpointName = "/assignAdmin")
    public ResponseEntity<?> assignAdmin(@RequestParam String token, @RequestParam int userId) {
        Optional<UserEntity> target = userRepository.findById(userId);
        if (target.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        target.get().setAdministrator(true);
        userRepository.save(target.get());
        banStatusService.evictUserDetails(target.get().getEmail());
        banStatusService.evictProfileMetadata(target.get().getId());
        adminUserMailService.sendRoleAssignedNotification(userId, target.get().getEmail(), target.get().getFirstName(), "ADMINISTRATOR");
        return ResponseEntity.ok().build();
    }

    @RateLimit(requests = 30)
    @PostMapping("/revokeAdmin")
    @TokenAuth(endpointName = "/revokeAdmin")
    public ResponseEntity<?> revokeAdmin(@RequestParam String token, @RequestParam int userId) {
        Optional<UserEntity> target = userRepository.findById(userId);
        if (target.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        target.get().setAdministrator(false);
        userRepository.save(target.get());
        banStatusService.evictUserDetails(target.get().getEmail());
        banStatusService.evictProfileMetadata(target.get().getId());
        adminUserMailService.sendRoleRevokedNotification(userId, target.get().getEmail(), target.get().getFirstName(), "ADMINISTRATOR");
        return ResponseEntity.ok().build();
    }

    /**
     * Cancels a token-auth token. When called, it looks up the token to discover its processId,
     * then bulk-deletes all outstanding tokens sharing that processId. This ensures that a single
     * "Deny" click in an approval email revokes every founder's outstanding approve link for the
     * same assignment request, with no risk of accidentally revoking tokens from a separate
     * re-request for the same user.
     */
    @RateLimit(requests = 30)
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelToken(@RequestParam String token, @RequestParam int userId) {
        String hashedToken = tokenAuthService.hashToken(token);
        Optional<TokenAuthEntity> tokenEntity = tokenAuthRepository.findByTokenNonExpired(hashedToken);

        if (tokenEntity.isEmpty()) {
            // Token not found or already expired — treat as success so deny links are idempotent
            return ResponseEntity.ok().build();
        }

        TokenAuthEntity entity = tokenEntity.get();
        // Verify the provided userId matches this token's target to prevent unauthorized cancellation
        if (entity.getTargetUserId() == null || entity.getTargetUserId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        tokenAuthRepository.deleteAllByProcessId(entity.getProcessId());
        return ResponseEntity.ok().build();
    }

}

