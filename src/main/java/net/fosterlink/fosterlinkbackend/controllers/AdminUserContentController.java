package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.models.rest.*;
import net.fosterlink.fosterlinkbackend.models.rest.admin.*;
import net.fosterlink.fosterlinkbackend.repositories.*;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
import net.fosterlink.fosterlinkbackend.repositories.mappers.FaqMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Admin-only endpoints to fetch all entities (including hidden, pending, etc.) for a specific user.
 * Returned data includes visibility/status so admins can discern hidden vs pending without changing entity models.
 */
@RestController
@RequestMapping("/v1/admin/users/{userId}")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
public class AdminUserContentController {

    @Autowired private UserRepository userRepository;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private ThreadReplyRepository threadReplyRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private FAQRepository faqRepository;
    @Autowired private FAQApprovalRepository faqApprovalRepository;
    @Autowired private FAQRequestRepository faqRequestRepository;
    @Autowired private AgencyMapper agencyMapper;
    @Autowired private FaqMapper faqMapper;

    @Operation(
            summary = "Get all threads for a user (admin only)",
            description = "Returns all threads posted by the user, including hidden and user-deleted. Visibility state is included (hidden, userDeleted, locked, verified, hiddenBy).",
            tags = {"Admin"},
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of threads with visibility state"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/threads")
    public ResponseEntity<?> getThreadsForUser(@PathVariable int userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ThreadEntity> threads = threadRepository.findAllByPostedByIdWithRelations(userId);
        List<AdminThreadForUserResponse> result = new ArrayList<>();
        for (ThreadEntity t : threads) {
            AdminThreadForUserResponse r = new AdminThreadForUserResponse();
            r.setId(t.getId());
            r.setTitle(t.getTitle());
            r.setContent(t.getContent());
            r.setCreatedAt(t.getCreatedAt());
            r.setUpdatedAt(t.getUpdatedAt());
            r.setAuthor(new UserResponse(t.getPostedBy()));
            PostMetadataEntity pm = t.getPostMetadata();
            r.setHidden(pm.isHidden());
            r.setUserDeleted(pm.isUser_deleted());
            r.setLocked(pm.isLocked());
            r.setVerified(pm.isVerified());
            r.setHiddenBy(pm.getHidden_by());
            result.add(r);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get all replies for a user (admin only)",
            description = "Returns all replies posted by the user, including hidden. Post metadata (hidden, userDeleted, locked, verified, hiddenBy) is included.",
            tags = {"Admin"},
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of replies with visibility state"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/replies")
    public ResponseEntity<?> getRepliesForUser(@PathVariable int userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ThreadReplyEntity> replies = threadReplyRepository.findAllByPostedByIdWithRelations(userId);
        List<Integer> threadIds = replies.stream().map(ThreadReplyEntity::getThread_id).distinct().toList();
        Map<Integer, String> threadTitleById = new java.util.HashMap<>();
        Map<Integer, String> threadAuthorUsernameById = new java.util.HashMap<>();
        for (Integer tid : threadIds) {
            threadRepository.findByIdWithRelations(tid).ifPresent(te -> {
                threadTitleById.put(te.getId(), te.getTitle());
                threadAuthorUsernameById.put(te.getId(), te.getPostedBy().getUsername());
            });
        }
        List<AdminReplyForUserResponse> result = new ArrayList<>();
        for (ThreadReplyEntity tr : replies) {
            AdminReplyForUserResponse r = new AdminReplyForUserResponse();
            r.setId(tr.getId());
            r.setContent(tr.getContent());
            r.setCreatedAt(tr.getCreatedAt());
            r.setUpdatedAt(tr.getUpdatedAt());
            r.setThreadId(tr.getThread_id());
            r.setThreadTitle(threadTitleById.get(tr.getThread_id()));
            r.setThreadAuthorUsername(threadAuthorUsernameById.get(tr.getThread_id()));
            r.setAuthor(new UserResponse(tr.getPostedBy()));
            PostMetadataEntity pm = tr.getMetadata();
            r.setPostMetadata(new PostMetadataResponse(
                    pm.getId(),
                    pm.isHidden(),
                    pm.isUser_deleted(),
                    pm.isLocked(),
                    pm.isVerified(),
                    pm.getHidden_by()
            ));
            result.add(r);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get all agencies for a user (admin only)",
            description = "Returns all agencies where the user is the agent, including pending, denied, and hidden. entityStatus: PENDING, APPROVED, DENIED, or HIDDEN.",
            tags = {"Admin"},
            parameters = @Parameter(name = "userId", description = "Target user ID (agent)", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of agencies with status"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/agencies")
    public ResponseEntity<?> getAgenciesForUser(@PathVariable int userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AgencyEntity> agencies = agencyRepository.findByAgentIdWithRelations(userId);
        List<AdminAgencyForUserResponse> result = new ArrayList<>();
        for (AgencyEntity e : agencies) {
            AdminAgencyForUserResponse r = new AdminAgencyForUserResponse();
            r.setAgency(agencyMapper.fromEntity(e));
            r.setHidden(e.isHidden());
            if (e.isHidden()) {
                r.setEntityStatus("HIDDEN");
            } else if (e.getApproved() == null) {
                r.setEntityStatus("PENDING");
            } else if (e.getApproved()) {
                r.setEntityStatus("APPROVED");
            } else {
                r.setEntityStatus("DENIED");
            }
            result.add(r);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get all FAQ answers for a user (admin only)",
            description = "Returns all FAQs authored by the user, including pending, denied, and hidden. entityStatus: PENDING, APPROVED, DENIED, or HIDDEN.",
            tags = {"Admin"},
            parameters = @Parameter(name = "userId", description = "Target user ID (author)", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of FAQs with status"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/faq-answers")
    public ResponseEntity<?> getFaqAnswersForUser(@PathVariable int userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<FaqEntity> faqs = faqRepository.findByAuthor_IdOrderByCreatedAtDesc(userId);
        List<AdminFaqForUserResponse> result = new ArrayList<>();
        for (FaqEntity faq : faqs) {
            FaqResponse faqResponse = faqMapper.mapNewFaq(faq);
            Optional<FAQApprovalEntity> approvalOpt = faqApprovalRepository.findFAQApprovalEntityByFaqId(faq.getId());
            if (approvalOpt.isPresent()) {
                FAQApprovalEntity approval = approvalOpt.get();
                faqResponse.setApproved(approval.isApproved());
                faqResponse.setApprovedByUsername(null);
            }
            AdminFaqForUserResponse r = new AdminFaqForUserResponse();
            r.setFaq(faqResponse);
            boolean hidden = approvalOpt.map(a -> a.getHiddenBy() != null).orElse(false);
            boolean hiddenByAuthor = approvalOpt.map(FAQApprovalEntity::isHiddenByAuthor).orElse(false);
            r.setHidden(hidden);
            r.setHiddenByAuthor(hiddenByAuthor);
            if (hidden) {
                r.setEntityStatus("HIDDEN");
            } else if (approvalOpt.isEmpty()) {
                r.setEntityStatus("PENDING");
            } else if (approvalOpt.get().isApproved()) {
                r.setEntityStatus("APPROVED");
            } else {
                r.setEntityStatus("DENIED");
            }
            result.add(r);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get all FAQ suggestions for a user (admin only)",
            description = "Returns all FAQ suggestion requests submitted by the user. Includes createdAt.",
            tags = {"Admin"},
            parameters = @Parameter(name = "userId", description = "Target user ID (requester)", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of FAQ suggestions"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            },
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @RateLimit(requests = 30, keyType = "USER")
    @DisallowRestricted
    @GetMapping("/faq-suggestions")
    public ResponseEntity<?> getFaqSuggestionsForUser(@PathVariable int userId) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String suggestingUsername = userRepository.findById(userId)
                .map(UserEntity::getUsername)
                .orElse("");
        List<FAQRequestEntity> requests = faqRequestRepository.findByRequestedByIdOrderByCreatedAtDesc(userId);
        List<AdminFaqSuggestionForUserResponse> result = new ArrayList<>();
        for (FAQRequestEntity req : requests) {
            AdminFaqSuggestionForUserResponse r = new AdminFaqSuggestionForUserResponse();
            r.setId(req.getId());
            r.setSuggestion(req.getSuggestedTopic());
            r.setSuggestingUsername(suggestingUsername);
            r.setCreatedAt(req.getCreatedAt());
            result.add(r);
        }
        return ResponseEntity.ok(result);
    }
}
