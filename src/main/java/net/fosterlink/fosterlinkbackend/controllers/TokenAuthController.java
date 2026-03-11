package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.tokenauth.TokenAuth;
import net.fosterlink.fosterlinkbackend.entities.TokenAuthEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.mail.service.AdminUserMailService;
import net.fosterlink.fosterlinkbackend.repositories.DontSendEmailRepository;
import net.fosterlink.fosterlinkbackend.repositories.TokenAuthRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.service.BanStatusService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private @Autowired DontSendEmailRepository dontSendEmailRepository;

    @Operation(
            summary = "Assign ADMINISTRATOR role via token",
            description = "Assigns the ADMINISTRATOR role to a user after a founder-approved email link is used. " +
                    "The token in the query string must match a non-expired token created for the /assignAdmin endpoint.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "token", description = "Raw token from the approval email link", required = true),
                    @Parameter(name = "userId", description = "ID of the user receiving the ADMINISTRATOR role", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role assigned successfully (idempotent if already assigned)"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            }
    )
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
        String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(target.get());
        adminUserMailService.sendRoleAssignedNotification(userId, target.get().getEmail(), target.get().getFirstName(), "ADMINISTRATOR", unsubscribeToken);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Revoke ADMINISTRATOR role via token",
            description = "Revokes the ADMINISTRATOR role from a user after a founder-approved email link is used. " +
                    "The token in the query string must match a non-expired token created for the /revokeAdmin endpoint.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "token", description = "Raw token from the revocation email link", required = true),
                    @Parameter(name = "userId", description = "ID of the user losing the ADMINISTRATOR role", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role revoked successfully (idempotent if already revoked)"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            }
    )
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
        String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(target.get());
        adminUserMailService.sendRoleRevokedNotification(userId, target.get().getEmail(), target.get().getFirstName(), "ADMINISTRATOR", unsubscribeToken);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Verify user email via token",
            description = "Marks a user's email as verified when a verification link from an email is used. " +
                    "The token in the query string must match a non-expired token created for the /verifyEmail endpoint.",
            tags = {"User"},
            parameters = {
                    @Parameter(name = "token", description = "Raw token from the verification email link", required = true),
                    @Parameter(name = "userId", description = "ID of the user whose email is being verified", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email verified successfully (idempotent if already verified)"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            }
    )
    @RateLimit(requests = 30)
    @PostMapping("/verifyEmail")
    @TokenAuth(endpointName = "/verifyEmail")
    public ResponseEntity<?> verifyEmail(@RequestParam String token, @RequestParam int userId) {
        Optional<UserEntity> target = userRepository.findById(userId);
        if (target.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserEntity user = target.get();
        user.setEmailVerified(true);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Unsubscribe a user from all emails via token",
            description = "Sets unsubscribeAll=true and clears per-type email opt-outs when a user clicks an unsubscribe-all link in an email. " +
                    "The token in the query string must match a non-expired token created for the /unsubscribeAll endpoint.",
            tags = {"Mail"},
            parameters = {
                    @Parameter(name = "token", description = "Raw token from the unsubscribe-all email link", required = true),
                    @Parameter(name = "userId", description = "ID of the user being unsubscribed", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User unsubscribed from all emails (idempotent)"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            }
    )
    @RateLimit(requests = 30)
    @Transactional
    @PostMapping("/unsubscribeAll")
    @TokenAuth(endpointName = "/unsubscribeAll", consumeOnUse = false)
    public ResponseEntity<?> unsubscribeAll(@RequestParam String token, @RequestParam int userId) {
        Optional<UserEntity> target = userRepository.findById(userId);
        if (target.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        target.get().setUnsubscribeAll(true);
        userRepository.save(target.get());
        dontSendEmailRepository.deleteAllByUserId(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Cancels a token-auth token. When called, it looks up the token to discover its processId,
     * then bulk-deletes all outstanding tokens sharing that processId. This ensures that a single
     * "Deny" click in an approval email revokes every founder's outstanding approve link for the
     * same assignment request, with no risk of accidentally revoking tokens from a separate
     * re-request for the same user.
     */
    @Operation(
            summary = "Cancel a token-auth process",
            description = "Cancels all non-expired token-auth tokens that share the same processId as the provided token. " +
                    "This is typically invoked by a \"Deny\" link in an approval email and is idempotent.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "token", description = "Raw token from the deny link", required = true),
                    @Parameter(name = "userId", description = "ID of the user associated with the token process", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens cancelled or already expired/not found"),
                    @ApiResponse(responseCode = "403", description = "Provided userId does not match the token's target user"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            }
    )
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

