package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.mail.service.AdminUserContentMailService;
import net.fosterlink.fosterlinkbackend.mail.service.ThreadMailService;
import net.fosterlink.fosterlinkbackend.models.rest.HiddenThreadResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadReplyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.models.web.HiddenByType;
import net.fosterlink.fosterlinkbackend.models.web.thread.*;
import net.fosterlink.fosterlinkbackend.repositories.*;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadMapper;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadReplyMapper;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.service.AuditLogService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST API for forum threads and replies: create, update, delete, search, like, and reply to threads.
 * Base path: /v1/threads/
 */
@RestController
@RequestMapping("/v1/threads/")
public class ThreadController {

    private @Autowired ThreadRepository threadRepository;
    private @Autowired ThreadTagRepository threadTagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ThreadLikeRepository threadLikeRepository;
    @Autowired
    private ThreadMapper threadMapper;
    @Autowired
    private ThreadReplyMapper threadReplyMapper;
    @Autowired
    private ThreadReplyLikeRepository threadReplyLikeRepository;
    @Autowired
    private PostMetadataRepository postMetadataRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ThreadMailService threadMailService;
    @Autowired
    private AdminUserContentMailService adminUserContentMailService;
    @Autowired
    private TokenAuthService tokenAuthService;
    @Autowired
    private AuditLogService auditLogService;


    @Operation(
            summary = "Create a new thread",
            description = "Creates a new forum thread. Rate limit: 5 requests per 60 seconds per user, with burst limit of 2 requests per 15 seconds.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The successfully created thread",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadResponse.class)
                            )
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
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 2, burstDurationSeconds = 15, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateThreadModel model) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        UserEntity user = loggedIn != null ? userRepository.findById(loggedIn.getDatabaseId()).orElse(null) : null;

        if (user != null) {

            // TODO allow only verified foster parents to create a thread?

            PostMetadataEntity postMetadata = new PostMetadataEntity();
            postMetadata.setHidden(false);
            postMetadata.setLocked(false);
            postMetadata.setVerified(false);
            postMetadata.setUser_deleted(false);
            PostMetadataEntity saved = postMetadataRepository.save(postMetadata);

            ThreadEntity threadEntity = new ThreadEntity();
            threadEntity.setCreatedAt(new Date());
            threadEntity.setTitle(StringUtil.cleanString(model.getTitle()));
            threadEntity.setContent(StringUtil.cleanString(model.getContent()));
            threadEntity.setPostMetadata(saved);
            threadEntity.setPostedBy(user);
            ThreadEntity savedThread = threadRepository.save(threadEntity);

            Set<ThreadTagEntity> entities = new HashSet<>();
            if (model.getTags() != null) {
                Set<String> seenNormalized = new HashSet<>();
                for (String tag : model.getTags()) {
                    String normalized = normalizeTagName(tag);
                    if (!normalized.isEmpty() && seenNormalized.add(normalized)) {
                        ThreadTagEntity threadTagEntity = new ThreadTagEntity();
                        threadTagEntity.setName(normalized);
                        threadTagEntity.setThread(savedThread);
                        entities.add(threadTagEntity);
                    }
                }
            }


            threadTagRepository.saveAll(entities);

            int commentCount = threadReplyRepository.visibleReplyCountForThread(savedThread.getId());
            int userPostCount = threadRepository.visibleThreadCountForUser(user.getId());
            return ResponseEntity.ok().body(new ThreadResponse(savedThread, 0, commentCount, userPostCount));
        } else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

    }

    @Operation(
            summary = "Update a thread",
            description = "Updates an existing thread's title and/or content. Request body must include threadId; title and content are optional (omit or null to leave unchanged). Only the thread author can update their thread. Returns empty body on success. Rate limit: 10 requests per 60 seconds per user.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The thread was successfully updated. Empty response body."
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "The author of the thread did not match the logged in user"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The thread with the given ID was not found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 10 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, keyType = "USER")
    @DisallowRestricted
    @PutMapping("/update")
    public ResponseEntity<?> update(@Valid @RequestBody UpdateThreadModel model) {

        ThreadEntity thread = threadRepository.findByIdWithRelations(model.getThreadId()).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }

        if (Objects.equals(JwtUtil.getLoggedInEmail(), thread.getPostedBy().getEmail())) {

            if (model.getContent() != null) {
                thread.setContent(StringUtil.cleanString(model.getContent()));
            }
            if (model.getTitle() != null) {
                thread.setTitle(StringUtil.cleanString(model.getTitle()));
            }
            threadRepository.save(thread);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }
    @Operation(
            summary = "Search thread by ID",
            description = "Search for a thread by its internal ID. Rate limit: 30 requests per 60 seconds per IP.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name="threadId", description = "The internal ID of the thread to search for.")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A thread by that ID was found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "A thread by that ID could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 30)
    @GetMapping("/search-by-id")
    public ResponseEntity<?> searchById(@RequestParam int threadId) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        int userId = loggedIn != null ? loggedIn.getDatabaseId() : -1;
        ThreadResponse thread = threadMapper.findById(threadId, userId);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(thread);
    }

    @Operation(
            summary = "Search threads by author",
            description = "Retrieves threads created by a specific user, paginated. Returns 404 if the user does not exist. Rate limit: 30 requests per 60 seconds per IP.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name = "userId", description = "The internal ID of the thread author", required = true),
                    @Parameter(name = "pageNumber", description = "Zero-based page index", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated threads by the user",
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
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 30)
    @GetMapping("/search-by-user")
    public ResponseEntity<?> searchByUser(@RequestParam int userId, @RequestParam int pageNumber) {
        Optional<UserEntity> authorOpt = userRepository.findById(userId);
        if (authorOpt.isEmpty() || authorOpt.get().isAccountDeleted()) {
            return ResponseEntity.notFound().build();
        }

        LoggedInUser loggedInForSearch = JwtUtil.getLoggedInUser();
        int sendingUserId = loggedInForSearch != null ? loggedInForSearch.getDatabaseId() : -1;

        var threads = threadMapper.searchByUser(sendingUserId, userId, pageNumber);
        int totalCount = threadRepository.visibleThreadCountForUser(userId);
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(threads, totalPages));
    }

    @Operation(
            summary = "Delete a thread",
            description = "Delete a thread by its ID. Accessible to the user who created the thread as well as administrators. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name="threadId", description = "The internal ID of the thread to delete")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The thread was successfully deleted. Does not return anything."
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "The logged in user was not the author of the thread, or an administrator"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The provided thread ID could not be matched to any threads."
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
                    @SecurityRequirement(name = "bearerAuth")
            }

    )
    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<?> deleteById(@RequestParam int threadId) {
        ThreadEntity t =  threadRepository.findByIdWithRelations(threadId).orElse(null);

        if (t == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = JwtUtil.hasAuthority("ADMINISTRATOR");
        String email = JwtUtil.getLoggedInEmail();
        if (t.getPostedBy().getEmail().equals(email) || isAdmin) {
            t.getPostMetadata().setHidden(true);
            if (!isAdmin) {
                t.getPostMetadata().setUser_deleted(true);
                t.getPostMetadata().setDeletedAt(new Date());
            }
            threadRepository.save(t);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @Operation(
            summary = "Search threads",
            description = "Search for threads using various criteria (username, title, content, tags). Rate limit: 30 requests per 60 seconds per user.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A collection of threads matching the search criteria. Can be empty if no matches found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ThreadResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The search criteria provided was invalid"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "When searching by username, the username could not be found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per user."
                    )
            }
    )
    @RateLimit(requests = 30, keyType = "USER")
    @PostMapping("/search")
    public ResponseEntity<?> search(@Valid @RequestBody SearchThreadModel search) {
        switch (search.getSearchBy()) {
            case USERNAME:
                UserEntity user = userRepository.findByUsername(search.getSearchTerm());
                if (user == null) {
                    return ResponseEntity.notFound().build();
                }
                // Use repository method to avoid lazy loading threadsAuthored collection
                List<ThreadEntity> userThreads = threadRepository.findByPostedByAndPostMetadataHiddenFalseWithRelations(user.getId(), PageRequest.of(search.getPageNumber(), SqlUtil.ITEMS_PER_PAGE));
                return ResponseEntity.ok(toResponseModel(userThreads));
            case THREAD_TITLE:
                List<ThreadEntity> threads = threadRepository.findByTitleContaining(search.getSearchTerm(), PageRequest.of(search.getPageNumber(), SqlUtil.ITEMS_PER_PAGE));
                return ResponseEntity.ok(toResponseModel(threads));
            case THREAD_CONTENT:
                List<ThreadEntity> threads2 = threadRepository.findByContentContaining(search.getSearchTerm(), PageRequest.of(search.getPageNumber(), SqlUtil.ITEMS_PER_PAGE));
                return ResponseEntity.ok(toResponseModel(threads2));
            case TAGS:
                // Fetch threads directly from tags to avoid lazy loading
                List<Integer> threadIds = threadTagRepository.findThreadIdsByName(search.getSearchTerm());
                List<ThreadEntity> threads3 = threadIds.isEmpty()
                    ? new ArrayList<>()
                    : threadRepository.findAllByIdWithRelations(threadIds, PageRequest.of(search.getPageNumber(), SqlUtil.ITEMS_PER_PAGE));
                return ResponseEntity.ok(toResponseModel(threads3));
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(
            summary = "Get random threads",
            description = "Get random weighted threads. If a userId is provided, the threads will be weighted based on the user's preferences. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name = "userId", description = "Optional user ID to weight threads based on user preferences. If not provided or -1, returns unweighted random threads.", required = false)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A collection of random threads",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ThreadResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/rand")
    public ResponseEntity<?> rand(@RequestParam(required = false, defaultValue = "-1") int userId) {
        if (userId != -1) {
            return ResponseEntity.ok(threadMapper.findRandomWeightedThreadsForUser(userId));
        }
        return ResponseEntity.ok(threadMapper.findRandomWeightedThreads(-1));
    }
    @Operation(
            summary = "Get threads",
            description = "Get threads ordered by a specified strategy (most liked, oldest, newest), paginated. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name = "orderBy", description = "most liked | oldest | newest", required = true),
                    @Parameter(name = "pageNumber", description = "Zero-based page index", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A collection of threads with total page count",
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
    @RateLimit
    @GetMapping("/getThreads")
    public ResponseEntity<?> getThreads(
            @RequestParam String orderBy,
            @RequestParam int pageNumber
    ) {
        LoggedInUser loggedInForThreads = JwtUtil.getLoggedInUser();
        int userId = loggedInForThreads != null ? loggedInForThreads.getDatabaseId() : -1;
        var threads = threadMapper.getThreads(orderBy, userId, pageNumber);
        int totalCount = threadRepository.countVisible();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(threads, totalPages));
    }
    @Operation(
            summary = "Get replies for a thread",
            description = "Retrieves all replies for a specific thread. If a user is logged in, includes whether the user has liked each reply. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name = "threadId", description = "The internal ID of the thread", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of replies for the thread",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ThreadReplyResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 50 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit
    @GetMapping("/replies")
    public ResponseEntity<?> replies(@RequestParam int threadId) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            int uid = loggedIn.getDatabaseId();
            if (JwtUtil.hasAuthority("ADMINISTRATOR")) {
                return ResponseEntity.ok(threadReplyMapper.getAllRepliesForThreadAdmin(threadId, uid));
            }
            return ResponseEntity.ok(threadReplyMapper.getRepliesForThread(threadId, uid));
        }
        return ResponseEntity.ok(threadReplyMapper.getRepliesForThread(threadId, -1));
    }
    @Operation(
            summary = "Reply to a thread",
            description = "Creates a new reply to a thread. Only accessible to logged-in users. Rate limit: 15 requests per 60 seconds per user, with burst limit of 3 requests per 10 seconds.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The reply was successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadReplyResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, burstRequests = 3, burstDurationSeconds = 10, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/replies")
    public ResponseEntity<?> replyTo(@Valid @RequestBody ReplyToThreadModel reply) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user == null) return ResponseEntity.status(403).build();

            PostMetadataEntity postMetadata = new PostMetadataEntity();
            postMetadata.setHidden(false);
            postMetadata.setLocked(false);
            postMetadata.setVerified(false);
            postMetadata.setUser_deleted(false);

            PostMetadataEntity saved = postMetadataRepository.save(postMetadata);

            ThreadReplyEntity dbReply = new ThreadReplyEntity();
            dbReply.setContent(reply.getContent());
            dbReply.setCreatedAt(new Date());
            dbReply.setMetadata(saved);
            dbReply.setPostedBy(user);
            dbReply.setThread_id(reply.getThreadId());

            ThreadReplyEntity replyEntity = threadReplyRepository.save(dbReply);

            threadRepository.findByIdWithRelations(reply.getThreadId()).ifPresent(thread -> {
                UserEntity threadAuthor = thread.getPostedBy();
                if (threadAuthor != null && threadAuthor.getId() != user.getId()) {
                    String preview = reply.getContent() != null && reply.getContent().length() > 200
                            ? reply.getContent().substring(0, 200) + "…"
                            : reply.getContent();
                    String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(threadAuthor);
                    threadMailService.sendThreadReplyNotification(
                            threadAuthor.getId(), threadAuthor.getEmail(), threadAuthor.getFirstName(),
                            thread.getTitle(), preview, unsubToken);
                }
            });

            return ResponseEntity.ok(new ThreadReplyResponse(replyEntity, 0));
        } else {
            return ResponseEntity.status(403).build();
        }

    }
    @Operation(
            summary = "Update a reply",
            description = "Updates the content of a reply. Only accessible to the author of the reply. Rate limit: 10 requests per 60 seconds per user.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The reply was successfully updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadReplyResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "The author of the reply did not match the logged in user"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The provided reply ID could not be matched to any replies."
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
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, keyType = "USER")
    @DisallowRestricted
    @PutMapping("/replies/update")
    public ResponseEntity<?> updateReply(@Valid @RequestBody UpdateReplyModel model) {
        ThreadReplyEntity reply = threadReplyRepository.findByIdWithRelations(model.getReplyId()).orElse(null);
        if (reply == null) {
            return ResponseEntity.notFound().build();
        }

        if (Objects.equals(JwtUtil.getLoggedInEmail(), reply.getPostedBy().getEmail())) {
            reply.setContent(StringUtil.cleanString(model.getContent()));
            reply.setUpdatedAt(new Date());
            ThreadReplyEntity saved = threadReplyRepository.save(reply);
            int lc = threadReplyLikeRepository.likeCountForReply(saved.getId());

            return ResponseEntity.ok().body(new ThreadReplyResponse(saved, lc));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @Operation(
            summary = "Delete a reply",
            description = "Delete a reply by its ID. Accessible to the user who created the reply as well as administrators. Rate limit: 5 requests per 60 seconds per user.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name="replyId", description = "The internal ID of the reply to delete")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The reply was successfully deleted. Returns true.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "The logged in user was not the author of the reply, or an administrator"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The provided reply ID could not be matched to any replies."
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
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @RateLimit(requests = 5, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/replies/delete")
    public ResponseEntity<?> deleteReplyById(@RequestParam int replyId,
                                             @RequestParam(required = false) Boolean markAsUserDeleted) {
        ThreadReplyEntity reply = threadReplyRepository.findByIdWithRelations(replyId).orElse(null);

        if (reply == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = JwtUtil.hasAuthority("ADMINISTRATOR");
        String email = JwtUtil.getLoggedInEmail();
        if (reply.getPostedBy().getEmail().equals(email) || isAdmin) {
            reply.getMetadata().setHidden(true);
            if (!isAdmin) {
                reply.getMetadata().setUser_deleted(true);
                reply.getMetadata().setDeletedAt(new Date());
            } else if (Boolean.TRUE.equals(markAsUserDeleted)) {
                reply.getMetadata().setUser_deleted(true);
                reply.getMetadata().setDeletedAt(new Date());
            } else {
                LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
                UserEntity admin = loggedIn != null ? userRepository.findById(loggedIn.getDatabaseId()).orElse(null) : null;
                if (admin != null) reply.getMetadata().setHiddenByUserId(admin.getId());
            }
            threadReplyRepository.save(reply);
            return ResponseEntity.ok().body(true);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(
            summary = "Hide or restore a reply",
            description = "Hides or restores a reply. Authors can hide or restore their own reply (soft delete); administrators can hide any reply and can restore only replies they hid (not author-hidden). Rate limit: 10 requests per 60 seconds per user, with burst limit of 3 requests per 30 seconds.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "replyId", description = "The internal ID of the reply to hide or restore", required = true),
                    @Parameter(name = "hidden", description = "true to hide, false to restore", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The reply was successfully hidden or restored"),
                    @ApiResponse(responseCode = "403", description = "Not logged in, or insufficient permission (author can hide/restore own; admin can hide any, restore only admin-hidden)"),
                    @ApiResponse(responseCode = "404", description = "The reply was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, burstRequests = 3, burstDurationSeconds = 30, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/replies/hide")
    public ResponseEntity<?> hideReply(@RequestParam int replyId, @RequestParam boolean hidden) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();
        ThreadReplyEntity reply = threadReplyRepository.findByIdWithRelations(replyId).orElse(null);
        if (reply == null) return ResponseEntity.status(404).build();

        boolean hiddenByAuthor = reply.getPostedBy().getId() == user.getId();
        boolean canHide = user.isAdministrator() || (hiddenByAuthor && hidden);
        boolean wasHiddenByAuthor = reply.getMetadata().isUser_deleted();
        boolean canRestore = !hidden && ((wasHiddenByAuthor && reply.getPostedBy().getId() == user.getId()) || (!wasHiddenByAuthor && user.isAdministrator()));
        if (canHide || canRestore) {
            if (hidden) {
                reply.getMetadata().setUser_deleted(hiddenByAuthor);
                reply.getMetadata().setDeletedAt(hiddenByAuthor ? new Date() : null);
                if (!hiddenByAuthor) {
                    reply.getMetadata().setHiddenByUserId(user.getId());
                }
            } else {
                reply.getMetadata().setUser_deleted(false);
                reply.getMetadata().setDeletedAt(null);
                reply.getMetadata().setHiddenByUserId(null);
            }
            reply.getMetadata().setHidden(hidden);
            threadReplyRepository.save(reply);

            if (!hidden) {
                auditLogService.log("restored reply", reply.getPostedBy().getId());
            }
            if (hidden && !hiddenByAuthor && user.isAdministrator()) {
                UserEntity author = reply.getPostedBy();
                auditLogService.log("hid reply", author.getId());
                String preview = reply.getContent() != null && reply.getContent().length() > 200
                        ? reply.getContent().substring(0, 200) + "…"
                        : reply.getContent();
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(author);
                adminUserContentMailService.sendContentModeratedNotification(
                        author.getId(), author.getEmail(), author.getFirstName(), "reply", preview, unsubToken);
            }

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(
            summary = "Permanently delete a hidden reply",
            description = "Permanently deletes a hidden reply and its metadata. Administrator only. Rate limit: 5 requests per 60 seconds per user, with burst limit of 2.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "replyId", description = "The internal ID of the hidden reply to permanently delete", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The reply was permanently deleted"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The reply was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 2, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/replies/hidden/delete")
    @Transactional
    public ResponseEntity<?> fullDeleteHiddenReply(@RequestParam int replyId) {
        ThreadReplyEntity reply = threadReplyRepository.findByIdWithRelations(replyId).orElse(null);
        if (reply == null) return ResponseEntity.status(404).build();
        if (!reply.getMetadata().isHidden()) {
            return ResponseEntity.status(403).body("Only hidden replies can be permanently deleted");
        }

        int authorId = reply.getPostedBy().getId();
        threadReplyLikeRepository.deleteByThreadIn(List.of(replyId));
        threadReplyRepository.delete(reply);  // cascade deletes metadata
        auditLogService.log("permanently deleted reply", authorId);
        return ResponseEntity.ok().build();
    }
    @Operation(
            summary = "Like or unlike a reply",
            description = "Toggles the like status for a reply. If the reply is not liked, it will be liked. If it is already liked, it will be unliked. Returns true if the reply is now liked, false if it is now unliked. Rate limit: 30 requests per 60 seconds per user, with burst limit of 5 requests per 5 seconds.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The like status was toggled successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean", description = "true if the reply is now liked, false if it is now unliked")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 30, burstRequests = 5, burstDurationSeconds = 5, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/replies/like")
    public ResponseEntity<?> likeReply(@Valid @RequestBody LikeReplyModel likeReply) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user == null) return ResponseEntity.status(403).build();
            if (!threadReplyLikeRepository.existsByThreadAndUser(likeReply.getReplyId(), user)) {
                ThreadReplyLikeEntity threadReplyLikeEntity = new ThreadReplyLikeEntity();
                threadReplyLikeEntity.setThread(likeReply.getReplyId());
                threadReplyLikeEntity.setUser(user);
                threadReplyLikeRepository.save(threadReplyLikeEntity);
                return ResponseEntity.ok(true);
            } else {
                threadReplyLikeRepository.deleteThreadReplyLikeEntitiesByThreadAndUser(likeReply.getReplyId(), user);
                return ResponseEntity.ok(false);
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    @Operation(
            summary = "Like or unlike a thread",
            description = "Toggles the like status for a thread. If the thread is not liked, it will be liked. If it is already liked, it will be unliked. Returns true if the thread is now liked, false if it is now unliked. Rate limit: 30 requests per 60 seconds per user, with burst limit of 5 requests per 5 seconds.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The like status was toggled successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean", description = "true if the thread is now liked, false if it is now unliked")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 30, burstRequests = 5, burstDurationSeconds = 5, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/like")
    public ResponseEntity<?> likeThread(@Valid @RequestBody LikeThreadModel model) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user == null) return ResponseEntity.status(403).build();
            if (!threadLikeRepository.existsByThreadAndUser(model.getThreadId(), user)) {
                ThreadLikeEntity threadLikeEntity = new ThreadLikeEntity();
                threadLikeEntity.setThread(model.getThreadId());
                threadLikeEntity.setUser(user);
                threadLikeRepository.save(threadLikeEntity);
                return ResponseEntity.ok(true);
            } else {
                threadLikeRepository.deleteThreadLikeEntityByThreadAndUser(model.getThreadId(), user);
                return ResponseEntity.ok(false);
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    @Operation(
            summary = "Get a single hidden thread by ID",
            description = "Retrieves one hidden thread by ID, including full post metadata. Administrator only. Rate limit: default per IP.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "threadId", description = "The internal ID of the hidden thread", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The hidden thread",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = HiddenThreadResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The thread was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @GetMapping("/hidden")
    public ResponseEntity<?> getHiddenThread(@RequestParam int threadId) {
        if (JwtUtil.getLoggedInUser() == null) return ResponseEntity.status(403).build();
        int uid = JwtUtil.getLoggedInUser().getDatabaseId();
        HiddenThreadResponse hiddenThreadResponse = threadMapper.findHiddenThreadById(threadId, uid);
        return hiddenThreadResponse != null ? ResponseEntity.ok(hiddenThreadResponse) : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Hide or restore a thread",
            description = "Hides or restores a thread. Authors can hide or restore their own thread (soft delete); administrators can hide any thread and can restore only threads they hid (not author-hidden). Administrators cannot restore user-deleted threads. Rate limit: 10 requests per 60 seconds per user, with burst limit of 3 requests per 30 seconds.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "threadId", description = "The internal ID of the thread to hide or restore", required = true),
                    @Parameter(name = "hidden", description = "true to hide, false to restore", required = true),
                    @Parameter(name = "markAsUserDeleted", description = "If true and caller is admin, marks as deleted-by-user (visible in deleted-by-user tab) instead of hidden-by-admin")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The thread was successfully hidden or restored"),
                    @ApiResponse(responseCode = "403", description = "Not logged in, or insufficient permission (author can hide/restore own; admin can hide any, restore only admin-hidden; admin cannot restore user-deleted threads)"),
                    @ApiResponse(responseCode = "404", description = "The thread was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, burstRequests = 3, burstDurationSeconds = 30, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/hide")
    @Transactional
    public ResponseEntity<?> hideThread(@RequestParam int threadId, @RequestParam boolean hidden,
                                         @RequestParam(required = false) Boolean markAsUserDeleted) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
            if (user == null) return ResponseEntity.status(403).build();
            Optional<ThreadEntity> thread = threadRepository.findByIdWithRelations(threadId);
            if (thread.isEmpty()) return ResponseEntity.status(404).build();
            boolean wasHiddenByAuthor = thread.get().getPostMetadata().isUser_deleted();
            if (!hidden && wasHiddenByAuthor && user.isAdministrator()) {
                return ResponseEntity.status(403).build();
            }
            boolean hiddenByAuthor = (thread.get().getPostedBy().getId() == user.getId()) && !user.isAdministrator();
            boolean asUserDelete = Boolean.TRUE.equals(markAsUserDeleted) && user.isAdministrator();
            boolean canHide = user.isAdministrator() || (hiddenByAuthor && hidden);
            boolean canRestore = !hidden && ((wasHiddenByAuthor && thread.get().getPostedBy().getId() == user.getId()) || (!wasHiddenByAuthor && user.isAdministrator()));
            if (canHide || canRestore) {
                if (hidden) {
                    boolean willBeUserDeleted = hiddenByAuthor || asUserDelete;
                    thread.get().getPostMetadata().setUser_deleted(willBeUserDeleted);
                    thread.get().getPostMetadata().setDeletedAt(willBeUserDeleted ? new Date() : null);
                    thread.get().getPostMetadata().setHiddenByUserId(asUserDelete ? null : user.getId());
                } else {
                    thread.get().getPostMetadata().setUser_deleted(false);
                    thread.get().getPostMetadata().setDeletedAt(null);
                    thread.get().getPostMetadata().setHiddenByUserId(null);
                }
                thread.get().getPostMetadata().setHidden(hidden);
                postMetadataRepository.save(thread.get().getPostMetadata());
                threadRepository.save(thread.get());

                if (!hidden) {
                    auditLogService.log("restored thread", thread.get().getPostedBy().getId());
                }
                if (hidden && !hiddenByAuthor && user.isAdministrator() && !asUserDelete) {
                    UserEntity author = thread.get().getPostedBy();
                    auditLogService.log("hid thread", author.getId());
                    String preview = thread.get().getContent() != null && thread.get().getContent().length() > 200
                            ? thread.get().getContent().substring(0, 200) + "…"
                            : thread.get().getContent();
                    String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(author);
                    adminUserContentMailService.sendContentModeratedNotification(
                            author.getId(), author.getEmail(), author.getFirstName(), "thread", preview, unsubToken);
                }

                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(
            summary = "Get paginated hidden threads",
            description = "Retrieves a paginated list of hidden threads, filtered by who hid them (admin or user). Administrator only. Rate limit: default per IP.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "hiddenThreadType", description = "ADMIN for threads hidden by admins, USER for threads hidden by their author", required = true),
                    @Parameter(name = "pageNumber", description = "Zero-based page index", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of hidden threads",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaginatedResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/getHidden")
    public ResponseEntity<?> getHiddenThreads(@RequestParam HiddenByType hiddenThreadType, @RequestParam int pageNumber) {
        int uid = JwtUtil.getLoggedInUser().getDatabaseId();
        if (hiddenThreadType == HiddenByType.ADMIN) {
            return ResponseEntity.ok(threadMapper.getHiddenThreadsAdminDeleted(pageNumber, uid));
        } else {
            return ResponseEntity.ok(threadMapper.getHiddenThreadsUserDeleted(pageNumber, uid));
        }
    }

    @Operation(
            summary = "Permanently delete a hidden thread",
            description = "Permanently deletes a hidden thread and all its replies, likes, and metadata. Administrator only. Rate limit: 5 requests per 60 seconds per user, with burst limit of 2.",
            tags = {"Thread", "Admin"},
            parameters = {
                    @Parameter(name = "threadId", description = "The internal ID of the hidden thread to permanently delete", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The thread was permanently deleted"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The thread was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 2, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @DeleteMapping("/hidden/delete")
    @Transactional
    public ResponseEntity<?> fullDeleteHiddenThread(@RequestParam int threadId) {
        var threadOpt = threadRepository.findByIdWithRelations(threadId);
        if (threadOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        ThreadEntity thread = threadOpt.get();
        int authorId = thread.getPostedBy().getId();
        int metadataId = thread.getPostMetadata().getId();
        // Delete in order: reply likes (for all replies), replies, reply metadata, thread likes, thread tags, thread, thread metadata
        List<ThreadReplyEntity> replies = threadReplyRepository.findByThreadId(threadId);
        List<Integer> replyMetadataIds = replies.stream()
                .map(r -> r.getMetadata().getId())
                .toList();
        if (!replies.isEmpty()) {
            List<Integer> replyIds = replies.stream().map(ThreadReplyEntity::getId).toList();
            threadReplyLikeRepository.deleteByThreadIn(replyIds);
        }
        threadReplyRepository.deleteByThreadId(threadId);
        replyMetadataIds.forEach(postMetadataRepository::deleteById);
        threadLikeRepository.deleteByThread(threadId);
        threadTagRepository.deleteByThread_Id(threadId);
        entityManager.flush();
        threadRepository.deleteThreadById(threadId);
        postMetadataRepository.deleteById(metadataId);
        auditLogService.log("permanently deleted thread", authorId);
        return ResponseEntity.ok().build();
    }
    @Operation(
            summary = "Update thread tags",
            description = "Replaces all tags on a thread with the provided list. Only the thread author can update tags. Tag names are normalized (trimmed, lowercased). Rate limit: 30 requests per 60 seconds per user, with burst limit of 5 requests per 5 seconds.",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tags were successfully updated. Empty response body."
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Not logged in, or the logged-in user is not the author of the thread"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The thread with the given ID was not found"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 30 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 30, burstRequests = 5, burstDurationSeconds = 5, keyType = "USER")
    @DisallowRestricted
    @PutMapping("/tags")
    @Transactional
    public ResponseEntity<?> updateTags(@RequestBody UpdateTagsModel updateTagsModel) {
        var threadOpt = threadRepository.findByIdWithRelations(updateTagsModel.getThreadId());
        if (threadOpt.isEmpty()) return ResponseEntity.status(404).build();
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        var threadEntity = threadOpt.get();
        if (!Objects.equals(threadEntity.getPostedBy().getEmail(), JwtUtil.getLoggedInUser().getEmail())) {
            return ResponseEntity.status(403).build();
        }

        int threadId = threadEntity.getId();
        List<Object[]> currentTagRows = threadTagRepository.findTagsByThreadIds(List.of(threadId));
        Set<String> currentNamesNormalized = currentTagRows.stream()
                .map(arr -> normalizeTagName((String) arr[1]))
                .collect(Collectors.toSet());

        String[] requested = updateTagsModel.getTags() != null ? updateTagsModel.getTags() : new String[0];
        Set<String> newNames = Stream.of(requested)
                .filter(Objects::nonNull)
                .map(ThreadController::normalizeTagName)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> toRemoveNormalized = new HashSet<>(currentNamesNormalized);
        toRemoveNormalized.removeAll(newNames);
        Set<String> toRemoveActual = currentTagRows.stream()
                .map(arr -> (String) arr[1])
                .filter(name -> toRemoveNormalized.contains(normalizeTagName(name)))
                .collect(Collectors.toSet());
        Set<String> toAdd = new HashSet<>(newNames);
        toAdd.removeAll(currentNamesNormalized);

        if (!toRemoveActual.isEmpty()) {
            threadTagRepository.deleteByThreadIdAndNameIn(threadId, toRemoveActual);
        }
        for (String name : toAdd) {
            ThreadTagEntity tagEntity = new ThreadTagEntity();
            tagEntity.setName(name);
            tagEntity.setThread(threadEntity);
            threadTagRepository.save(tagEntity);
        }

        return ResponseEntity.ok().build();
    }

    /** Normalizes a tag for storage and comparison: trim and lowercase. */
    private static String normalizeTagName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private List<ThreadResponse> toResponseModel(List<ThreadEntity> threads) {
        if (threads == null || threads.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter out hidden threads first
        List<ThreadEntity> visibleThreads = threads.stream()
            .filter(thread -> thread.getPostMetadata() != null && !thread.getPostMetadata().isHidden())
            .toList();

        if (visibleThreads.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch fetch all counts to avoid N+1 queries
        List<Integer> threadIds = visibleThreads.stream()
            .map(ThreadEntity::getId)
            .collect(Collectors.toList());

        // Batch fetch like counts
        Map<Integer, Integer> likeCounts = threadLikeRepository.likeCountsForThreads(threadIds)
            .stream()
            .collect(Collectors.toMap(
                arr -> (Integer) arr[0],
                arr -> ((Number) arr[1]).intValue()
            ));

        // Batch fetch comment counts
        Map<Integer, Integer> commentCounts = threadReplyRepository.visibleReplyCountsForThreads(threadIds)
            .stream()
            .collect(Collectors.toMap(
                arr -> (Integer) arr[0],
                arr -> ((Number) arr[1]).intValue()
            ));

        // Batch fetch user post counts
        Set<Integer> userIds = visibleThreads.stream()
            .map(thread -> thread.getPostedBy().getId())
            .collect(Collectors.toSet());

        Map<Integer, Integer> userPostCounts = threadRepository.visibleThreadCountsForUsers(new ArrayList<>(userIds))
            .stream()
            .collect(Collectors.toMap(
                arr -> (Integer) arr[0],
                arr -> ((Number) arr[1]).intValue()
            ));

        // Batch fetch tags for all threads
        Map<Integer, List<String>> threadTags = threadTagRepository.findTagsByThreadIds(threadIds)
            .stream()
            .collect(Collectors.groupingBy(
                arr -> (Integer) arr[0],
                Collectors.mapping(
                    arr -> (String) arr[1],
                    Collectors.toList()
                )
            ));

        // Build responses with pre-fetched data
        List<ThreadResponse> responses = new ArrayList<>();
        for (ThreadEntity thread : visibleThreads) {
            int threadId = thread.getId();
            int lc = likeCounts.getOrDefault(threadId, 0);
            int commentCount = commentCounts.getOrDefault(threadId, 0);
            int userPostCount = userPostCounts.getOrDefault(thread.getPostedBy().getId(), 0);
            List<String> tags = threadTags.getOrDefault(threadId, new ArrayList<>());

            ThreadResponse response = new ThreadResponse(thread, lc, commentCount, userPostCount);
            response.setTags(tags);
            responses.add(response);
        }
        return responses;
    }

}
