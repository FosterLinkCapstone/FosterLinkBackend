package net.fosterlink.fosterlinkbackend.controllers;


import com.google.maps.GeoApiContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import net.fosterlink.fosterlinkbackend.entities.AgencyDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApproveAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetAgenciesResponse;
import net.fosterlink.fosterlinkbackend.models.rest.GetAgencyDeletionRequestsResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.util.SqlUtil;
import net.fosterlink.fosterlinkbackend.models.web.agency.CreateAgencyModel;
import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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
    private final GeoApiContext geoApiContext;
    private final EntityManager entityManager;

    public AgencyController(AgencyMapper agencyMapper, UserRepository userRepository, LocationRepository locationRepository, AgencyRepository agencyRepository, AgencyDeletionRequestRepository deletionRequestRepository, GeoApiContext geoApiContext, EntityManager entityManager) {
        this.agencyMapper = agencyMapper;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.agencyRepository = agencyRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.geoApiContext = geoApiContext;
        this.entityManager = entityManager;
    }

    @Operation(
            summary = "Get all approved agencies",
            description = "Retrieves a paginated list of all agencies that have been approved by an administrator. Rate limit: 50 requests per 60 seconds per IP.",
            tags = {"Agency"},
            parameters = {
                    @Parameter(name = "pageNumber", description = "Zero-based page number", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Paginated list of approved agencies",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = GetAgenciesResponse.class)
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
    public ResponseEntity<?> getAllAgencies(@RequestParam int pageNumber) {
        int totalCount = agencyRepository.countApproved();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        boolean includeDeletionRequestForAdmin = false;
        Integer currentUserId = null;
        if (JwtUtil.isLoggedIn()) {
            UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
            if (user != null) {
                currentUserId = user.getId();
                if (user.isAdministrator()) {
                    includeDeletionRequestForAdmin = true;
                }
            }
        }
        return ResponseEntity.ok(new GetAgenciesResponse(agencyMapper.getAllApprovedAgencies(pageNumber, includeDeletionRequestForAdmin, currentUserId), totalPages));
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
                                    schema = @Schema(implementation = GetAgenciesResponse.class)
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
    public ResponseEntity<?> getPendingAgencies(@RequestParam int pageNumber) {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            int totalCount = agencyRepository.countPendingOrDenied();
            int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
            return ResponseEntity.ok(new GetAgenciesResponse(agencyMapper.getAllPendingAgencies(pageNumber), totalPages));
        } else return ResponseEntity.status(403).build();
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
    @PostMapping("/approve")
    public ResponseEntity<?> approveAgency(@Valid @RequestBody ApproveAgencyResponse model) {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            Optional<AgencyEntity> agency = agencyRepository.findById(model.getId());
            if (agency.isPresent()) {
                agency.get().setApproved(model.isApproved());
                agency.get().setApproved_by_id(userEntity.getId());
                agencyRepository.save(agency.get());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(404).build();
            }
        } else {
            return ResponseEntity.status(403).build();
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
    @PostMapping("/create")
    public ResponseEntity<?> createAgency(@Valid @RequestBody CreateAgencyModel model) {
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (user.isAdministrator() || user.isVerifiedAgencyRep()) {
                    AgencyEntity agency =  new AgencyEntity();
                    agency.setName(model.getName());
                    agency.setWebsiteUrl(model.getWebsiteUrl());
                    agency.setMissionStatement(model.getMissionStatement());
                    agency.setAgent(user);

                    LocationEntity location = new LocationEntity();
                    location.setZipCode(model.getLocationZipCode());
                    location.setCity(model.getLocationCity());
                    location.setState(model.getLocationState());
                    location.setAddrLine1(model.getLocationAddrLine1());
                    location.setAddrLine2(model.getLocationAddrLine2());

                    LocationEntity savedLocation = locationRepository.save(location);

                    agency.setAddress(savedLocation);

                    AgencyEntity savedAgency = agencyRepository.save(agency);

                    AgencyResponse agencyResponse = new AgencyResponse();

                    agencyResponse.setAgencyName(savedAgency.getName());
                    agencyResponse.setAgencyMissionStatement(savedAgency.getMissionStatement());
                    agencyResponse.setAgencyWebsiteLink(savedAgency.getWebsiteUrl());
                    agencyResponse.setId(agency.getId());
                    agencyResponse.setAgent(new UserResponse(user));
                    agencyResponse.setAgentInfo(new AgentInfoResponse(user));
                    agencyResponse.setLocation(savedLocation);

                    return ResponseEntity.ok(agency);
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
    @GetMapping("/pending/count")
    public ResponseEntity<?> countPending() {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            return ResponseEntity.ok(agencyRepository.countPending());
        } else return ResponseEntity.status(403).build();
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
    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<?> deleteAgency(@RequestParam int id) {
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (user == null || !user.isAdministrator()) {
            return ResponseEntity.status(403).build();
        }
        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(id);
        if (agencyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AgencyEntity agency = agencyOpt.get();
        LocationEntity address = agency.getAddress();
        deletionRequestRepository.deleteByAgencyId(id);
        agencyRepository.deleteAgencyById(id);
        entityManager.flush();
        if (address != null) {
            locationRepository.deleteById(address.getId());
        }
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
    @PostMapping("/hide")
    public ResponseEntity<?> hideAgency(@RequestParam int id, @RequestParam boolean hidden) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (!user.isAdministrator()) return ResponseEntity.status(403).build();

        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(id);
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyEntity agency = agencyOpt.get();
        agency.setHidden(hidden);
        agency.setHiddenByUsername(hidden ? user.getUsername() : null);
        agencyRepository.save(agency);
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
                                    schema = @Schema(implementation = GetAgenciesResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @GetMapping("/getHidden")
    public ResponseEntity<?> getHiddenAgencies(@RequestParam int pageNumber) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (!user.isAdministrator()) return ResponseEntity.status(403).build();

        int totalCount = agencyRepository.countHidden();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new GetAgenciesResponse(agencyMapper.getAllHiddenAgencies(pageNumber), totalPages));
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
    @PostMapping("/deletion-request")
    public ResponseEntity<?> createDeletionRequest(@RequestParam int agencyId) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());

        Optional<AgencyEntity> agencyOpt = agencyRepository.findById(agencyId);
        if (agencyOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyEntity agency = agencyOpt.get();
        if (agency.getAgent().getId() != user.getId() && !user.isAdministrator()) {
            return ResponseEntity.status(403).build();
        }

        AgencyDeletionRequestEntity request = new AgencyDeletionRequestEntity();
        request.setAgency(agency);
        request.setRequestedBy(user);
        request.setCreatedAt(new Date());
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
    @DeleteMapping("/deletion-request")
    @Transactional
    public ResponseEntity<?> cancelDeletionRequest(@RequestParam int agencyId) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());

        Optional<AgencyDeletionRequestEntity> requestOpt = deletionRequestRepository.findPendingByAgencyId(agencyId);
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyDeletionRequestEntity request = requestOpt.get();
        if (request.getRequestedBy().getId() != user.getId() && !user.isAdministrator()) {
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
                                    schema = @Schema(implementation = GetAgencyDeletionRequestsResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit
    @GetMapping("/deletion-requests")
    public ResponseEntity<?> getDeletionRequests(@RequestParam int pageNumber) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (!user.isAdministrator()) return ResponseEntity.status(403).build();

        int totalCount = deletionRequestRepository.countPending();
        int totalPages = totalCount <= 0 ? 1 : (totalCount + SqlUtil.ITEMS_PER_PAGE - 1) / SqlUtil.ITEMS_PER_PAGE;
        return ResponseEntity.ok(new GetAgencyDeletionRequestsResponse(agencyMapper.getAllDeletionRequests(pageNumber), totalPages));
    }

    @Operation(
            summary = "Approve or deny a deletion request",
            description = "Approves or denies a pending agency deletion request. If approved, the agency and its address are permanently deleted. Only accessible to administrators. Rate limit: 15 requests per 60 seconds per user.",
            tags = {"Agency", "Admin"},
            parameters = {
                    @Parameter(name = "requestId", description = "The internal ID of the deletion request to approve or deny", required = true),
                    @Parameter(name = "approved", description = "true to approve (delete the agency), false to deny (cancel the request)", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The deletion request was processed (agency deleted if approved, request cancelled if denied)"),
                    @ApiResponse(responseCode = "403", description = "Not logged in or not an administrator"),
                    @ApiResponse(responseCode = "404", description = "The deletion request was not found"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded. Maximum 15 requests per 60 seconds per user.")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @RateLimit(requests = 15, keyType = "USER")
    @PostMapping("/deletion-request/approve")
    @Transactional
    public ResponseEntity<?> approveDeletionRequest(@RequestParam int requestId, @RequestParam boolean approved) {
        if (!JwtUtil.isLoggedIn()) return ResponseEntity.status(403).build();
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (!user.isAdministrator()) return ResponseEntity.status(403).build();

        Optional<AgencyDeletionRequestEntity> requestOpt = deletionRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) return ResponseEntity.notFound().build();

        AgencyDeletionRequestEntity deletionRequest = requestOpt.get();

        if (approved) {
            AgencyEntity agency = deletionRequest.getAgency();
            LocationEntity address = agency.getAddress();
            deletionRequestRepository.deleteByAgencyId(agency.getId());
            agencyRepository.deleteAgencyById(agency.getId());
            entityManager.flush();
            if (address != null) {
                locationRepository.deleteById(address.getId());
            }
        } else {
            deletionRequestRepository.deleteRequestById(requestId);
        }
        return ResponseEntity.ok().build();
    }

}
