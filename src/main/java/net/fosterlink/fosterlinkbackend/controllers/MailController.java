package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.entities.DontSendEmailEntity;
import net.fosterlink.fosterlinkbackend.entities.EmailTypeEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.EmailPreferenceResponse;
import net.fosterlink.fosterlinkbackend.models.rest.EmailPreferencesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UpdateEmailPreferencesRequest;
import net.fosterlink.fosterlinkbackend.repositories.DontSendEmailRepository;
import net.fosterlink.fosterlinkbackend.repositories.EmailTypeRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API for mail-related preferences exposed to the frontend.
 * Base path: /v1/mail/
 */
@RestController
@RequestMapping("/v1/mail/")
public class MailController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTypeRepository emailTypeRepository;

    @Autowired
    private DontSendEmailRepository dontSendEmailRepository;

    @Operation(
            summary = "Get email notification preferences for the current user",
            description = "Returns all toggleable email notification types along with whether the current user has individually disabled each one. " +
                    "If the user has unsubscribed from all emails, disabled is returned as false for every entry. " +
                    "Rate limit: 30 requests per 60 seconds per user.",
            tags = {"Mail"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Email preferences for the current user, including unsubscribe-all status and per-type opt-outs",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = EmailPreferencesResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No user found for the current JWT"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded."
                    )
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @GetMapping("/emailPreferences")
    public ResponseEntity<?> getEmailPreferences() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<EmailTypeEntity> disableableTypes = emailTypeRepository.findByCanDisableTrue();

        if (user.isUnsubscribeAll()) {
            List<EmailPreferenceResponse> preferences = disableableTypes.stream()
                    .map(type -> new EmailPreferenceResponse(type.getName(), type.getUiName(), false))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new EmailPreferencesResponse(true, preferences));
        }

        Set<Integer> disabledTypeIds = dontSendEmailRepository.findAllByUserId(user.getId())
                .stream()
                .map(DontSendEmailEntity::getEmailTypeId)
                .collect(Collectors.toSet());

        List<EmailPreferenceResponse> preferences = disableableTypes.stream()
                .map(type -> new EmailPreferenceResponse(type.getName(), type.getUiName(), disabledTypeIds.contains(type.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new EmailPreferencesResponse(false, preferences));
    }

    @Operation(
            summary = "Bulk update email notification preferences for the current user",
            description = "Updates per-type opt-outs. If the user has unsubscribed from all emails, returns 409 until they resubscribe. " +
                    "Only types with canDisable=true are accepted; others are silently ignored.",
            tags = {"Mail"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Preferences updated"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "User is unsubscribed from all emails; resubscribe first"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 20, keyType = "USER")
    @Transactional
    @PutMapping("/emailPreferences")
    public ResponseEntity<?> updateEmailPreferences(@RequestBody UpdateEmailPreferencesRequest request) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (user.isUnsubscribeAll()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (request == null || request.getPreferences() == null) {
            return ResponseEntity.ok().build();
        }

        Map<String, EmailTypeEntity> typesByName = emailTypeRepository.findByCanDisableTrue()
                .stream()
                .collect(Collectors.toMap(EmailTypeEntity::getName, t -> t));

        for (UpdateEmailPreferencesRequest.EmailPreferenceUpdate update : request.getPreferences()) {
            EmailTypeEntity type = typesByName.get(update.getName());
            if (type == null) continue;

            if (update.isDisabled()) {
                if (!dontSendEmailRepository.existsByUserIdAndEmailTypeId(user.getId(), type.getId())) {
                    DontSendEmailEntity entity = new DontSendEmailEntity();
                    entity.setUserId(user.getId());
                    entity.setEmailTypeId(type.getId());
                    dontSendEmailRepository.save(entity);
                }
            } else {
                dontSendEmailRepository.deleteByUserIdAndEmailTypeId(user.getId(), type.getId());
            }
        }

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Unsubscribe the current user from all emails",
            description = "Sets unsubscribe_all=true on the user and removes all per-type opt-outs (they are redundant). " +
                    "The unsaved per-type preferences are wiped server-side; the frontend should reflect this.",
            tags = {"Mail"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User unsubscribed from all emails"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 10, keyType = "USER")
    @Transactional
    @PostMapping("/unsubscribeAll")
    public ResponseEntity<?> unsubscribeAll() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        user.setUnsubscribeAll(true);
        userRepository.save(user);
        dontSendEmailRepository.deleteAllByUserId(user.getId());

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Undo unsubscribe-all for the current user",
            description = "Sets unsubscribe_all=false. Individual per-type preferences are not restored; the user is resubscribed to everything by default.",
            tags = {"Mail"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User resubscribed"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 10, keyType = "USER")
    @PostMapping("/resubscribe")
    public ResponseEntity<?> resubscribe() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        user.setUnsubscribeAll(false);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }
}
