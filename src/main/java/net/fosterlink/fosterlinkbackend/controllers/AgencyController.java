package net.fosterlink.fosterlinkbackend.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.config.restriction.DisallowRestricted;
import net.fosterlink.fosterlinkbackend.entities.AgencyDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.AuditLogEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApproveAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.PaginatedResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.web.agency.LocationInput;
import net.fosterlink.fosterlinkbackend.models.web.agency.DelayAgencyDeletionModel;
import net.fosterlink.fosterlinkbackend.models.web.agency.UpdateAgencyLocationModel;
import net.fosterlink.fosterlinkbackend.models.web.agency.UpdateAgencyModel;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.models.web.agency.CreateAgencyModel;
import net.fosterlink.fosterlinkbackend.mail.service.AgencyMailService;
import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.AuditLogRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
import net.fosterlink.fosterlinkbackend.service.AuditLogService;
import net.fosterlink.fosterlinkbackend.service.AgencyDeletionService;
import net.fosterlink.fosterlinkbackend.service.TokenAuthService;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST API for agency management: list approved/pending agencies, create, approve/deny, and delete agencies.
 * Base path: /v1/agencies/
 */
@RestController
@RequestMapping("/v1/agencies/")
public class AgencyController {

    private final AgencyMapper agencyMapper;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyDeletionRequestRepository deletionRequestRepository;
    private final AgencyDeletionService agencyDeletionService;
    private final EntityManager entityManager;
    private final AgencyMailService agencyMailService;
    private final TokenAuthService tokenAuthService;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    public AgencyController(AgencyMapper agencyMapper, UserRepository userRepository, LocationRepository locationRepository,
                            AgencyRepository agencyRepository, AgencyDeletionRequestRepository deletionRequestRepository,
                            AgencyDeletionService agencyDeletionService, EntityManager entityManager,
                            AgencyMailService agencyMailService, TokenAuthService tokenAuthService,
                            AuditLogRepository auditLogRepository, AuditLogService auditLogService) {
        this.agencyMapper = agencyMapper;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.agencyRepository = agencyRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.agencyDeletionService = agencyDeletionService;
        this.entityManager = entityManager;
        this.agencyMailService = agencyMailService;
        this.tokenAuthService = tokenAuthService;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }

    @Operation(
            summary = "Get all approved agencies",
            description = "Retrieves a paginated list of all agencies that have been approved by an administrator. Supports search by agency (name, mission), agent (full name, username, email, phone), or location (city, state, zip). Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Agency"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true),
                    @Parameter(name = "search", description = "Optional search term"),
                    @Parameter(name = "searchBy", description = "Category to search in: agency, agent, or location")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of approved agencies",
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
    public ResponseEntity<?> getAllAgencies(
            @RequestParam int pageNumber,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchBy) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedSearchBy = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim() : null;
        int totalCount = agencyMapper.countApprovedWithSearch(normalizedSearch, normalizedSearchBy);
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        boolean includeDeletionRequestForAdmin = false;
        Integer currentUserId = null;
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn != null) {
            currentUserId = loggedIn.getDatabaseId();
            includeDeletionRequestForAdmin = JwtUtil.hasAuthority("ADMINISTRATOR");
        }
        List<AgencyResponse> agencies = (normalizedSearch == null || normalizedSearch.isEmpty())
                ? agencyMapper.getAllApprovedAgencies(pageNumber, includeDeletionRequestForAdmin, currentUserId)
                : agencyMapper.getAllApprovedAgenciesWithSearch(pageNumber, normalizedSearch, normalizedSearchBy, includeDeletionRequestForAdmin, currentUserId);
        return ResponseEntity.ok(new PaginatedResponse<>(agencies, totalPages));
    }
    @Operation(
            summary = "Get all pending agencies",
            description = "Retrieves a paginated list of all agencies that are pending approval. Only accessible to administrators. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of pending agencies",
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
    public ResponseEntity<?> getPendingAgencies(@RequestParam int pageNumber) {
        int totalCount = agencyRepository.countPendingOrDenied();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(agencyMapper.getAllPendingAgencies(pageNumber), totalPages));
    }
    @Operation(
            summary = "Approve or deny an agency",
            description = "Allows an administrator to approve or deny a pending agency. The administrator who performs this action will be recorded as the approver. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The agency was successfully approved or denied"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The agency with the provided ID could not be found"
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
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public ResponseEntity<?> approveAgency(@Valid @RequestBody ApproveAgencyResponse model) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        Optional<AgencyEntity> agency = agencyRepository.findById(model.getId());
        if (agency.isPresent()) {
            AgencyEntity ag = agency.get();
            ag.setApproved(model.isApproved());
            ag.setApproved_by_id(Objects.requireNonNull(loggedIn).getDatabaseId());
            ag.setUpdatedAt(new Date());
            agencyRepository.save(ag);

            UserEntity agent = ag.getAgent();
            if (agent != null) {
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(agent);
                if (model.isApproved()) {
                    agencyMailService.sendAgencyApproved(agent.getId(), agent.getEmail(), agent.getFirstName(), ag.getName(), unsubToken);
                } else {
                    agencyMailService.sendAgencyDenied(agent.getId(), agent.getEmail(), agent.getFirstName(), ag.getName(), unsubToken);
                }
            }

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
    @Operation(
            summary = "Create a new agency",
            description = "Creates a new agency. Only accessible to administrators or verified agency representatives. The agency will be created in a pending state and must be approved by an administrator. Rate limit: 5 requests per 60 seconds per user, with burst limit of 1 request per 30 seconds.",
            tags = {"Agency", "Admin", "Agent"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The agency was successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AgencyEntity.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The request body validation failed (e.g., missing required fields, invalid URL format, invalid zip code)"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access this endpoint without administrator or verified agency representative privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 5 requests per 60 seconds per user."
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 1, burstDurationSeconds = 30, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR','AGENCY_REP')")
    @PostMapping("/create")
    public ResponseEntity<?> createAgency(@Valid @RequestBody CreateAgencyModel model) {
        LoggedInUser loggedInUser = JwtUtil.getLoggedInUser();
        UserEntity user = userRepository.findById(Objects.requireNonNull(loggedInUser).getDatabaseId()).orElse(null);
        if (user != null) {
                    if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A phone number is required for agency representatives. Please update your profile before creating an agency.");
                    }
                    AgencyEntity agency =  new AgencyEntity();
                    agency.setName(model.getName());
                    agency.setWebsiteUrl(model.getWebsiteUrl());
                    agency.setMissionStatement(model.getMissionStatement());
                    agency.setAgent(user);
                    agency.setCreatedAt(new Date());

                    LocationEntity location = new LocationEntity();
                    LocationInput loc = model.getLocation();
                    location.setZipCode(loc.getZipCode());
                    location.setCity(loc.getCity());
                    location.setState(loc.getState());
                    location.setAddrLine1(loc.getAddrLine1());
                    location.setAddrLine2(loc.getAddrLine2());

                    LocationEntity savedLocation = locationRepository.save(location);

                    agency.setAddress(savedLocation);

                    AgencyEntity savedAgency = agencyRepository.save(agency);

                    String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(user);
                    agencyMailService.sendAgencySubmittedConfirmation(
                            user.getId(), user.getEmail(), user.getFirstName(), agency.getName(), unsubToken);

                    AgencyResponse agencyResponse = new AgencyResponse();
                    agencyResponse.setId(savedAgency.getId());
                    agencyResponse.setAgencyName(savedAgency.getName());
                    agencyResponse.setAgencyMissionStatement(savedAgency.getMissionStatement());
                    agencyResponse.setAgencyWebsiteLink(savedAgency.getWebsiteUrl());
                    agencyResponse.setCreatedAt(savedAgency.getCreatedAt() != null ? savedAgency.getCreatedAt() : null);
                    agencyResponse.setAgent(new UserResponse(user));
                    agencyResponse.setAgentInfo(new AgentInfoResponse(user));
                    agencyResponse.setLocation(savedLocation);

                    return ResponseEntity.ok(agencyResponse);
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    @Operation(
            summary = "Get count of pending agencies",
            description = "Returns the number of agencies that are currently pending approval. Only accessible to administrators. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Agency", "Admin"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The count of pending agencies",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "integer", description = "The number of pending agencies")
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
    @GetMapping("/pending/count")
    public ResponseEntity<?> countPending() {
        return ResponseEntity.ok(agencyRepository.countPending());
    }

    @Operation(
            summary = "Delete an agency",
            description = "Permanently deletes an agency, its deletion requests (if any), and its address. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "id", description = "The internal ID of the agency to delete", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The agency was successfully deleted"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "The agency with the provided ID could not be found"
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
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public ResponseEntity<?> deleteAgency(@RequestParam int id) {
        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(id);
        if (agencyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AgencyEntity agency = agencyOpt.get();
        int agentId = agency.getAgent().getId();
        LocationEntity address = agency.getAddress();
        deletionRequestRepository.deleteByAgencyId(id);
        agencyRepository.deleteAgencyById(id);
        entityManager.flush();
        if (address != null) {
            locationRepository.deleteById(address.getId());
        }
        auditLogService.log("permanently deleted agency", agentId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Hide or restore an agency",
            description = "Hides or restores an agency from public listing. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "id", description = "The internal ID of the agency to hide or restore", required = true),
                    @Parameter(name = "hidden", description = "true to hide, false to restore", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The agency was successfully hidden or restored"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The agency was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/hide")
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public ResponseEntity<?> hideAgency(@RequestParam int id, @RequestParam boolean hidden) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        UserEntity user = loggedIn != null ? userRepository.findById(loggedIn.getDatabaseId()).orElse(null) : null;
        if (user == null) return ResponseEntity.status(403).build();

        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(id);
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyEntity agency = agencyOpt.get();
        agency.setHidden(hidden);
        agency.setHiddenByUserId(hidden ? user.getId() : null);
        agency.setHiddenByDeletionRequest(false);
        agency.setUpdatedAt(new Date());
        agencyRepository.save(agency);
        if (hidden) {
            auditLogService.log("hid agency", agency.getAgent().getId());
        } else {
            auditLogService.log("restored agency", agency.getAgent().getId());
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get all hidden agencies",
            description = "Retrieves a paginated list of agencies that have been hidden by administrators. Only accessible to administrators. Rate limit: default per IP.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of hidden agencies",
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
    @GetMapping("/getHidden")
    public ResponseEntity<?> getHiddenAgencies(@RequestParam int pageNumber) {
        int totalCount = agencyRepository.countHidden();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(agencyMapper.getAllHiddenAgencies(pageNumber), totalPages));
    }

    @Operation(
            summary = "Request agency deletion",
            description = "Creates a request to permanently delete an agency. Only the agency's representative or an administrator may submit. The agency is not deleted until an administrator approves the request. Rate limit: 5 requests per 60 seconds per user, with burst limit of 1 request per 60 seconds.",
            tags = {"Agency", "Admin", "Agent"},
            parameters = {
                    @Parameter(name = "agencyId", description = "The internal ID of the agency to request deletion for", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The deletion request was successfully created"),
                    @ApiResponse(responseCode = "403", description = "Not logged in, or not the agency representative or an administrator"),
                    @ApiResponse(responseCode = "404", description = "The agency was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 5, burstRequests = 1, burstDurationSeconds = 60, keyType = "USER")
    @DisallowRestricted
    @PostMapping("/deletion-request")
    public ResponseEntity<?> createDeletionRequest(@RequestParam int agencyId) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();

        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(agencyId);
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyEntity agency = agencyOpt.get();
        if (agency.getAgent().getId() != loggedIn.getDatabaseId() && !JwtUtil.hasAuthority("ADMINISTRATOR")) {
            return ResponseEntity.status(403).build();
        }

        UserEntity user = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        AgencyDeletionRequestEntity request = new AgencyDeletionRequestEntity();
        request.setAgency(agency);
        request.setRequestedBy(user);
        request.setCreatedAt(new Date());
        request.setAutoApproveBy(AgencyDeletionService.thirtyDaysFromNow());
        deletionRequestRepository.save(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Cancel a deletion request",
            description = "Cancels a pending agency deletion request. Only the user who created the request or an administrator may cancel. Rate limit: 10 requests per 60 seconds per user.",
            tags = {"Agency", "Admin", "Agent"},
            parameters = {
                    @Parameter(name = "agencyId", description = "The internal ID of the agency whose deletion request to cancel", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The deletion request was successfully cancelled"),
                    @ApiResponse(responseCode = "403", description = "Not logged in, or not the requester or an administrator"),
                    @ApiResponse(responseCode = "404", description = "No pending deletion request found for the agency"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 10, keyType = "USER")
    @DisallowRestricted
    @DeleteMapping("/deletion-request")
    @Transactional
    public ResponseEntity<?> cancelDeletionRequest(@RequestParam int agencyId) {
        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();

        Optional<AgencyDeletionRequestEntity> requestOpt = deletionRequestRepository.findPendingByAgencyId(agencyId);
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyDeletionRequestEntity request = requestOpt.get();
        if (request.getRequestedBy().getId() != loggedIn.getDatabaseId() && !JwtUtil.hasAuthority("ADMINISTRATOR")) {
            return ResponseEntity.status(403).build();
        }

        deletionRequestRepository.deleteRequestById(request.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get all agency deletion requests",
            description = "Retrieves a paginated list of pending agency deletion requests. Only accessible to administrators. Rate limit: default per IP.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of deletion requests",
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
    @GetMapping("/deletion-requests")
    public ResponseEntity<?> getDeletionRequests(@RequestParam int pageNumber,
                                                  @RequestParam(defaultValue = "recency") String sortBy) {
        int totalCount = deletionRequestRepository.countPending();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new PaginatedResponse<>(agencyMapper.getAllDeletionRequests(pageNumber, sortBy), totalPages));
    }

    @Operation(
            summary = "Approve a deletion request",
            description = "Approves a pending agency deletion request. The agency and its address are permanently deleted. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "requestId", description = "The internal ID of the deletion request to approve", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The deletion request was approved and the agency was deleted"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The deletion request was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/deletion-request/approve")
    public ResponseEntity<?> approveDeletionRequest(@RequestParam int requestId) {
        Optional<AgencyDeletionRequestEntity> requestOpt = deletionRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity admin = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (admin == null) return ResponseEntity.status(403).build();

        AgencyDeletionRequestEntity request = requestOpt.get();
        Integer agentId = request.getAgency() != null && request.getAgency().getAgent() != null
                ? request.getAgency().getAgent().getId() : null;
        agencyDeletionService.approveDeletion(request, admin);
        if (agentId != null) {
            auditLogService.log("permanently deleted agency", agentId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Delay a deletion request",
            description = "Delays a pending agency deletion request by 30 days with an administrator-provided reason. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The deletion request was delayed by 30 days"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The deletion request was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    @PostMapping("/deletion-request/delay")
    public ResponseEntity<?> delayDeletionRequest(@Valid @RequestBody DelayAgencyDeletionModel model) {
        Optional<AgencyDeletionRequestEntity> requestOpt = deletionRequestRepository.findById(model.getRequestId());
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        LoggedInUser loggedIn = JwtUtil.getLoggedInUser();
        if (loggedIn == null) return ResponseEntity.status(403).build();
        UserEntity admin = userRepository.findById(loggedIn.getDatabaseId()).orElse(null);
        if (admin == null) return ResponseEntity.status(403).build();

        AgencyDeletionRequestEntity request = requestOpt.get();
        Integer agentId = request.getAgency() != null && request.getAgency().getAgent() != null
                ? request.getAgency().getAgent().getId() : null;
        if (agentId != null) {
            AuditLogEntity auditEntry = new AuditLogEntity();
            auditEntry.setAction("delayed agency deletion");
            auditEntry.setActingUserId(admin.getId());
            auditEntry.setTargetUserId(agentId);
            auditEntry.setCreatedAt(new Date());
            auditLogRepository.save(auditEntry);
        }

        agencyDeletionService.delayDeletion(request, admin, model.getReason());
        return ResponseEntity.ok().build();
    }
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAnyAuthority('AGENCY_REP', 'ADMINISTRATOR')")
    @PutMapping("/update")
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public ResponseEntity<?> updateAgency(@RequestBody UpdateAgencyModel model) {
        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(model.getAgencyId());
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();
        AgencyEntity agency = agencyOpt.get();
        if (!Objects.equals(agency.getAgent().getEmail(), JwtUtil.getLoggedInEmail())) return ResponseEntity.status(403).build();

        if (model.getName() != null) agency.setName(model.getName());
        if (model.getMissionStatement() != null) agency.setMissionStatement(model.getMissionStatement());
        if (model.getWebsiteUrl() != null) agency.setWebsiteUrl(model.getWebsiteUrl());

        Date now = new Date();
        agency.setUpdatedAt(now);
        agencyRepository.save(agency);
        agencyRepository.setAgencyPending(agency.getId(), now);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Update an agency's location",
            description = "Updates the physical address of an agency. Only the agency's representative may update. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin", "Agent"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "The agency location was successfully updated"),
                    @ApiResponse(responseCode = "400", description = "The request body validation failed (e.g., invalid zip code, missing required fields)"),
                    @ApiResponse(responseCode = "403", description = "You must be the owner of this agency to update its location."),
                    @ApiResponse(responseCode = "404", description = "The agency was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @DisallowRestricted
    @PreAuthorize("hasAnyAuthority('AGENCY_REP', 'ADMINISTRATOR')")
    @PutMapping("/update-location")
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public ResponseEntity<?> updateAgencyLocation(@Valid @RequestBody UpdateAgencyLocationModel model) {
        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(model.getAgencyId());
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();
        AgencyEntity agency = agencyOpt.get();
        if (!Objects.equals(agency.getAgent().getEmail(), JwtUtil.getLoggedInEmail())) return ResponseEntity.status(403).build();

        LocationEntity location = agency.getAddress();
        if (location == null) {
            location = new LocationEntity();
            agency.setAddress(location);
        }
        LocationInput loc = model.getLocation();
        location.setAddrLine1(loc.getAddrLine1());
        location.setAddrLine2(loc.getAddrLine2());
        location.setCity(loc.getCity());
        location.setState(loc.getState());
        location.setZipCode(loc.getZipCode());

        locationRepository.save(location);
        Date now = new Date();
        agency.setUpdatedAt(now);
        agencyRepository.save(agency);
        agencyRepository.setAgencyPending(agency.getId(), now);
        return ResponseEntity.ok().build();
    }

}
