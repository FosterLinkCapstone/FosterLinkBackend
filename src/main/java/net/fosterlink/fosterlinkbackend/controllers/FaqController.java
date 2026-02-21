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
import net.fosterlink.fosterlinkbackend.entities.FAQApprovalEntity;
import net.fosterlink.fosterlinkbackend.entities.FAQRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.FaqEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AnswerFaqSuggestionResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApprovalCheckResponse;
import net.fosterlink.fosterlinkbackend.models.web.faq.CreateFaqSuggestionModel;
import net.fosterlink.fosterlinkbackend.models.rest.FaqRequestResponse;
import net.fosterlink.fosterlinkbackend.models.rest.FaqResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetFaqsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetPendingFaqsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PendingFaqResponse;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.models.web.faq.ApproveFaqModel;
import net.fosterlink.fosterlinkbackend.models.web.faq.CreateFaqModel;
import net.fosterlink.fosterlinkbackend.repositories.FAQApprovalRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRepository;
import net.fosterlink.fosterlinkbackend.repositories.FAQRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.FaqMapper;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    private @Autowired FAQRepository fAQRepository;
    private @Autowired FaqMapper faqMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FAQApprovalRepository fAQApprovalRepository;
    @Autowired
    private FAQRequestRepository fAQRequestRepository;


    @Operation(
            summary = "Get all approved FAQs",
            description = "Retrieves a paginated list of all FAQs that have been approved by an administrator. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of approved FAQs",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GetFaqsResponse.class)
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
    public ResponseEntity<?> getAllFaqs(@RequestParam int pageNumber) {
        int totalCount = fAQRepository.countApproved();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new GetFaqsResponse(faqMapper.allApprovedPreviews(pageNumber), totalPages));
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
        if (faq.isPresent()) {
            return ResponseEntity.ok(faq.get().getContent());
        } else {
            return ResponseEntity.notFound().build();
        }
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
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateFaqModel createFaqModel) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isFaqAuthor() || user.isAdministrator()) {
                FaqEntity faqEntity = new FaqEntity();
                faqEntity.setAuthor(user);
                faqEntity.setContent(createFaqModel.getContent());
                faqEntity.setTitle(createFaqModel.getTitle());
                faqEntity.setSummary(createFaqModel.getSummary());
                faqEntity.setCreatedAt(new Date());

                FaqEntity saved = fAQRepository.save(faqEntity);
                return ResponseEntity.ok(faqMapper.mapNewFaq(saved));
            }
        }
        return ResponseEntity.status(403).build();
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
                                    schema = @Schema(implementation = GetPendingFaqsResponse.class)
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
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingFaqs(@RequestParam int pageNumber) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator()) {
                int totalCount = fAQRepository.countPending();
                int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
                return ResponseEntity.ok(new GetPendingFaqsResponse(faqMapper.allPendingPreviews(pageNumber), totalPages));
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
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
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            return ResponseEntity.ok(faqMapper.checkApprovalStatusForUser(user.getId()));
        }
        return ResponseEntity.ok(new ApprovalCheckResponse(0,0));
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
    @PostMapping("/approve")
    public ResponseEntity<?> approveFaq(@Valid @RequestBody ApproveFaqModel faq) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator()) {
                Optional<FaqEntity> faqEntity = fAQRepository.findById(faq.getId());
                if (faqEntity.isPresent()) {
                    Optional<FAQApprovalEntity> approval = fAQApprovalRepository.findFAQApprovalEntityByFaqId(faq.getId());
                    FAQApprovalEntity entity;
                    entity = approval.orElseGet(FAQApprovalEntity::new);
                    entity.setApproved(faq.isApproved());
                    entity.setApprovedById(user.getId());
                    entity.setFaqId(faqEntity.get().getId());
                    fAQApprovalRepository.save(entity);
                    return ResponseEntity.ok().build();
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
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
    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator() || user.isFaqAuthor()) {
                return ResponseEntity.ok(faqMapper.getAllRequests());
            }
        }
        return ResponseEntity.status(403).build();
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
    @PostMapping("/requests/answer")
    public ResponseEntity<?> answerRequest(@Valid @RequestBody AnswerFaqSuggestionResponse model) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user.isAdministrator() || user.isFaqAuthor()) {
                fAQRequestRepository.deleteById(model.getReqId());
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(403).build();
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
    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<?> deleteFaq(@RequestParam int id) {
        if (!JwtUtil.isLoggedIn()) {
            return ResponseEntity.status(403).build();
        }
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (!user.isAdministrator()) {
            return ResponseEntity.status(403).build();
        }
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
    @PostMapping("/requests/create")
    public ResponseEntity<?> createFaqRequest(@Valid @RequestBody CreateFaqSuggestionModel model) {
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        FAQRequestEntity faqRequestResponse = new FAQRequestEntity();
        faqRequestResponse.setSuggestedTopic(model.getSuggested());
        faqRequestResponse.setRequestedById(user.getId());
        faqRequestResponse.setCreatedAt(new Date());
        fAQRequestRepository.save(faqRequestResponse);
        return ResponseEntity.ok().build();
    }
    @Operation(
            summary = "Get all approved FAQs by author",
            description = "Retrieves all approved FAQs created by a specific user. Returns 404 if the user does not exist or has no approved FAQs. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"FAQ"},
            parameters = {
                    @Parameter(name = "userId", description = "The internal ID of the FAQ author", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of approved FAQs by the user",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = FaqResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The user with the provided ID could not be found, or the user has no approved FAQs"
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

        boolean userExists = userRepository.existsById(userId);

        if (!userExists) return ResponseEntity.notFound().build();

        List<FaqResponse> faqs = faqMapper.allApprovedPreviewsForUser(userId, pageNumber);

        if (faqs.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(faqs);
    }

}
