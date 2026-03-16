package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.entities.FAQRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AnswerFaqSuggestionResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import net.fosterlink.fosterlinkbackend.models.web.faq.CreateFaqSuggestionModel;
import net.fosterlink.fosterlinkbackend.models.rest.FaqRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PendingFaqResponse;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.models.web.faq.ApproveFaqModel;
import net.fosterlink.fosterlinkbackend.models.web.faq.CreateFaqModel;
import net.fosterlink.fosterlinkbackend.models.web.faq.UpdateFaqModel;
import net.fosterlink.fosterlinkbackend.models.web.HiddenByType;
import net.fosterlink.fosterlinkbackend.mail.service.AdminUserContentMailService;
import net.fosterlink.fosterlinkbackend.mail.service.FaqMailService;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.service.AuditLogService;
import net.fosterlink.fosterlinkbackend.repositories.mappers.FaqMapper;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * REST API for FAQ management: list approved/pending FAQs, create, approve/deny, delete, and FAQ suggestion requests.
 * Base path: /v1/faq/
 */
@RestController
@RequestMapping("/v1/faq/")
public class FaqController {

    private static final Logger log = LoggerFactory.getLogger(FaqController.class);

    private @Autowired FAQRepository fAQRepository;
    private @Autowired FaqMapper faqMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FAQApprovalRepository fAQApprovalRepository;
    @Autowired
    private FAQRequestRepository fAQRequestRepository;
    @Autowired
    private FaqMailService faqMailService;
    @Autowired
    private AdminUserContentMailService adminUserContentMailService;
    @Autowired
    private TokenAuthService tokenAuthService;
    @Autowired
    private AuditLogService auditLogService;


    @Operation(
            summary = "Get all approved FAQs",
            description = "Retrieves a paginated list of all FAQs that have been approved by an administrator. Supports search by author full name, author username, FAQ title, and FAQ summary. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true),
                    @Parameter(name = "search", description = "Optional search term"),
                    @Parameter(name = "searchBy", description = "Category to search in: authorFullName, authorUsername, title, summary, or omit for all")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of approved FAQs",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @GetMapping("/all")
    @RateLimit
    public ResponseEntity<?> getAllFaqs(
            @RequestParam int pageNumber,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchBy) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        int totalCount = faqMapper.countApprovedWithSearch(normalizedSearch, normalizedSearchBy);
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        List<FaqResponse> faqs = faqMapper.allApprovedPreviewsWithSearch(pageNumber, normalizedSearch, normalizedSearchBy);
        return ResponseEntity.ok(new PaginatedResponse<>(faqs, totalPages));
    }
    @Operation(
            summary = "Get FAQ content by ID",
            description = "Retrieves the full content of a specific FAQ by its ID. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ"},
            parameters = {
                    @Parameter(name = "id", description = "The internal ID of the FAQ", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ content",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string", description = "The full content of the FAQ")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The FAQ with the provided ID could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/content")
    public ResponseEntity<?> getContentFor(@RequestParam int id) {
        Optional<FaqEntity> faq = fAQRepository.findById(id);
        if (faq.isEmpty()) return ResponseEntity.notFound().build();
        Optional<FAQApprovalEntity> approval = fAQApprovalRepository.findFAQApprovalEntityByFaqId(id);
        boolean approved = approval.isPresent() && approval.get().isApproved();
        boolean hidden = approval.isPresent() && approval.get().getHiddenBy() != null;
        boolean hiddenByAuthor = approval.isPresent() && approval.get().getHiddenBy() != null;

        if (approved && !hidden) {
            return ResponseEntity.ok(faq.get().getContent());
        }
        // Pending or hidden: allow if caller is admin, or (hidden by author and caller is the author)
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user != null) {
                boolean canView = user.isAdministrator()
                        || (hidden && hiddenByAuthor && faq.get().getAuthor().getId() == user.getId());
                if (canView) {
                    return ResponseEntity.ok(faq.get().getContent());
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Create a new FAQ",
            description = "Creates a new FAQ. Only accessible to FAQ authors or administrators. The FAQ will be created in a pending state and must be approved by an administrator. Rate limit: 5 requests per 60 seconds per user, with burst limit of 2 requests per 15 seconds.",
            tags = {"FAQ", "FaqAuthor"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ was successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FaqResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access this endpoint without FAQ author or administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 5 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 2, burstDurationSeconds = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAnyAuthority('FAQ_AUTHOR','ADMINISTRATOR')")
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateFaqModel createFaqModel) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        FaqEntity faqEntity = new FaqEntity();
        faqEntity.setAuthor(user);
        faqEntity.setContent(createFaqModel.getContent());
        faqEntity.setTitle(createFaqModel.getTitle());
        faqEntity.setSummary(createFaqModel.getSummary());
        faqEntity.setCreatedAt(new Date());

        FaqEntity saved = fAQRepository.save(faqEntity);
        return ResponseEntity.ok(faqMapper.mapNewFaq(saved));
    }

    @Operation(
            summary = "Get all pending FAQs",
            description = "Retrieves a paginated list of all FAQs that are pending approval. Only accessible to administrators. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ", "Admin"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of pending FAQs",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingFaqs(@RequestParam int pageNumber) {
        int totalCount = fAQRepository.countPending();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(faqMapper.allPendingPreviews(pageNumber), totalPages));
    }
    @Operation(
            summary = "Check approval status for logged-in user's FAQs",
            description = "Returns the count of pending and denied FAQs created by the currently logged-in user. If not logged in, returns zero counts. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ", "Admin", "FaqAuthor"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The approval status counts for the user's FAQs",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApprovalCheckResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/checkApproval")
    public ResponseEntity<?> getUnapprovedFaqCount() {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            return ResponseEntity.ok(faqMapper.checkApprovalStatusForUser(loggedIn.getDatabaseId()));
        }
        return ResponseEntity.ok(new ApprovalCheckResponse(0, 0));
    }
    @Operation(
            summary = "Approve or deny an FAQ",
            description = "Allows an administrator to approve or deny a pending FAQ. The administrator who performs this action will be recorded as the approver. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"FAQ", "Admin"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ was successfully approved or denied"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The FAQ with the provided ID could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/approve")
    @CacheEvict(value = "faqApprovedPreviews", allEntries = true)
    public ResponseEntity<?> approveFaq(@Valid @RequestBody ApproveFaqModel faq) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        Optional<FaqEntity> faqEntity = fAQRepository.findById(faq.getId());
        if (faqEntity.isPresent()) {
            Optional<FAQApprovalEntity> approval = fAQApprovalRepository.findFAQApprovalEntityByFaqId(faq.getId());
            FAQApprovalEntity entity = approval.orElseGet(FAQApprovalEntity::new);
            entity.setApproved(faq.isApproved());
            entity.setApprovedById(loggedIn.getDatabaseId());
            entity.setFaqId(faqEntity.get().getId());
            fAQApprovalRepository.save(entity);

            UserEntity author = faqEntity.get().getAuthor();
            if (author != null) {
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(author);
                if (faq.isApproved()) {
                    faqMailService.sendFaqApproved(author.getId(), author.getEmail(), author.getFirstName(), faqEntity.get().getTitle(), unsubToken);
                } else {
                    faqMailService.sendFaqDenied(author.getId(), author.getEmail(), author.getFirstName(), faqEntity.get().getTitle(), unsubToken);
                }
            }

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @Operation(
            summary = "Get all FAQ requests",
            description = "Retrieves a list of all FAQ suggestion requests. Only accessible to FAQ authors or administrators. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ", "FaqAuthor"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all FAQ requests",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = FaqRequestResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access this endpoint without FAQ author or administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR','FAQ_AUTHOR')")
    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        return ResponseEntity.ok(faqMapper.getAllRequests());
    }
    @Operation(
            summary = "Answer/delete an FAQ request",
            description = "Deletes an FAQ suggestion request, typically after it has been addressed. Only accessible to FAQ authors or administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"FAQ", "FaqAuthor"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ request was successfully deleted"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access this endpoint without FAQ author or administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR','FAQ_AUTHOR')")
    @PostMapping("/requests/answer")
    public ResponseEntity<?> answerRequest(@Valid @RequestBody AnswerFaqSuggestionResponse model) {
        fAQRequestRepository.deleteById(model.getReqId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delete an FAQ",
            description = "Permanently deletes an FAQ and its approval record. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"FAQ", "Admin"},
            parameters = {
                    @Parameter(name = "id", description = "The internal ID of the FAQ to delete", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ was successfully deleted"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The FAQ with the provided ID could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/delete")
    @Transactional
    @CacheEvict(value = "faqApprovedPreviews", allEntries = true)
    public ResponseEntity<?> deleteFaq(@RequestParam int id) {
        if (!fAQRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fAQApprovalRepository.deleteByFaqId(id);
        fAQRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    @Operation(
            summary = "Create an FAQ suggestion request",
            description = "Creates a new FAQ suggestion request. The request will be visible to FAQ authors and administrators who can then address it. Rate limit: 10 requests per 60 seconds per IP, with burst limit of 2 requests per 15 seconds.",
            tags = {"FAQ"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The FAQ request was successfully created"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access this endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 10 requests per 60 seconds per IP."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, burstRequests = 2, burstDurationSeconds = 15)
    @DisallowRestricted
    @PostMapping("/requests/create")
    public ResponseEntity<?> createFaqRequest(@Valid @RequestBody CreateFaqSuggestionModel model) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        FAQRequestEntity faqRequestResponse = new FAQRequestEntity();
        faqRequestResponse.setSuggestedTopic(model.getSuggested());
        faqRequestResponse.setRequestedById(loggedIn.getDatabaseId());
        faqRequestResponse.setCreatedAt(new Date());
        fAQRequestRepository.save(faqRequestResponse);

        UserEntity requester = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (requester != null) {
            String requesterToken = tokenAuthService.getOrCreateUnsubscribeToken(requester);
            faqMailService.sendFaqSuggestionReceived(requester.getId(), requester.getEmail(), requester.getFirstName(), model.getSuggested(), requesterToken);
        }

        userRepository.findAllAdministrators().forEach(admin -> {
            String adminToken = tokenAuthService.getOrCreateUnsubscribeToken(admin);
            faqMailService.sendFaqSuggestionReceivedNotice(admin.getId(), admin.getEmail(), admin.getFirstName(), model.getSuggested(), adminToken);
        });

        return ResponseEntity.ok().build();
    }
    @Operation(
            summary = "Get all approved FAQs by author",
            description = "Retrieves a paginated list of approved FAQs created by a specific user. Returns 404 if the user does not exist. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ"},
            parameters = {
                    @Parameter(name = "userId", description = "The internal ID of the FAQ author", required = true),
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of approved FAQs by the user",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class)
                            )
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
    @GetMapping("/allAuthor")
    public ResponseEntity<?> getAllAuthor(@RequestParam Integer userId, @RequestParam int pageNumber) {

        Optional<UserEntity> authorOpt = userRepository.findById(userId);
        if (authorOpt.isEmpty() || authorOpt.get().isAccountDeleted()) return ResponseEntity.notFound().build();

        int totalCount = fAQRepository.countApprovedByAuthor(userId);
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        List<FaqResponse> faqs = faqMapper.allApprovedPreviewsForUser(userId, pageNumber);

        return ResponseEntity.ok(new PaginatedResponse<>(faqs, totalPages));
    }

    @Operation(
            summary = "Update an FAQ (author only)",
            description = "Updates an FAQ's title, summary, and/or content. Only the author may update their own FAQ. Saving sends the FAQ back to pending approval; an administrator must approve it again. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"FAQ", "FaqAuthor"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The FAQ was successfully updated and is now pending approval"),
                    @ApiResponse(responseCode = "400", description = "No title, summary, or content provided, or validation failed"),
                    @ApiResponse(responseCode = "403", description = "Only the author may update this FAQ"),
                    @ApiResponse(responseCode = "404", description = "The FAQ was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @CacheEvict(value = "faqApprovedPreviews", allEntries = true)
    public ResponseEntity<?> updateFaq(@Valid @RequestBody UpdateFaqModel model) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        if (model.getTitle() == null && model.getSummary() == null && model.getContent() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<FaqEntity> faqOpt = fAQRepository.findById(model.getId());
        if (faqOpt.isEmpty()) return ResponseEntity.notFound().build();

        FaqEntity faq = faqOpt.get();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        if (faq.getAuthor().getId() != user.getId()) {
            return ResponseEntity.status(403).build();
        }

        if (model.getTitle() != null) faq.setTitle(model.getTitle());
        if (model.getSummary() != null) faq.setSummary(model.getSummary());
        if (model.getContent() != null) faq.setContent(model.getContent());
        faq.setUpdatedAt(new Date());
        fAQRepository.save(faq);
        fAQApprovalRepository.deleteByFaqId(faq.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Hide or restore an FAQ",
            description = "Allows an FAQ author to hide (soft-delete) or restore their own FAQ, or an administrator to hide any FAQ and to restore only FAQs they hid (not author-hidden). Rate limit: 10 requests per 60 seconds per user.",
            tags = {"FAQ", "Admin", "FaqAuthor"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The FAQ was successfully hidden or restored"),
                    @ApiResponse(responseCode = "403", description = "The user does not have permission to perform this action"),
                    @ApiResponse(responseCode = "404", description = "The FAQ was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/hide")
    @CacheEvict(value = "faqApprovedPreviews", allEntries = true)
    public ResponseEntity<?> hideFaq(@RequestParam int faqId, @RequestParam boolean hidden,
                                     @RequestParam(required = false) Boolean markAsUserDeleted) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();
        Optional<FaqEntity> faqOpt = fAQRepository.findById(faqId);
        if (faqOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<FAQApprovalEntity> approvalOpt = fAQApprovalRepository.findFAQApprovalEntityByFaqId(faqId);
        boolean isAuthor = faqOpt.get().getAuthor().getId() == user.getId();
        boolean hiddenByAuthor = isAuthor && !user.isAdministrator();
        boolean asUserDelete = Boolean.TRUE.equals(markAsUserDeleted) && user.isAdministrator();

        FAQApprovalEntity approval;
        if (approvalOpt.isEmpty()) {
            if (!user.isAdministrator()) return ResponseEntity.status(403).build();
            approval = new FAQApprovalEntity();
            approval.setFaqId(faqId);
        } else {
            approval = approvalOpt.get();
        }

        boolean canHide = user.isAdministrator() || (hiddenByAuthor && hidden);
        boolean canRestore = !hidden && approvalOpt.isPresent() && ((approval.getHiddenBy() != null && faqOpt.get().getAuthor().getId() == user.getId()) || (approval.getHiddenBy() == null && user.isAdministrator()));
        if (canHide || canRestore) {
            if (hidden) {
                approval.setHiddenByAuthor(hiddenByAuthor || asUserDelete);
                approval.setHiddenBy(asUserDelete ? null : user.getUsername());
            } else {
                approval.setHiddenByAuthor(false);
                approval.setHiddenBy(null);
            }
            fAQApprovalRepository.save(approval);

            if (!hidden) {
                auditLogService.log("restored FAQ", faqOpt.get().getAuthor().getId());
            }
            if (hidden && !hiddenByAuthor && user.isAdministrator() && !asUserDelete) {
                UserEntity author = faqOpt.get().getAuthor();
                auditLogService.log("hid FAQ", author.getId());
                String preview = faqOpt.get().getSummary() != null ? faqOpt.get().getSummary() : faqOpt.get().getTitle();
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(author);
                adminUserContentMailService.sendContentModeratedNotification(
                        author.getId(), author.getEmail(), author.getFirstName(), "FAQ", preview, unsubToken);
            }

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(
            summary = "Get hidden FAQs",
            description = "Retrieves a paginated list of hidden FAQs filtered by who hid them. Only accessible to administrators. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ", "Admin"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of hidden FAQs",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Administrator privileges required"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/getHidden")
    public ResponseEntity<?> getHiddenFaqs(@RequestParam HiddenByType type, @RequestParam int pageNumber) {
        int totalCount;
        var faqs = type == HiddenByType.ADMIN
                ? faqMapper.allHiddenByAdminPreviews(pageNumber)
                : faqMapper.allHiddenByUserPreviews(pageNumber);
        totalCount = type == HiddenByType.ADMIN
                ? fAQRepository.countHiddenByAdmin()
                : fAQRepository.countHiddenByUser();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(faqs, totalPages));
    }

    @Operation(
            summary = "Permanently delete a hidden FAQ",
            description = "Permanently deletes a hidden FAQ and its approval record. Only accessible to administrators. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"FAQ", "Admin"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The FAQ was permanently deleted"),
                    @ApiResponse(responseCode = "403", description = "Administrator privileges required"),
                    @ApiResponse(responseCode = "404", description = "The FAQ was not found or is not hidden"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/hidden/delete")
    @Transactional
    @CacheEvict(value = "faqApprovedPreviews", allEntries = true)
    public ResponseEntity<?> deleteHiddenFaq(@RequestParam int id) {
        Optional<FaqEntity> faqOpt = fAQRepository.findById(id);
        if (faqOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<FAQApprovalEntity> approvalOpt = fAQApprovalRepository.findFAQApprovalEntityByFaqId(id);
        if (approvalOpt.isEmpty() || (approvalOpt.get().getHiddenBy() == null && !approvalOpt.get().isHiddenByAuthor())) {
            return ResponseEntity.notFound().build();
        }

        int authorId = faqOpt.get().getAuthor().getId();
        fAQApprovalRepository.deleteByFaqId(id);
        fAQRepository.deleteById(id);
        auditLogService.log("permanently deleted FAQ", authorId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delete all FAQ suggestion requests for the current user",
            description = "Deletes all faq_request rows submitted by the currently authenticated user. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"FAQ"},
            responses = {
                    @ApiResponse(responseCode = "204", description = "All FAQ requests for the user were deleted"),
                    @ApiResponse(responseCode = "403", description = "Not authenticated"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, keyType = "USER")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/my-requests")
    public ResponseEntity<?> deleteMyFaqRequests() {
        try {
            LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
            if (loggedIn == null) return ResponseEntity.status(403).build();
            fAQRequestRepository.deleteByRequestedById(loggedIn.getDatabaseId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("Failed to delete FAQ requests for user", e);
            return ResponseEntity.status(500).build();
        }
    }

}
