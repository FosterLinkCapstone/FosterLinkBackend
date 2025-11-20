package net.fosterlink.fosterlinkbackend.service;


import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
     * All user loads are done by email
     * @param email !! Email !! not username
     * @return user details for auth
     * @throws UsernameNotFoundException email not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) return null;
        return convertEntity(userEntity);
    }
    private LoggedInUser convertEntity(UserEntity userEntity) {
        return new LoggedInUser(userEntity.getId(), userEntity.getEmail(), userEntity.getPassword(), buildAuthorities(userEntity), true, true, true, true); // TODO account locking choke point
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
