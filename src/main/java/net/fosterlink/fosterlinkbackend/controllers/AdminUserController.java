package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.fosterlink.fosterlinkbackend.config.audit.AuditLog;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.models.rest.AdminUserResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AdminUserStatsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AuditLogEntryResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.mail.service.AdminUserMailService;
import net.fosterlink.fosterlinkbackend.mail.service.MailingListMailService;
import net.fosterlink.fosterlinkbackend.util.UserConstants;
import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AdminUserMapper;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AuditLogMapper;
import net.fosterlink.fosterlinkbackend.service.BanStatusService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for admin-only user management: search and role assignment.
 * Base path: /v1/admin/users/
 */
@RestController
@RequestMapping("/v1/admin/users/")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
public class AdminUserController {

    private @Autowired AdminUserMapper adminUserMapper;
    private @Autowired AuditLogRepository auditLogRepository;
    private @Autowired AuditLogMapper auditLogMapper;
    
    private static final List<String> ASSIGNABLE_ROLES = List.of(
            "FAQ_AUTHOR", "VERIFIED_FOSTER", "AGENCY_REP", "ID_VERIFIED"
    );
    private static final List<String> VALID_SEARCH_BY = List.of(
            "FULL_NAME", "USERNAME", "EMAIL", "PHONE_NUMBER", "ROLE"
    );
    private static final List<String> VALID_ROLES = List.of(
            "ADMINISTRATOR", "FAQ_AUTHOR", "AGENCY_REP", "VERIFIED_FOSTER", "ID_VERIFIED"
    );

    private static final String ASSIGN_ADMIN_ENDPOINT = "/assignAdmin";
    private static final String REVOKE_ADMIN_ENDPOINT = "/revokeAdmin";

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final BanStatusService banStatusService;
    private final AdminUserMailService adminUserMailService;
    private final MailingListMailService mailingListMailService;
    private final TokenAuthService tokenAuthService;

    public AdminUserController(UserRepository userRepository, BanStatusService banStatusService,
                               AdminUserMapper adminUserMapper, AdminUserMailService adminUserMailService,
                               MailingListMailService mailingListMailService, TokenAuthService tokenAuthService) {
        this.userRepository = userRepository;
        this.banStatusService = banStatusService;
        this.adminUserMailService = adminUserMailService;
        this.mailingListMailService = mailingListMailService;
        this.tokenAuthService = tokenAuthService;
    }

    @Operation(
            summary = "Search users (admin only)",
            description = "Searches all users by a given field with pagination. Requires administrator privileges.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "searchBy", description = "Field to search by: FULL_NAME, USERNAME, EMAIL, PHONE_NUMBER, ROLE", required = true),
                    @Parameter(name = "query", description = "Search term, or a role name when searchBy=ROLE (e.g. ADMINISTRATOR, FAQ_AUTHOR, AGENCY_REP, VERIFIED_FOSTER, ID_VERIFIED)", required = true),
                    @Parameter(name = "page", description = "Zero-based page index", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of matching users",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid searchBy or query value"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String searchBy,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page) {

        if (!VALID_SEARCH_BY.contains(searchBy)) {
            return ResponseEntity.badRequest().body("Invalid searchBy value.");
        }

        if (searchBy.equals("ROLE") && !VALID_ROLES.contains(query)) {
            return ResponseEntity.badRequest().body("Invalid role value for ROLE search.");
        }

        int offset = page * SqlUtil.ITEMS_PER_PAGE;
        List<Object[]> rows;
        long totalCount;

        switch (searchBy) {
            case "USERNAME" -> {
                rows = userRepository.searchByUsername(query, SqlUtil.ITEMS_PER_PAGE, offset);
                totalCount = userRepository.countByUsername(query);
            }
            case "EMAIL" -> {
                rows = userRepository.searchByEmail(query, SqlUtil.ITEMS_PER_PAGE, offset);
                totalCount = userRepository.countByEmail(query);
            }
            case "PHONE_NUMBER" -> {
                rows = userRepository.searchByPhoneNumber(query, SqlUtil.ITEMS_PER_PAGE, offset);
                totalCount = userRepository.countByPhoneNumber(query);
            }
            case "ROLE" -> {
                rows = userRepository.searchByRole(query, SqlUtil.ITEMS_PER_PAGE, offset);
                totalCount = userRepository.countByRole(query);
            }
            default -> {
                // FULL_NAME
                rows = userRepository.searchByFullName(query, SqlUtil.ITEMS_PER_PAGE, offset);
                totalCount = userRepository.countByFullName(query);
            }
        }

        List<AdminUserResponse> users = new ArrayList<>();
        for (Object[] row : rows) {
            users.add(adminUserMapper.mapRow(row));
        }

        int totalPages = (int) Math.ceil((double) totalCount / SqlUtil.ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        return ResponseEntity.ok(new PaginatedResponse<>(users, totalPages));
    }

    @Operation(
            summary = "Get user statistics (admin only)",
            description = "Returns aggregate user counts: total users, administrators, FAQ authors, and agents. Requires administrator privileges.",
            tags = {"Admin"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "User statistics",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AdminUserStatsResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        long totalUsers = userRepository.countAll();
        long totalAdmins = userRepository.countByRole("ADMINISTRATOR");
        long totalFaqAuthors = userRepository.countByRole("FAQ_AUTHOR");
        long totalAgents = userRepository.countByRole("AGENCY_REP");
        long totalDeleted = userRepository.countDeleted();
        return ResponseEntity.ok(new AdminUserStatsResponse(totalUsers, totalAdmins, totalFaqAuthors, totalAgents, totalDeleted));
    }

    @Operation(
            summary = "List deleted accounts (admin only)",
            description = "Returns all accounts marked as deleted in a paginated view, ordered by ID. Requires administrator privileges.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "page", description = "Zero-based page index", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of deleted accounts",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/deleted")
    public ResponseEntity<?> getDeletedUsers(@RequestParam(defaultValue = "0") int page) {
        List<Object[]> rows = userRepository.findDeletedPaginated(PageRequest.of(page, SqlUtil.ITEMS_PER_PAGE));
        long totalCount = userRepository.countDeleted();

        List<AdminUserResponse> users = new ArrayList<>();
        for (Object[] row : rows) {
            users.add(adminUserMapper.mapRow(row));
        }

        int totalPages = (int) Math.ceil((double) totalCount / SqlUtil.ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        return ResponseEntity.ok(new PaginatedResponse<>(users, totalPages));
    }

    @Operation(
            summary = "List all users (admin only)",
            description = "Returns all users in a paginated view, ordered by ID. Requires administrator privileges.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "page", description = "Zero-based page index", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of all users",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page) {
        int offset = page * SqlUtil.ITEMS_PER_PAGE;
        List<Object[]> rows = userRepository.findAllPaginated(SqlUtil.ITEMS_PER_PAGE, offset);
        long totalCount = userRepository.countAll();

        List<AdminUserResponse> users = new ArrayList<>();
        for (Object[] row : rows) {
            users.add(adminUserMapper.mapRow(row));
        }

        int totalPages = (int) Math.ceil((double) totalCount / SqlUtil.ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        return ResponseEntity.ok(new PaginatedResponse<>(users, totalPages));
    }

    @Operation(
            summary = "List audit log (admin only)",
            description = "Returns all audit log entries in descending order by id, paginated. Requires administrator privileges.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "page", description = "Zero-based page index", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of audit log entries",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(@RequestParam(defaultValue = "0") int page) {
        var pageable = PageRequest.of(page, SqlUtil.ITEMS_PER_PAGE);
        var result = auditLogRepository.findAllForAdminDisplay(pageable);
        List<AuditLogEntryResponse> entries = result.getContent().stream()
                .map(auditLogMapper::mapRow)
                .toList();
        int totalPages = result.getTotalPages();
        if (totalPages == 0) totalPages = 1;
        return ResponseEntity.ok(new PaginatedResponse<>(entries, totalPages));
    }

    @Operation(
            summary = "Set a role on a user (admin only)",
            description = "Enables or disables a non-administrator role for the specified user. The ADMINISTRATOR role cannot be set via this endpoint. Requires administrator privileges.",
            tags = {"Admin"},
            parameters = {
                    @Parameter(name = "userId", description = "Target user ID", required = true),
                    @Parameter(name = "role", description = "Role to change: FAQ_AUTHOR, VERIFIED_FOSTER, AGENCY_REP, ID_VERIFIED", required = true),
                    @Parameter(name = "enabled", description = "true to grant the role, false to revoke it", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid or disallowed role (e.g. ADMINISTRATOR)"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Target user not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @AuditLog(action = "set role on user", targetUserIdIndex = 0)
    @PostMapping("/setRole")
    public ResponseEntity<?> setUserRole(
            @RequestParam int userId,
            @RequestParam String role,
            @RequestParam boolean enabled) {

        if (!ASSIGNABLE_ROLES.contains(role)) {
            return ResponseEntity.badRequest().body("Role '" + role + "' cannot be set via this endpoint.");
        }

        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        }

        if ("AGENCY_REP".equals(role) && enabled
                && (target.getPhoneNumber() == null || target.getPhoneNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "User must have a phone number before being granted the agency representative role.");
        }

        switch (role) {
            case "FAQ_AUTHOR"      -> target.setFaqAuthor(enabled);
            case "VERIFIED_FOSTER" -> target.setVerifiedFoster(enabled);
            case "AGENCY_REP"      -> target.setVerifiedAgencyRep(enabled);
            case "ID_VERIFIED"     -> target.setIdVerified(enabled);
        }

        userRepository.save(target);
        banStatusService.evictUserDetails(target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());

        String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(target);
        if (enabled) {
            adminUserMailService.sendRoleAssignedNotification(
                    target.getId(), target.getEmail(), target.getFirstName(), role, unsubscribeToken);
        } else {
            adminUserMailService.sendRoleRevokedNotification(
                    target.getId(), target.getEmail(), target.getFirstName(), role, unsubscribeToken);
        }

        return ResponseEntity.ok().build();
    }

    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/requestAdminRole")
    public ResponseEntity<?> requestAdminRole(@RequestParam int userId) {
        LoggedInUser caller = JwtUtil.getLoggedInUser();
        if (caller == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        }
        if (target.isAdministrator()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User is already an administrator.");
        }

        String processId = UUID.randomUUID().toString();
        mailingListMailService.sendToMailingList("FounderMailingList", (founder) -> {
            String rawToken = tokenAuthService.generateToken(ASSIGN_ADMIN_ENDPOINT, caller.getDatabaseId(), userId, processId);
            String founderUnsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(founder);
            adminUserMailService.sendAdminApprovalRequest(
                    founder.getEmail(),
                    founder.getFirstName(),
                    target.getUsername(),
                    userId,
                    rawToken,
                    frontendUrl,
                    founderUnsubscribeToken,
                    founder.getId()
            );
        });

        return ResponseEntity.ok().build();
    }

    @RateLimit(requests = 20, keyType = "USER")
    @DisallowRestricted
    @AuditLog(action = "clear user profile", targetUserIdIndex = 0)
    @PostMapping("/clearProfile")
    public ResponseEntity<?> clearUserProfile(
            @RequestParam int userId,
            @RequestParam(defaultValue = "false") boolean clearFullName,
            @RequestParam(defaultValue = "false") boolean clearUsername,
            @RequestParam(defaultValue = "false") boolean clearProfilePicture) {

        if (!clearFullName && !clearUsername && !clearProfilePicture) {
            return ResponseEntity.badRequest().body("At least one field must be selected for clearing.");
        }

        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        }

        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String originalFirstName = target.getFirstName();

        List<String> clearedFieldLabels = new ArrayList<>();

        if (clearFullName) {
            target.setFirstName("Anonymized");
            target.setLastName("User-" + shortId);
            clearedFieldLabels.add("Full name");
        }
        if (clearUsername) {
            target.setUsername("anonymized-user-" + shortId);
            clearedFieldLabels.add("Username");
        }
        if (clearProfilePicture) {
            target.setProfilePictureUrl(UserConstants.DEFAULT_PROFILE_PIC);
            clearedFieldLabels.add("Profile picture");
        }

        userRepository.save(target);
        banStatusService.evictUserDetails(target.getEmail());
        banStatusService.evictProfileMetadata(target.getId());

        String clearedFields = clearedFieldLabels.stream().collect(Collectors.joining(", "));
        String unsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(target);
        adminUserMailService.sendProfileClearedNotification(
                target.getId(), target.getEmail(), originalFirstName, clearedFields, unsubscribeToken);

        List<Object[]> rows = userRepository.findAdminUserById(userId);
        if (rows.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        AdminUserResponse response = adminUserMapper.mapRow(rows.get(0));
        return ResponseEntity.ok(response);
    }

    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/requestRevokeAdminRole")
    public ResponseEntity<?> requestRevokeAdminRole(@RequestParam int userId) {
        LoggedInUser caller = JwtUtil.getLoggedInUser();
        if (caller == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<UserEntity> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserEntity target = targetOpt.get();
        if (target.isAccountDeleted()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot modify a deleted account.");
        }
        if (!target.isAdministrator()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User is not an administrator.");
        }

        String processId = UUID.randomUUID().toString();
        mailingListMailService.sendToMailingList("FounderMailingList", (founder) -> {
            String rawToken = tokenAuthService.generateToken(REVOKE_ADMIN_ENDPOINT, caller.getDatabaseId(), userId, processId);
            String founderUnsubscribeToken = tokenAuthService.getOrCreateUnsubscribeToken(founder);
            adminUserMailService.sendAdminRevocationRequest(
                    founder.getEmail(),
                    founder.getFirstName(),
                    target.getUsername(),
                    userId,
                    rawToken,
                    frontendUrl,
                    founderUnsubscribeToken,
                    founder.getId()
            );
        });

        return ResponseEntity.ok().build();
    }

}
