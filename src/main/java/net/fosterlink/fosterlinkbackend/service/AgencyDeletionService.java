package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.AgencyDeletionRequestEntity;
import net.fosterlink.fosterlinkbackend.entities.AgencyEntity;
import net.fosterlink.fosterlinkbackend.entities.LocationEntity;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.mail.service.AgencyMailService;
import net.fosterlink.fosterlinkbackend.repositories.AgencyDeletionRequestRepository;
import net.fosterlink.fosterlinkbackend.repositories.AgencyRepository;
import net.fosterlink.fosterlinkbackend.repositories.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service handling the lifecycle of agency deletion requests:
 * manual approval, delay, and scheduled auto-approval.
 */
@Service
public class AgencyDeletionService {

    @Autowired private AgencyDeletionRequestRepository deletionRequestRepository;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private AgencyMailService agencyMailService;
    @Autowired private TokenAuthService tokenAuthService;

    /** Admin approves a deletion request. Deletes the agency and sends a confirmation email. */
    @Transactional
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public void approveDeletion(AgencyDeletionRequestEntity request, UserEntity admin) {
        AgencyEntity agency = request.getAgency();
        UserEntity agent = agency != null ? agency.getAgent() : null;
        String agencyName = agency != null ? agency.getName() : "";

        request.setApproved(true);
        request.setReviewedAt(new Date());
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);

        if (agency != null) {
            LocationEntity address = agency.getAddress();
            deletionRequestRepository.deleteByAgencyId(agency.getId());
            agencyRepository.deleteAgencyById(agency.getId());
            if (address != null) {
                locationRepository.deleteById(address.getId());
            }
        }

        if (agent != null) {
            String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(agent);
            agencyMailService.sendAgencyDeletionApproved(
                    agent.getId(), agent.getEmail(), agent.getFirstName(), agencyName, unsubToken);
        }
    }

    /**
     * Admin delays a deletion request by 30 days. Records the reason and updates the reviewer.
     * Only one delay note is kept (overwrites the previous).
     */
    @Transactional
    public void delayDeletion(AgencyDeletionRequestEntity request, UserEntity admin, String reason) {
        request.setDelayNote(reason);
        request.setAutoApproveBy(thirtyDaysFromNow());
        request.setReviewedAt(new Date());
        request.setReviewedBy(admin);
        deletionRequestRepository.save(request);

        AgencyEntity agency = request.getAgency();
        UserEntity agent = agency != null ? agency.getAgent() : null;
        if (agent != null) {
            String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(agent);
            agencyMailService.sendAgencyDeletionDelayed(
                    agent.getId(), agent.getEmail(), agent.getFirstName(),
                    agency.getName(), request.getAutoApproveBy(), reason, unsubToken);
        }
    }

    /**
     * Finds all requests past their auto-approve date and executes the deletion for each.
     * Called by the scheduled job every hour.
     */
    @Transactional
    @CacheEvict(value = "agencyApprovedRows", allEntries = true)
    public void processAutoApprovals() {
        List<AgencyDeletionRequestEntity> expired = deletionRequestRepository.findAllPastAutoApproveDate(new Date());
        for (AgencyDeletionRequestEntity request : expired) {
            AgencyEntity agency = request.getAgency();
            UserEntity agent = agency != null ? agency.getAgent() : null;
            String agencyName = agency != null ? agency.getName() : "";

            request.setAutoApproved(true);
            request.setApproved(true);
            request.setReviewedAt(new Date());
            deletionRequestRepository.save(request);

            if (agency != null) {
                LocationEntity address = agency.getAddress();
                deletionRequestRepository.deleteByAgencyId(agency.getId());
                agencyRepository.deleteAgencyById(agency.getId());
                if (address != null) {
                    locationRepository.deleteById(address.getId());
                }
            }

            if (agent != null) {
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(agent);
                agencyMailService.sendAgencyDeletionApproved(
                        agent.getId(), agent.getEmail(), agent.getFirstName(), agencyName, unsubToken);
            }
        }
    }

    /**
     * Sends 7-day auto-approval warnings for pending requests approaching their deadline.
     * Called by the scheduler independently of processAutoApprovals.
     */
    @Transactional(readOnly = true)
    public void processAutoApprovalWarnings() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        Date sevenDaysFromNow = cal.getTime();

        List<AgencyDeletionRequestEntity> approaching =
                deletionRequestRepository.findApproachingAutoApproval(now, sevenDaysFromNow);
        for (AgencyDeletionRequestEntity request : approaching) {
            AgencyEntity agency = request.getAgency();
            UserEntity agent = agency != null ? agency.getAgent() : null;
            if (agent != null) {
                String unsubToken = tokenAuthService.getOrCreateUnsubscribeToken(agent);
                agencyMailService.sendAgencyAutoApprovalWarning(
                        agent.getId(), agent.getEmail(), agent.getFirstName(),
                        agency.getName(), request.getAutoApproveBy(), unsubToken);
            }
        }
    }

    public static Date thirtyDaysFromNow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        return cal.getTime();
    }
}
