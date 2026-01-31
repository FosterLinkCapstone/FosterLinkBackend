package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ProfileMetadataResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PrivilegesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.web.user.UpdateUserModel;
import net.fosterlink.fosterlinkbackend.models.web.user.UserLoginModelEmail;
import net.fosterlink.fosterlinkbackend.models.web.user.UserRegisterModel;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.UserMapper;
import net.fosterlink.fosterlinkbackend.security.JwtTokenProvider;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/users/")
public class UserController {

    private static final String DEFAULT_PROFILE_PIC = "https://upload.wikimedia.org/wikipedia/commons/a/ac/Default_pfp.jpg?20200418092106";

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
    @RateLimit(requests = 5, burstRequests = 1, burstDurationSeconds = 30)
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterModel model) {

        if (userRepository.existsByUsernameOrEmail(model.getUsername(), model.getEmail())) {
            return ResponseEntity.status(409).build();
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(model.getUsername());

        userEntity.setEmail(model.getEmail());
        userEntity.setPhoneNumber((model.getPhoneNumber()));

        userEntity.setPassword(passwordEncoder.encode(model.getPassword()));
        userEntity.setFirstName(model.getFirstName());
        userEntity.setLastName(model.getLastName());
        userEntity.setCreatedAt(new Date());
        userEntity.setProfilePictureUrl(DEFAULT_PROFILE_PIC);

        UserEntity dbEntity = userRepository.save(userEntity);
        try {
            String jwt = loginUser(dbEntity.getEmail(), model.getPassword());
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (Exception e) {
            // TODO better logging
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
    public ResponseEntity<?> loginUser(@RequestBody UserLoginModelEmail model) {
        try {
            String jwt = loginUser(model.getEmail(), model.getPassword());
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }

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
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserModel model, HttpServletRequest req) {
        String email = JwtUtil.getLoggedInEmail();
        UserEntity user = email != null ? userRepository.findByEmail(email) : null;
        boolean logout = false;
        if (JwtUtil.isLoggedIn() && user != null && user.getId() == model.getUserId()) {
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
            UserEntity saved = userRepository.save(user);
            if (logout) {
                try {
                    manualLogout(req);
                } catch (ServletException e) {
                    // TODO log error somewhere
                    return ResponseEntity.status(500).build();
                }
            }
            // the frontend should always refresh after this request
            return ResponseEntity.ok(saved);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
    public ResponseEntity<?> deleteUser() {
            String email = JwtUtil.getLoggedInEmail();
            UserEntity user = userRepository.findByEmail(email);
            if (user != null) {
                // TODO logout
                userRepository.delete(user);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
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
        String email = JwtUtil.getLoggedInEmail();
        UserEntity user = userRepository.findByEmail(email);
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
        PrivilegesResponse res = new  PrivilegesResponse(false,false,false);
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            res.setAdmin(user.isAdministrator());
            res.setFaqAuthor(user.isFaqAuthor());
            res.setAgent(user.isVerifiedAgencyRep());
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
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            return ResponseEntity.ok(user.isAdministrator());
        }
        return ResponseEntity.ok(false);
    }
    @Operation(
            summary = "Check if user is verified agency representative",
            description = "Returns whether the currently logged-in user is a verified agency representative. Returns false if not logged in. Rate limit: 60 requests per 60 seconds per IP.",
            tags = {"User"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Whether the user is a verified agency representative",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "boolean", description = "true if the user is a verified agency representative, false otherwise")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 60 requests per 60 seconds per IP."
                    )
            }
    )
    @RateLimit(requests = 60)
    @GetMapping("/isAgent")
    public ResponseEntity<?> isAgent() {
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            return ResponseEntity.ok(user.isVerifiedAgencyRep());
        }
        return ResponseEntity.ok(false);
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
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            return ResponseEntity.ok(user.isFaqAuthor());
        }
        return ResponseEntity.ok(false);
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
    public ResponseEntity<?> logout(HttpServletRequest request) {
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

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ProfileMetadataResponse res = userMapper.mapProfileMetadataResponse(userId);
        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(res);
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


}
