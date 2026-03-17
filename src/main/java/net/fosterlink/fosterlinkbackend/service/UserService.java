package net.fosterlink.fosterlinkbackend.service;


import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.CachedUserData;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    private @Autowired UserRepository userRepository;

    /**
     * Called by Spring's AuthenticationManager during the login flow only.
     * Returns the full UserDetails including the BCrypt password hash so the
     * AuthenticationManager can verify credentials. Not cached — login is
     * infrequent and the hash must never be stored in the heap-visible cache.
     *
     * @param email !! Email !! not username
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) return null;
        return toLoggedInUser(userEntity);
    }

    /**
     * Loads a lightweight projection for per-request JWT validation. Cached in the
     * "userDetails" cache (10 000 entries, 2-min TTL) so that every authenticated
     * request does not hit the database. The BCrypt password hash is excluded from
     * the cached value — ban status is handled separately by BanStatusService.
     */
    @Cacheable(value = "userDetails", key = "#email", unless = "#result == null")
    public CachedUserData loadCachedData(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) return null;
        return new CachedUserData(
                userEntity.getId(),
                userEntity.getEmail(),
                userEntity.getAuthTokenVersion(),
                buildAuthorities(userEntity),
                userEntity.getRestrictedAt() != null
        );
    }

    private LoggedInUser toLoggedInUser(UserEntity userEntity) {
        boolean isBanned = userEntity.getBannedAt() != null;
        boolean enabled = !isBanned;
        boolean accountNonLocked = !isBanned;
        return new LoggedInUser(userEntity.getId(), userEntity.getEmail(), userEntity.getAuthTokenVersion(), userEntity.getPassword(), buildAuthorities(userEntity), enabled, true, true, accountNonLocked, userEntity.getRestrictedAt() != null);
    }

    private Set<String> buildAuthorities(UserEntity user) {
        Set<String> authorities = new HashSet<>();
        authorities.add("USER");
        if (user.isFaqAuthor()) authorities.add("FAQ_AUTHOR");
        if (user.isEmailVerified()) authorities.add("EMAIL_VERIFIED");
        if (user.isIdVerified()) authorities.add("ID_VERIFIED");
        if (user.isVerifiedFoster())  authorities.add("FOSTER_VERIFIED");
        if (user.isVerifiedAgencyRep()) authorities.add("AGENCY_REP");
        if (user.isAdministrator())  authorities.add("ADMINISTRATOR");
        return authorities;
    }

}
