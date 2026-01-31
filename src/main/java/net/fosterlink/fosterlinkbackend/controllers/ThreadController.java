package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.fosterlink.fosterlinkbackend.entities.*;
import net.fosterlink.fosterlinkbackend.models.rest.GetThreadsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadReplyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.models.web.thread.*;
import net.fosterlink.fosterlinkbackend.repositories.*;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadMapper;
import net.fosterlink.fosterlinkbackend.repositories.mappers.ThreadReplyMapper;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    @Operation(
            summary = "Create a new thread",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The successfully created schema",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreateThreadModel model) {

        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());

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
                for (String tag : model.getTags()) {
                    ThreadTagEntity threadTagEntity = new ThreadTagEntity();
                    threadTagEntity.setName(tag);
                    threadTagEntity.setThread(savedThread);
                    entities.add(threadTagEntity);

                }
            }


            threadTagRepository.saveAll(entities);

            int commentCount = threadReplyRepository.visibleReplyCountForThread(savedThread.getId());
            int userPostCount = threadRepository.visibleThreadCountForUser(user.getId());
            return ResponseEntity.ok().body(new ThreadResponse(savedThread, 0, commentCount, userPostCount));
        } else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

    }

    @Operation(
            summary="Update a thread",
            tags = {"Thread"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The successfully updated thread",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ThreadResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "The author of the thread did not match the logged in user"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access a secure endpoint without providing an authorized JWT (see bearerAuth security policy)"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody UpdateThreadModel model) {

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
            ThreadEntity saved = threadRepository.save(thread);
            int lc = threadLikeRepository.likeCountForThread(saved.getId());

            int commentCount = threadReplyRepository.visibleReplyCountForThread(saved.getId());
            int userPostCount = threadRepository.visibleThreadCountForUser(saved.getPostedBy().getId());
            return ResponseEntity.ok().body(new ThreadResponse(saved, lc, commentCount, userPostCount));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }
    @Operation(
            description = "Search for a thread by it's internal ID",
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
                    )
            }
    )
    @GetMapping("/search-by-id")
    public ResponseEntity<?> searchById(@RequestParam int threadId) {
        int userId = JwtUtil.isLoggedIn() ? userRepository.findByEmail(JwtUtil.getLoggedInEmail()).getId() : -1;
        ThreadResponse thread = threadMapper.findById(threadId, userId);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(thread);
    }

    @Operation(
            description = "Search for a thread by it's title, including non-complete matches.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name="title", description = "The title, or a portion of the title, that should be searched.")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A collection of threads were found with similar titles. The response can also be empty, which means no threads were found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ThreadResponse.class))
                            )
                    )
            }
    )
    @GetMapping("/search-by-title")
    public ResponseEntity<?> searchByTitle(@RequestParam String title, @RequestParam int pageNumber) {
        List<ThreadEntity> threads = threadRepository.findByContentContaining(title, PageRequest.of(pageNumber, SqlUtil.ITEMS_PER_PAGE));
        return ResponseEntity.ok(toResponseModel(threads));
    }

    @Operation(
            description = "Search for threads by the day they were created on.",
            tags = {"Thread"},
            parameters = {
                    @Parameter(name="date", description = "The day on which to search. Must be formatted as MM-dd-yyyy", example = "01-01-2026")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "A collection of threads made on the specified date. "
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The date provided was not in the proper format. must be MM-dd-yyyy"
                    )
            }
    )
    // REQUIRED FORMAT mm-dd-yyyy
    @GetMapping("/search-by-date")
    public ResponseEntity<?> searchByDate(@RequestParam String date) {
        DateFormat sourceFormat = new SimpleDateFormat("MM-dd-yyyy");
        try {
            Date dateParsed = sourceFormat.parse(date);
            Calendar cal =  Calendar.getInstance();
            cal.setTime(dateParsed);
            cal.add(Calendar.HOUR_OF_DAY, 0);
            cal.add(Calendar.MINUTE, 0);
            cal.add(Calendar.SECOND, 0);
            cal.add(Calendar.MILLISECOND, 0);
            Date start = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date end = cal.getTime();

            List<ThreadEntity> threads = threadRepository.findByCreatedAtBetween(start, end);
            return ResponseEntity.ok(toResponseModel(threads));

        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "Search threads by author",
            description = "Retrieves threads created by a specific user, paginated. Returns 404 if the user does not exist.",
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
                                    schema = @Schema(implementation = GetThreadsResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The user with the provided ID could not be found"
                    )
            }
    )
    @GetMapping("/search-by-user")
    public ResponseEntity<?> searchByUser(@RequestParam int userId, @RequestParam int pageNumber) {
        boolean authorExists = userRepository.existsById(userId);

        int sendingUserId = JwtUtil.isLoggedIn() ? userRepository.findByEmail(JwtUtil.getLoggedInEmail()).getId() : -1;

        if (!authorExists) {
            return ResponseEntity.notFound().build();
        }

        var threads = threadMapper.searchByUser(sendingUserId, userId, pageNumber);
        int totalCount = threadRepository.visibleThreadCountForUser(userId);
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new GetThreadsResponse(threads, totalPages));
    }

    @Operation(
            description = "Delete a thread by its ID. Accessible to the user who created the thread as well as administrators.",
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
                    )
            },
            security = {
                    @SecurityRequirement(name = "bearerAuth")
            }

    )
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteById(@RequestParam int threadId) {
        ThreadEntity t =  threadRepository.findByIdWithRelations(threadId).orElse(null);

        if (t == null) {
            return ResponseEntity.notFound().build();
        }

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(s->s.equals("ADMINISTRATOR"));
        String email = JwtUtil.getLoggedInEmail();
        if (t.getPostedBy().getEmail().equals(email) || isAdmin) {
            t.getPostMetadata().setHidden(true);
            if (!isAdmin) t.getPostMetadata().setUser_deleted(true);
            threadRepository.save(t);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @Operation(
            description = "Search for threads using various criteria. Note: This endpoint uses POST for search, which is not ideal REST practice but is implemented for flexibility.",
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
                    )
            }
    )
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody SearchThreadModel search) {
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
            description = "Get random weighted threads. If a userId is provided, the threads will be weighted based on the user's preferences.",
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
                    )
            }
    )
    @GetMapping("/rand")
    public ResponseEntity<?> rand(@RequestParam(required = false, defaultValue = "-1") int userId) {
        if (userId != -1) {
            return ResponseEntity.ok(threadMapper.findRandomWeightedThreadsForUser(userId));
        }
        return ResponseEntity.ok(threadMapper.findRandomWeightedThreads(-1));
    }
    @Operation(
            description = "Get threads ordered by a specified strategy, limited to a count.",
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
                                    schema = @Schema(implementation = GetThreadsResponse.class)
                            )
                    )
            }
    )
    @GetMapping("/getThreads")
    public ResponseEntity<?> getThreads(
            @RequestParam String orderBy,
            @RequestParam int pageNumber
    ) {
        int userId = JwtUtil.isLoggedIn() ? userRepository.findByEmail(JwtUtil.getLoggedInEmail()).getId() : -1;
        var threads = threadMapper.getThreads(orderBy, userId, pageNumber);
        int totalCount = threadRepository.countVisible();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new GetThreadsResponse(threads, totalPages));
    }
    @Operation(
            summary = "Get replies for a thread",
            description = "Retrieves all replies for a specific thread. If a user is logged in, includes whether the user has liked each reply.",
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
                    )
            }
    )
    @GetMapping("/replies")
    public ResponseEntity<?> replies(@RequestParam int threadId) {
        int userId = JwtUtil.isLoggedIn() ? userRepository.findByEmail(JwtUtil.getLoggedInEmail()).getId() : -1;
        return ResponseEntity.ok(threadReplyMapper.getRepliesForThread(threadId, userId));

    }
    @Operation(
            summary = "Reply to a thread",
            description = "Creates a new reply to a thread. Only accessible to logged-in users.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/replies")
    public ResponseEntity<?> replyTo(@RequestBody ReplyToThreadModel reply) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());

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
            return ResponseEntity.ok(new ThreadReplyResponse(replyEntity, 0));
        } else {
            return ResponseEntity.status(403).build();
        }

    }
    @Operation(
            summary = "Update a reply",
            description = "Updates the content of a reply. Only accessible to the author of the reply.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/replies/update")
    public ResponseEntity<?> updateReply(@RequestBody UpdateReplyModel model) {
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
            description = "Delete a reply by its ID. Accessible to the user who created the reply as well as administrators.",
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
                    )
            },
            security = {
                    @SecurityRequirement(name = "bearerAuth")
            }
    )
    @DeleteMapping("/replies/delete")
    public ResponseEntity<?> deleteReplyById(@RequestParam int replyId) {
        ThreadReplyEntity reply = threadReplyRepository.findByIdWithRelations(replyId).orElse(null);

        if (reply == null) {
            return ResponseEntity.notFound().build();
        }

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(s->s.equals("ADMINISTRATOR"));
        String email = JwtUtil.getLoggedInEmail();
        if (reply.getPostedBy().getEmail().equals(email) || isAdmin) {
            reply.getMetadata().setHidden(true);
            if (!isAdmin) reply.getMetadata().setUser_deleted(true);
            threadReplyRepository.save(reply);
            return ResponseEntity.ok().body(true);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @Operation(
            summary = "Like or unlike a reply",
            description = "Toggles the like status for a reply. If the reply is not liked, it will be liked. If it is already liked, it will be unliked. Returns true if the reply is now liked, false if it is now unliked.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/replies/like")
    public ResponseEntity<?> likeReply(@RequestBody LikeReplyModel likeReply) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
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
            description = "Toggles the like status for a thread. If the thread is not liked, it will be liked. If it is already liked, it will be unliked. Returns true if the thread is now liked, false if it is now unliked.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/like")
    public ResponseEntity<?> likeThread (@RequestBody LikeThreadModel model) {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
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
