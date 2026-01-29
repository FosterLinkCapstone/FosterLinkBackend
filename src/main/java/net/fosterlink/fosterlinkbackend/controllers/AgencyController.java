package net.fosterlink.fosterlinkbackend.controllers;


import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.AgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.AgentInfoResponse;
import net.fosterlink.fosterlinkbackend.models.rest.ApproveAgencyResponse;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.web.agency.CreateAgencyModel;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.repositories.mappers.AgencyMapper;
import net.fosterlink.fosterlinkbackend.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/v1/agencies/")
public class AgencyController {

    private final AgencyMapper agencyMapper;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final AgencyRepository agencyRepository;
    private final GeoApiContext geoApiContext;

    public AgencyController(AgencyMapper agencyMapper, UserRepository userRepository, LocationRepository locationRepository, AgencyRepository agencyRepository, GeoApiContext geoApiContext) {
        this.agencyMapper = agencyMapper;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.agencyRepository = agencyRepository;
        this.geoApiContext = geoApiContext;
    }

    @Operation(
            summary = "Get all approved agencies",
            description = "Retrieves a list of all agencies that have been approved by an administrator",
            tags = {"Agency"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all approved agencies",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AgencyResponse.class))
                            )
                    )
            }
    )
    @GetMapping("/all")
    public ResponseEntity<?> getAllAgencies() {
        return ResponseEntity.ok(agencyMapper.getAllApprovedAgencies());
    }
    @Operation(
            summary = "Get all pending agencies",
            description = "Retrieves a list of all agencies that are pending approval. Only accessible to administrators.",
            tags = {"Agency", "Admin"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of all pending agencies",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AgencyResponse.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user attempted to access an administrator-only endpoint without administrator privileges, or without providing an authorized JWT (see bearerAuth security policy)"
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAgencies() {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            return ResponseEntity.ok(agencyMapper.getAllPendingAgencies());
        } else return ResponseEntity.status(403).build();
    }
    @Operation(
            summary = "Approve or deny an agency",
            description = "Allows an administrator to approve or deny a pending agency. The administrator who performs this action will be recorded as the approver.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/approve")
    public ResponseEntity<?> approveAgency(@RequestBody ApproveAgencyResponse model) {
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
            description = "Creates a new agency. Only accessible to administrators or verified agency representatives. The agency will be created in a pending state and must be approved by an administrator.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
            description = "Returns the number of agencies that are currently pending approval. Only accessible to administrators.",
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
                    )
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/pending/count")
    public ResponseEntity<?> countPending() {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            return ResponseEntity.ok(agencyRepository.countByApprovedNull());
        } else return ResponseEntity.status(403).build();
    }

}
