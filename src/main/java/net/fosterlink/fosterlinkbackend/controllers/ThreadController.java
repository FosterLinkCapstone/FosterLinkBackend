package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;
import net.fosterlink.fosterlinkbackend.models.rest.ThreadResponse;
import net.fosterlink.fosterlinkbackend.models.web.thread.CreateThreadModel;
import net.fosterlink.fosterlinkbackend.models.web.thread.UpdateThreadModel;
import net.fosterlink.fosterlinkbackend.repositories.ThreadLikeRepository;
import net.fosterlink.fosterlinkbackend.repositories.ThreadRepository;
import net.fosterlink.fosterlinkbackend.repositories.ThreadTagRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import net.fosterlink.fosterlinkbackend.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/v1/threads/")
public class ThreadController {

    private @Autowired ThreadRepository threadRepository;
    private @Autowired ThreadTagRepository threadTagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ThreadLikeRepository threadLikeRepository;

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
        // TODO allow only verified foster parents to create a thread?

        ThreadEntity threadEntity = new ThreadEntity();
        threadEntity.setCreatedAt(new Date());
        threadEntity.setTitle(StringUtil.cleanString(model.getTitle()));
        threadEntity.setContent(StringUtil.cleanString(model.getContent()));

        ThreadEntity savedThread = threadRepository.save(threadEntity);

        Set<ThreadTagEntity> entities = new HashSet<>();

        for (String tag : model.getTags()) {

            ThreadTagEntity threadTagEntity = new ThreadTagEntity();
            threadTagEntity.setName(tag);
            threadTagEntity.setThread(savedThread);
            entities.add(threadTagEntity);

        }

        threadTagRepository.saveAll(entities);

        return ResponseEntity.ok().body(new ThreadResponse(savedThread, 0));
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

        ThreadEntity thread = threadRepository.findById(model.getThreadId()).orElse(null);
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

            return ResponseEntity.ok().body(new ThreadResponse(saved, lc));
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

        ThreadEntity t =  threadRepository.findById(threadId).orElse(null);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        int lc = threadLikeRepository.likeCountForThread(threadId);
        return ResponseEntity.ok().body(new ThreadResponse(t, lc));
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
    public ResponseEntity<?> searchByTitle(@RequestParam String title) {
        List<ThreadEntity> threads = threadRepository.findByTitleContaining(title);
        return toResponseModel(threads);
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
            return toResponseModel(threads);

        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            description = "Delete a thread by its ID. Accessible to the user who created the thread as well as administrators.",
            tags = {"Thread"},
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
        ThreadEntity t =  threadRepository.findById(threadId).orElse(null);

        if (t == null) {
            return ResponseEntity.notFound().build();
        }

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        String email = JwtUtil.getLoggedInEmail();
        if (t.getPostedBy().getEmail().equals(email) || authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(s->s.equals("ADMINISTRATOR"))) {
            threadRepository.deleteById(threadId);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private ResponseEntity<?> toResponseModel(List<ThreadEntity> threads) {
        List<ThreadResponse> responses = new ArrayList<>();
        for (ThreadEntity thread : threads) {
            int lc = threadLikeRepository.likeCountForThread(thread.getId());
            responses.add(new ThreadResponse(thread, lc));
        }
        return ResponseEntity.ok().body(responses);
    }

}
