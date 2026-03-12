package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.AccountDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AccountDeletionRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.models.web.accountdeletion.DelayDeletionModel;
import net.fosterlink.fosterlinkbackend.repositories.AccountDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AccountDeletionRequestMapper;
import net.fosterlink.fosterlinkbackend.service.AccountDeletionService;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/v1/account-deletion/")
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AccountDeletionRequestMapper deletionRequestMapper;
    private final UserRepository userRepository;

    public AccountDeletionController(
            AccountDeletionService accountDeletionService,
            AccountDeletionRequestRepository deletionRequestRepository,
            AccountDeletionRequestMapper deletionRequestMapper,
            UserRepository userRepository) {
        this.accountDeletionService = accountDeletionService;
        this.deletionRequestRepository = deletionRequestRepository;
        this.deletionRequestMapper = deletionRequestMapper;
        this.userRepository = userRepository;
    }

    @Operation(
            summary = "Request account deletion",
            description = "Creates a deletion request for the currently logged-in user's account. The account is locked immediately. The request will be automatically approved after 30 days if not acted on by an administrator. Rate limit: 3 requests per 60 seconds per user.",
            tags = {"Account Deletion"},
            parameters = {
                    @Parameter(name = "clearAccount", description = "If true, all content associated with the account (threads, replies, FAQs, agencies, etc.) will also be deleted upon approval.", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deletion request created successfully"),
                    @ApiResponse(responseCode = "400", description = "User already has a pending deletion request"),
                    @ApiResponse(responseCode = "403", description = "Not logged in"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 3, keyType = "USER")
    @PostMapping("/request")
    @Transactional
    public ResponseEntity<?> requestDeletion(@RequestParam boolean clearAccount) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        Optional<AccountDeletionRequestEntity> existing = deletionRequestRepository.findPendingByUserId(user.getId());
        if (existing.isPresent()) return ResponseEntity.status(400).build();

        accountDeletionService.requestDeletion(user, clearAccount);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Cancel own deletion request",
            description = "Cancels the currently logged-in user's pending account deletion request. Only the account owner may cancel their own request. Rate limit: 10 requests per 60 seconds per user.",
            tags = {"Account Deletion"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deletion request cancelled"),
                    @ApiResponse(responseCode = "403", description = "Not logged in"),
                    @ApiResponse(responseCode = "404", description = "No pending deletion request found for this user"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, keyType = "USER")
    @DeleteMapping("/cancel")
    @Transactional
    public ResponseEntity<?> cancelDeletion() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        Optional<AccountDeletionRequestEntity> requestOpt = deletionRequestRepository.findPendingByUserId(user.getId());
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        accountDeletionService.cancelDeletion(requestOpt.get());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get own pending deletion request",
            description = "Returns the currently logged-in user's pending deletion request, if any. Rate limit: 30 requests per 60 seconds per user.",
            tags = {"Account Deletion"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The user's pending deletion request, or null if none exists"
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 30, keyType = "USER")
    @GetMapping("/my-request")
    public ResponseEntity<?> getMyRequest() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        Optional<AccountDeletionRequestEntity> requestOpt = deletionRequestRepository.findPendingByUserId(user.getId());
        if (requestOpt.isEmpty()) return ResponseEntity.ok(null);

        AccountDeletionRequestResponse response = getAccountDeletionRequestResponse(requestOpt);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Get all pending account deletion requests",
            description = "Retrieves a paginated list of all pending account deletion requests. Only accessible to administrators. Rate limit: default per IP.",
            tags = {"Account Deletion", "Admin"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true),
                    @Parameter(name = "sortBy", description = "Sort order: 'recency' (newest request first) or 'urgency' (soonest auto-approval first)", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of pending account deletion requests",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedResponse.class))
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @GetMapping("/requests")
    public ResponseEntity<?> getDeletionRequests(
            @RequestParam int pageNumber,
            @RequestParam(defaultValue = "recency") String sortBy) {
        int totalCount = deletionRequestRepository.countPending();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;

        var requests = "urgency".equalsIgnoreCase(sortBy)
                ? deletionRequestMapper.getAllPendingByUrgency(pageNumber)
                : deletionRequestMapper.getAllPendingByRecency(pageNumber);

        return ResponseEntity.ok(new PaginatedResponse<>(requests, totalPages));
    }

    @Operation(
            summary = "Approve an account deletion request",
            description = "Approves a pending account deletion request. The account will be anonymized and (if clearAccount was set) all content will be deleted. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Account Deletion", "Admin"},
            parameters = {
                    @Parameter(name = "requestId", description = "The internal ID of the deletion request to approve", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request approved and account deletion executed"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Deletion request not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/approve")
    @Transactional
    public ResponseEntity<?> approveDeletion(@RequestParam int requestId) {
        LoggedInUser loggedInAdmin = JwtUtil.getLoggedInUser();
        UserEntity admin = userRepository.findById(loggedInAdmin.getDatabaseId()).orElse(null);
        if (admin == null) return ResponseEntity.status(403).build();

        Optional<AccountDeletionRequestEntity> requestOpt = deletionRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        accountDeletionService.approveDeletion(requestOpt.get(), admin);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delay an account deletion request",
            description = "Delays an account deletion request by 30 days and records the administrator's reason. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Account Deletion", "Admin"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Deletion request delayed by 30 days"),
                    @ApiResponse(responseCode = "400", description = "Validation error in request body"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "Deletion request not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/delay")
    @Transactional
    public ResponseEntity<?> delayDeletion(@Valid @RequestBody DelayDeletionModel model) {
        LoggedInUser loggedInAdmin = JwtUtil.getLoggedInUser();
        UserEntity admin = userRepository.findById(loggedInAdmin.getDatabaseId()).orElse(null);
        if (admin == null) return ResponseEntity.status(403).build();

        Optional<AccountDeletionRequestEntity> requestOpt = deletionRequestRepository.findById(model.getRequestId());
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        accountDeletionService.delayDeletion(requestOpt.get(), admin, model.getReason());
        return ResponseEntity.ok().build();
    }
    @NotNull
    private static AccountDeletionRequestResponse getAccountDeletionRequestResponse(Optional<AccountDeletionRequestEntity> requestOpt) {
        AccountDeletionRequestEntity req = requestOpt.get();
        AccountDeletionRequestResponse response = new AccountDeletionRequestResponse();
        response.setId(req.getId());
        response.setRequestedAt(req.getRequestedAt());
        response.setAutoApproveBy(req.getAutoApproveBy());
        response.setReviewedAt(req.getReviewedAt());
        response.setAutoApproved(req.isAutoApproved());
        response.setApproved(req.isApproved());
        response.setDelayNote(req.getDelayNote());
        response.setClearAccount(req.isClearAccount());
        response.setRequestedBy(null);
        response.setReviewedBy(null);
        return response;
    }
}
