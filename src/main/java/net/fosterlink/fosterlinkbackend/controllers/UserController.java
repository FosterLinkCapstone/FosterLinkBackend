package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.audit.AuditLog;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.config.tokenauth.TokenAuth;
import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ProfileMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PrivilegesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserDataExportResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserSettingsResponse;
import net.fosterlink.fosterlinkbackend.models.web.user.ChangePasswordModel;
import net.fosterlink.fosterlinkbackend.models.web.user.ForgotPasswordModel;
import net.fosterlink.fosterlinkbackend.models.web.user.ResetPasswordModel;
import net.fosterlink.fosterlinkbackend.models.web.user.UpdateUserModel;
import net.fosterlink.fosterlinkbackend.models.web.user.UserLoginModelEmail;
import net.fosterlink.fosterlinkbackend.models.web.user.UserRegisterModel;
import net.fosterlink.fosterlinkbackend.mail.service.HomeMailService;
import net.fosterlink.fosterlinkbackend.mail.service.UserMailService;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.AccountDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.DontSendEmailRepository;
import net.fosterlink.fosterlinkbackend.repositories.MailingListMemberRepository;
import net.fosterlink.fosterlinkbackend.repositories.ThreadRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.UserMapper;
import net.fosterlink.fosterlinkbackend.security.JwtTokenProvider;
import net.fosterlink.fosterlinkbackend.service.BanStatusService;
import net.fosterlink.fosterlinkbackend.service.RefreshTokenService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for user operations: registration, login, profile updates, privilege checks, and profile metadata.
 * Base path: /v1/users/
 */
@RestController
@RequestMapping("/v1/users/")
public class UserController {

    private static final String DEFAULT_PROFILE_PIC = UserConstants.DEFAULT_PROFILE_PIC;
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${app.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;

    @Value("${app.refreshTokenExpirationLongMs}")
    private long refreshTokenExpirationLongMs;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserMapper userMapper;
    @Autowired(required = false)
    private UserMailService userMailService;
    @Autowired(required = false)
    private HomeMailService homeMailService;
    @Autowired
    private BanStatusService banStatusService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private net.fosterlink.fosterlinkbackend.service.TokenAuthService tokenAuthService;
    @Autowired
    private net.fosterlink.fosterlinkbackend.service.ConsentRecordService consentRecordService;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private DontSendEmailRepository dontSendEmailRepository;
    @Autowired
    private MailingListMemberRepository mailingListMemberRepository;
    @Autowired
    private AccountDeletionRequestRepository accountDeletionRequestRepository;

    @Operation(
            summary = "Register a new user",
            description = "Registers a new user account and returns a JWT token. Rate limit: 5 requests per 60 seconds per IP, with burst limit of 1 request per 30 seconds.",
            tags={"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The user was successfully registered and logged in",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(type="string", name = "token", description = "The logged-in JWT of the user"))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A user with that email or username already exists. Both properties must be unique."
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 5 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 5, burstRequests = 4, burstDurationSeconds = 30)
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegisterModel model, HttpServletRequest request, HttpServletResponse response) {

        if (userRepository.existsByUsernameOrEmail(model.getUsername(), model.getEmail())) {
            return ResponseEntity.status(409).build();
        }
        if (!hasMxRecord(model.getEmail())) {
            return ResponseEntity.status(422).build();
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(model.getUsername());

        userEntity.setEmail(model.getEmail());
        String rawPhone = model.getPhoneNumber();
        userEntity.setPhoneNumber((rawPhone != null && !rawPhone.isBlank()) ? rawPhone : null);

        userEntity.setPassword(passwordEncoder.encode(model.getPassword()));
        userEntity.setFirstName(model.getFirstName());
        userEntity.setLastName(model.getLastName());
        userEntity.setCreatedAt(new Date());
        userEntity.setProfilePictureUrl(DEFAULT_PROFILE_PIC);

        UserEntity dbEntity = userRepository.save(userEntity);

        String forwardedFor = request.getHeader("X-FORWARDED-FOR");
        String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        consentRecordService.record((long) dbEntity.getId(), "AGE_CONFIRMATION", true, null, "REGISTRATION_CHECKBOX", clientIp);
        consentRecordService.record((long) dbEntity.getId(), "TERMS", true, "1.0", "REGISTRATION_CHECKBOX", clientIp);
        consentRecordService.record((long) dbEntity.getId(), "PRIVACY", true, "1.0", "REGISTRATION_CHECKBOX", clientIp);
        consentRecordService.record((long) dbEntity.getId(), "MARKETING", model.isConsentMarketing(), "1.0", "REGISTRATION_CHECKBOX", clientIp);

        try {
            String jwt = loginUser(dbEntity.getEmail(), model.getPassword());
            // Registration never has stayLoggedIn; always issue a session-scoped refresh token
            String refreshToken = refreshTokenService.createRefreshToken(dbEntity, false);
            setRefreshCookie(response, refreshToken, false);

            String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(dbEntity);
            if (userMailService != null) {
                String verifyToken = tokenAuthService.generateToken(
                        net.fosterlink.fosterlinkbackend.service.TokenAuthService.VERIFY_EMAIL_ENDPOINT,
                        dbEntity.getId(), dbEntity.getId(), "verify_email_" + dbEntity.getEmail() + "_" + dbEntity.getId());
                userMailService.sendVerificationEmail(dbEntity.getId(), dbEntity.getEmail(), dbEntity.getFirstName(), verifyToken, unsubscribeToken);
            }

            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (Exception e) {
            log.error("Authentication following registration failed for user ID {}", dbEntity.getId());
            return ResponseEntity.status(500).build();
        }

    }
    @Operation(
            summary = "Login as a user using an email and password",
            description = "Authenticates a user and returns a JWT token. Rate limit: 5 requests per 60 seconds per IP, with burst limit of 2 requests per 10 seconds.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully logged in with correct credentials",
                            content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string", name = "token", description = "The JWT of the logged in user"))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Either the email or password that you tried to login with was incorrect"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The email could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 5 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 5, burstRequests = 2, burstDurationSeconds = 10)
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginModelEmail model, HttpServletResponse response) {
        try {
            String jwt = loginUser(model.getEmail(), model.getPassword());
            UserEntity user = userRepository.findByEmail(model.getEmail());
            if (user == null) return ResponseEntity.status(404).build();
            boolean longLived = Boolean.TRUE.equals(model.getStayLoggedIn());
            String refreshToken = refreshTokenService.createRefreshToken(user, longLived);
            setRefreshCookie(response, refreshToken, longLived);
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (DisabledException | LockedException e) {
            return ResponseEntity.status(403).body(Map.of("reason", "banned"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }

    }

    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token cookie for a new access token. The refresh token is rotated on every call. Rate limit: 30 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "New access token issued",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(type = "string", name = "token", description = "New access JWT"))),
                    @ApiResponse(responseCode = "401", description = "Refresh token missing, invalid, expired, or revoked")
            }
    )
    @RateLimit(requests = 30)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            clearRefreshCookie(response);
            return ResponseEntity.status(401).build();
        }

        return refreshTokenService.validateAndRotate(rawToken)
                .<ResponseEntity<?>>map(result -> {
                    String newAccessJwt = jwtTokenProvider.generateTokenForUsername(result.user().getEmail(), result.user().getAuthTokenVersion());
                    setRefreshCookie(response, result.newPlainToken(), result.longLived());
                    return ResponseEntity.ok(Map.of("token", newAccessJwt));
                })
                .orElseGet(() -> {
                    clearRefreshCookie(response);
                    return ResponseEntity.status(401).build();
                });
    }

     @Operation(
             summary = "Update specific info about a user",
             description = "Updates user profile information. If password, email, or phone number are changed, the user will be logged out. Rate limit: 10 requests per 60 seconds per user.",
             tags={"User"},
             responses = {
                     @ApiResponse(
                             responseCode = "200",
                             description = "Successfully updated the user. If the user's password, email, or phone number were updated, they will be logged out.",
                             content = @Content(mediaType = "application/json",
                             schema = @Schema(implementation = UserResponse.class))
                     ),
                     @ApiResponse(
                            responseCode = "500",
                             description = "There was an issue logging the user out, potentially because a logout request was made while the application was still processing the update."
                     ),
                     @ApiResponse(
                             responseCode = "401",
                             description = "The user making the request did not match the user being updated"
                     ),
                     @ApiResponse(
                             responseCode = "403",
                             description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                     ),
                     @ApiResponse(
                             responseCode = "429",
                             description = "Rate limit exceeded. Maximum 10 requests per 60 seconds per user."
                     )
             },
             security = {
                     @SecurityRequirement(name = "bearerAuth")
             }
     )
     @RateLimit(requests = 10, keyType = "USER")
    @DisallowRestricted
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UpdateUserModel model, HttpServletRequest req) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        boolean logout = false;
        if (user != null) {
            if (model.getFirstName() != null)  {
                user.setFirstName(model.getFirstName());
            }
            if (model.getLastName() != null)  {
                user.setLastName(model.getLastName());
            }
            if (model.getPassword() != null)  {
                user.setPassword(passwordEncoder.encode(model.getPassword()));
                logout = true;
            }
            if (model.getUsername() != null)  {
                user.setUsername(model.getUsername());
            }
            if (model.getEmail() != null)  {
                user.setEmail(model.getEmail());
                logout = true;
            }
            if (model.getPhoneNumber() != null)  {
                user.setPhoneNumber(model.getPhoneNumber());
                logout = true;
            }
            if (model.getProfilePictureUrl() != null)  {
                user.setProfilePictureUrl(model.getProfilePictureUrl());
            }
            if (model.getPassword() != null || model.getEmail() != null) {
                user.setAuthTokenVersion(user.getAuthTokenVersion() + 1);
            }
            String emailBeforeSave = user.getEmail();
            int userIdForEviction = user.getId();
            UserEntity saved = userRepository.save(user);
            banStatusService.evictUserDetails(emailBeforeSave);
            banStatusService.evictProfileMetadata(userIdForEviction);
            if (logout) {
                try {
                    manualLogout(req);
                } catch (ServletException e) {
                    log.error("Manual logout on user update failed for user ID {}", user.getId());
                    return ResponseEntity.status(500).build();
                }
            }
            // the frontend should always refresh after this request
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @Operation(
            summary = "Change the current user's password",
            description = "Verifies the user's old password and, if correct, replaces it with the new one. The user will be logged out after a successful change. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed successfully. The user has been logged out."),
                    @ApiResponse(responseCode = "401", description = "The old password provided is incorrect."),
                    @ApiResponse(responseCode = "404", description = "No user found for the current JWT."),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordModel model, HttpServletRequest req) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!passwordEncoder.matches(model.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        user.setPassword(passwordEncoder.encode(model.getNewPassword()));
        user.setAuthTokenVersion(user.getAuthTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        banStatusService.evictUserDetails(user.getEmail());
        try {
            manualLogout(req);
        } catch (ServletException e) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a user",
            description = "Deletes the user that is currently logged in. The user that makes the request will be the user that is deleted. Rate limit: 5 requests per 60 seconds per user.",
            tags={"User"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "The user was successfully deleted. They will also be logged out prior to the database deletion."
                ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "There was no user found with an email that matches the currently logged in user"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 5 requests per 60 seconds per user."
                    )
            },
            security = {
            @SecurityRequirement(name="bearerAuth")
            }
    )
    @RateLimit(requests = 5, keyType = "USER")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(HttpServletRequest req) {
            LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
            if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user != null) {
                String email = user.getEmail();
                try {
                    manualLogout(req);
                    userRepository.delete(user);
                    banStatusService.evict(user.getId(), email);
                    banStatusService.evictProfileMetadata(user.getId());
                    return ResponseEntity.ok().build();
                } catch (ServletException e) {
                    log.error("Manual logout on user delete failed for user ID {}", user.getId());
                    return ResponseEntity.status(500).build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
    }
    @Operation(
            summary = "Get account settings for the currently logged-in user",
            description = "Returns editable account fields (firstName, lastName, email, phoneNumber, username, profilePictureUrl) for the currently authenticated user. Rate limit: 30 requests per 60 seconds per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The editable account settings for the logged-in user",
                            content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserSettingsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No user found for the current JWT"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded."
                    )
            }, security = {
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @RateLimit(requests = 30, keyType = "USER")
    @GetMapping("/getSettings")
    public ResponseEntity<?> getUserSettings() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findByEmail(loggedIn.getEmail());
        if (user != null) {
            return ResponseEntity.ok(new UserSettingsResponse(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Get all personal data held for the current user",
            description = "Returns a full aggregation of personal data for GDPR right of access (RIGHTS-01): " +
                    "profile fields, authored threads, agencies, email preferences, mailing list memberships, " +
                    "consent records, and account deletion request status. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Personal data for the current user",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDataExportResponse.class))),
                    @ApiResponse(responseCode = "404", description = "No user found for the current JWT"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 5, keyType = "USER")
    @GetMapping("/my-data")
    public ResponseEntity<?> getMyData() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        int userId = user.getId();
        List<ThreadEntity> threads = threadRepository.findAllByPostedByIdWithRelations(userId);
        List<AgencyEntity> agencies = agencyRepository.findByAgentId(userId);
        List<Integer> disabledEmailTypeIds = dontSendEmailRepository.findAllByUserId(userId)
                .stream().map(DontSendEmailEntity::getEmailTypeId).toList();
        List<Integer> mailingListIds = mailingListMemberRepository.findMailingListIdsByUserId(userId);
        AccountDeletionRequestEntity deletionRequest =
                accountDeletionRequestRepository.findPendingByUserId(userId).orElse(null);
        List<ConsentRecordEntity> consentRecords = consentRecordService.findByUserId(userId);

        return ResponseEntity.ok(UserDataExportResponse.from(
                user, threads, agencies, disabledEmailTypeIds, mailingListIds,
                deletionRequest, consentRecords));
    }

    @Operation(
            summary = "Download all personal data as a JSON file",
            description = "Returns the same payload as /my-data as a downloadable JSON file for GDPR data portability (RIGHTS-04). " +
                    "Rate limit: 1 download per 24 hours per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Personal data JSON file"),
                    @ApiResponse(responseCode = "404", description = "No user found for the current JWT"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded — only 1 export per 24 hours per user.")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 1, durationSeconds = 86400, keyType = "USER")
    @GetMapping("/export-data")
    public ResponseEntity<?> exportData() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        int userId = user.getId();
        List<ThreadEntity> threads = threadRepository.findAllByPostedByIdWithRelations(userId);
        List<AgencyEntity> agencies = agencyRepository.findByAgentId(userId);
        List<Integer> disabledEmailTypeIds = dontSendEmailRepository.findAllByUserId(userId)
                .stream().map(DontSendEmailEntity::getEmailTypeId).toList();
        List<Integer> mailingListIds = mailingListMemberRepository.findMailingListIdsByUserId(userId);
        AccountDeletionRequestEntity deletionRequest =
                accountDeletionRequestRepository.findPendingByUserId(userId).orElse(null);
        List<ConsentRecordEntity> consentRecords = consentRecordService.findByUserId(userId);

        UserDataExportResponse exportData = UserDataExportResponse.from(
                user, threads, agencies, disabledEmailTypeIds, mailingListIds,
                deletionRequest, consentRecords);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"fosterlink-data-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(exportData);
    }

    @Operation(
            summary = "Resend email verification",
            description = "Sends a new verification email to the currently logged-in user's email address. No-op if the email is already verified. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification email sent (or already verified)."),
                    @ApiResponse(responseCode = "404", description = "Current user not found."),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            }, security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @RateLimit(requests = 5, keyType = "USER")
    @PostMapping("/resendVerificationEmail")
    public ResponseEntity<?> resendVerificationEmail() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (user.isEmailVerified()) {
            return ResponseEntity.ok().build();
        }
        if (userMailService != null) {
            String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(user);
            String verifyToken = tokenAuthService.generateToken(
                    TokenAuthService.VERIFY_EMAIL_ENDPOINT,
                    user.getId(), user.getId(), "verify_email_" + user.getEmail() + "_" + user.getId());
            userMailService.sendVerificationEmail(user.getId(), user.getEmail(), user.getFirstName(), verifyToken, unsubscribeToken);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Request a password reset email",
            description = "Sends a password reset link to the provided email address if it matches a registered account. " +
                    "Always returns 200 regardless of whether the email exists to prevent user enumeration. " +
                    "Rate limit: 3 requests per 60 seconds per IP, with burst limit of 1 request per 30 seconds.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request processed. If the email matches a registered account, a reset link has been sent."),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            }
    )
    @RateLimit(requests = 3, burstRequests = 1, burstDurationSeconds = 30)
    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordModel model) {
        UserEntity user = userRepository.findByEmail(model.getEmail());
        if (user != null && !user.isAccountDeleted()) {
            String processId = "reset_password_" + user.getId() + "_" + System.currentTimeMillis();
            String resetToken = tokenAuthService.generateToken(
                    TokenAuthService.RESET_PASSWORD_ENDPOINT, user.getId(), user.getId(), processId);
            if (userMailService != null) {
                String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(user);
                userMailService.sendPasswordResetEmail(user.getId(), user.getEmail(), user.getFirstName(), resetToken, unsubscribeToken);
            }
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Reset a user's password via token",
            description = "Resets the password for the user identified by userId using a single-use token from a password reset email. " +
                    "All existing sessions (refresh tokens) are invalidated on success. " +
                    "Rate limit: 5 requests per 60 seconds per IP.",
            tags = {"User"},
            parameters = {
                    @Parameter(name = "token", description = "Raw reset token from the password reset email link", required = true),
                    @Parameter(name = "userId", description = "ID of the user resetting their password", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset successfully. All sessions have been invalidated."),
                    @ApiResponse(responseCode = "403", description = "The reset token is invalid, expired, or has already been used."),
                    @ApiResponse(responseCode = "404", description = "User not found."),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            }
    )
    @RateLimit(requests = 5)
    @PostMapping("/resetPassword")
    @TokenAuth(endpointName = "/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam int userId,
                                           @Valid @RequestBody ResetPasswordModel model) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserEntity user = userOpt.get();
        user.setPassword(passwordEncoder.encode(model.getNewPassword()));
        user.setAuthTokenVersion(user.getAuthTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
        banStatusService.evictUserDetails(user.getEmail());
        banStatusService.evictProfileMetadata(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get the information of the currently logged in user",
            description = "Returns the profile information of the currently authenticated user. Rate limit: 60 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The information of the currently logged in user",
                            content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 60 requests per 60 seconds per IP."
                    )
            }, security = {
                    @SecurityRequirement(name="bearerAuth")
            }
    )
    @RateLimit(requests = 60)
    @GetMapping("/getInfo")
    public ResponseEntity<?> getUserInfo() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user != null) {
            return ResponseEntity.ok(new UserResponse(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Get user privileges",
            description = "Returns the privileges (administrator, FAQ author, agent) of the currently logged-in user. Returns all false if not logged in. Rate limit: 60 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The privileges of the currently logged-in user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PrivilegesResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 60 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 60)
    @GetMapping("/privileges")
    public ResponseEntity<?> getPrivileges() {
        PrivilegesResponse res = new PrivilegesResponse(false, false, false);
        if (JwtUtil.isLoggedIn()) {
            res.setAdmin(JwtUtil.hasAuthority("ADMINISTRATOR"));
            res.setFaqAuthor(JwtUtil.hasAuthority("FAQ_AUTHOR"));
            res.setAgent(JwtUtil.hasAuthority("AGENCY_REP"));
        }
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "Check if user is administrator",
            description = "Returns whether the currently logged-in user is an administrator. Returns false if not logged in. Rate limit: 60 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Whether the user is an administrator",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean", description = "true if the user is an administrator, false otherwise")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 60 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 60)
    @GetMapping("/isAdmin")
    public ResponseEntity<?> isUserAdmin() {
        return ResponseEntity.ok(JwtUtil.isLoggedIn() && JwtUtil.hasAuthority("ADMINISTRATOR"));
    }
    @Operation(
            summary = "Check if user is FAQ author",
            description = "Returns whether the currently logged-in user is an FAQ author. Returns false if not logged in. Rate limit: 60 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Whether the user is an FAQ author",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean", description = "true if the user is an FAQ author, false otherwise")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 60 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 60)
    @GetMapping("/isFaqAuthor")
    public ResponseEntity<?> isUserFaqAuthor() {
        return ResponseEntity.ok(JwtUtil.isLoggedIn() && JwtUtil.hasAuthority("FAQ_AUTHOR"));
    }
    @Operation(
            summary = "Get agent information",
            description = "Retrieves contact information (email and phone number) for a verified agency representative by their user ID. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"User"},
            parameters = {
                    @Parameter(name = "userId", description = "The internal ID of the user", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The agent information",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AgentInfoResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The user with the provided ID is not a verified agency representative"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The user with the provided ID could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/agentInfo")
    public ResponseEntity<?> getAgentInfo(@RequestParam("userId")int userId) {
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isPresent()) {
            if (user.get().isVerifiedAgencyRep()) {
                return ResponseEntity.ok(new AgentInfoResponse(user.get().getId(), user.get().getEmail(), user.get().getPhoneNumber()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @Operation(
            summary = "Logout the current user",
            description = "Logs out the currently logged-in user by clearing the security context and invalidating the session. Rate limit: 15 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The user was successfully logged out"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 15)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request);
        if (rawToken != null) {
            refreshTokenService.revokeByRawToken(rawToken);
        }
        clearRefreshCookie(response);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Log out from all devices",
            description = "Revokes all refresh tokens for the currently authenticated user, logging them out on every device. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "All sessions revoked"),
                    @ApiResponse(responseCode = "401", description = "Not authenticated")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 5, keyType = "USER")
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user != null) {
            user.setAuthTokenVersion(user.getAuthTokenVersion() + 1);
            userRepository.save(user);
            banStatusService.evictUserDetails(user.getEmail());
        }
        refreshTokenService.revokeAllForUser(loggedIn.getDatabaseId());
        clearRefreshCookie(response);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get profile metadata for a user",
            description = "Retrieves profile summary for a user by ID (thread count, FAQ count, agency info, privileges). Does not require authentication. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"User"},
            parameters = {
                    @Parameter(name = "userId", description = "The internal ID of the user", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The profile metadata for the user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProfileMetadataResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The user with the provided ID could not be found, or the user has no profile data"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/profileMetadata")
    public ResponseEntity<?> getProfileMetadata(@RequestParam int userId) {

        Optional<UserEntity> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserEntity targetUser = targetUserOpt.get();

        // Fully anonymized accounts (username set to "deleted_account_*" by anonymizeUser())
        // have no meaningful profile — return 404 for everyone, including admins.
        if (targetUser.getUsername() != null && targetUser.getUsername().startsWith("deleted_account_")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Accounts pending deletion are visible only to the owner and administrators.
        if (targetUser.isAccountDeleted()) {
            LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
            boolean isOwner = loggedIn != null && loggedIn.getDatabaseId() == userId;
            boolean isAdmin = JwtUtil.hasAuthority("ADMINISTRATOR");
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        ProfileMetadataResponse res = userMapper.mapProfileMetadataResponse(userId);
        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "Ban a user",
            description = "Bans a user by setting their banned_at timestamp. Requires administrator privileges.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully banned"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @AuditLog(action = "banned user", targetUserIdIndex = 0)
    @PostMapping("/ban")
    public ResponseEntity<?> banUser(@RequestParam int userId) {
        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        target.setBannedAt(new Date());
        userRepository.save(target);
        banStatusService.evict(target.getId(), target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());
        if (homeMailService != null) {
            homeMailService.sendAccountBannedNotification(target.getId(), target.getEmail(), target.getFirstName());
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Unban a user",
            description = "Unbans a user by clearing their banned_at timestamp. Requires administrator privileges.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully unbanned"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @AuditLog(action = "unbanned user", targetUserIdIndex = 0)
    @PostMapping("/unban")
    public ResponseEntity<?> unbanUser(@RequestParam int userId) {
        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        target.setBannedAt(null);
        userRepository.save(target);
        banStatusService.evict(target.getId(), target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());
        if (homeMailService != null) {
            String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(target);
            homeMailService.sendAccountUnbannedNotification(target.getId(), target.getEmail(), target.getFirstName(), unsubToken);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Restrict a user",
            description = "Restricts a user by setting their restricted_at timestamp. Optionally set restricted_until for a temporary restriction. Requires administrator privileges.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully restricted"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @AuditLog(action = "restricted user", targetUserIdIndex = 0)
    @PostMapping("/restrict")
    public ResponseEntity<?> restrictUser(@RequestParam int userId, @RequestParam(required = false) String restrictedUntil) {
        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        target.setRestrictedAt(new Date());
        if (restrictedUntil != null) {
            try {
                target.setRestrictedUntil(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(restrictedUntil));
            } catch (java.text.ParseException ignored) {}
        }
        userRepository.save(target);
        banStatusService.evict(target.getId(), target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());
        if (homeMailService != null) {
            String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(target);
            homeMailService.sendAccountRestrictedNotification(target.getId(), target.getEmail(), target.getFirstName(), target.getRestrictedUntil(), unsubToken);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Unrestrict a user",
            description = "Unrestricts a user by clearing their restricted_at and restricted_until timestamps. Requires administrator privileges.",
            tags = {"User"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully unrestricted"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @AuditLog(action = "unrestricted user", targetUserIdIndex = 0)
    @PostMapping("/unrestrict")
    public ResponseEntity<?> unrestrictUser(@RequestParam int userId) {
        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        target.setRestrictedAt(null);
        target.setRestrictedUntil(null);
        userRepository.save(target);
        banStatusService.evict(target.getId(), target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());
        if (homeMailService != null) {
            String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(target);
            homeMailService.sendAccountUnrestrictedNotification(target.getId(), target.getEmail(), target.getFirstName(), unsubToken);
        }
        return ResponseEntity.ok().build();
    }

    private String loginUser(String username, String password) throws BadCredentialsException {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,password
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        return jwtTokenProvider.generateToken(auth);
    }

    private void manualLogout(HttpServletRequest request) throws ServletException {
        request.logout();
    }

    private void setRefreshCookie(HttpServletResponse response, String plainToken, boolean longLived) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(REFRESH_TOKEN_COOKIE).append("=").append(plainToken).append("; ");
        cookie.append("HttpOnly; Secure; SameSite=Strict; Path=/v1/users/; ");
        if (longLived) {
            cookie.append("Max-Age=").append(refreshTokenExpirationLongMs / 1000L);
        } else {
            // Session cookie: omit Max-Age so it expires when the browser closes
            cookie.append("Max-Age=").append(refreshTokenExpirationMs / 1000L);
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                REFRESH_TOKEN_COOKIE + "=; HttpOnly; Secure; SameSite=Strict; Path=/v1/users/; Max-Age=0");
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static boolean hasMxRecord(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            return attrs.get("MX") != null && attrs.get("MX").size() > 0;
        } catch (NamingException e) {
            return false;
        }
    }

}
