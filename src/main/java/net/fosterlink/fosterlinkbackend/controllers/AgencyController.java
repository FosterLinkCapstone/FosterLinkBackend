package net.fosterlink.fosterlinkbackend.controllers;


import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
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

    @GetMapping("/all")
    public ResponseEntity<?> getAllAgencies() {
        return ResponseEntity.ok(agencyMapper.getAllApprovedAgencies());
    }
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAgencies() {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            return ResponseEntity.ok(agencyMapper.getAllPendingAgencies());
        } else return ResponseEntity.status(403).build();
    }
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
    @PostMapping("/create")
    public ResponseEntity<?> createAgency(@Valid @RequestBody CreateAgencyModel model) {
        UserEntity user = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (user.isAdministrator() || user.isVerifiedAgencyRep()) {

            String locationString = model.getLocationAddrLine1() + " " +  model.getLocationAddrLine2() + ", " + model.getLocationCity() + " " + model.getLocationState() + ", " + model.getLocationZipCode();

            try {
                GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, locationString).await();
                if (results != null && results.length > 0) {
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
                    return ResponseEntity.badRequest().build();
                }
            } catch (ApiException | InterruptedException | IOException e) {
                return ResponseEntity.status(502).build();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }
    @GetMapping("/pending/count")
    public ResponseEntity<?> countPending() {
        UserEntity userEntity = userRepository.findByEmail(JwtUtil.getLoggedInEmail());
        if (userEntity.isAdministrator()) {
            return ResponseEntity.ok(agencyRepository.countByApprovedNullOrApprovedFalse());
        } else return ResponseEntity.status(403).build();
    }

}
