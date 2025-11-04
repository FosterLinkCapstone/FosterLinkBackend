package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import net.fosterlink.fosterlinkbackend.models.rest.UserResponse;
import net.fosterlink.fosterlinkbackend.models.web.UserLoginModelEmail;
import net.fosterlink.fosterlinkbackend.models.web.UserRegisterModel;
import net.fosterlink.fosterlinkbackend.repositories.UserRepository;
import net.fosterlink.fosterlinkbackend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/")
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

    @GetMapping("/get-all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserEntity> users = userRepository.getAllUsers();
        return ResponseEntity.ok(users.stream().map(UserResponse::new).toList());
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterModel model) {

        if (userRepository.existsByUsernameOrEmail(model.getUsername(), model.getEmail())) {
            return ResponseEntity.status(409).build();
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(model.getUsername());

        if (model.isRegisteredUsingEmail()) {
            userEntity.setEmail(model.getEmail());
        } else {
            // TODO fix conflict
            userEntity.setEmail(model.getPhoneNumber());
            userEntity.setPhoneNumber((model.getPhoneNumber()));
        }

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
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }

    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginModelEmail model) {
        String jwt = loginUser(model.getEmail(), model.getPassword());
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    private String loginUser(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,password
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        return jwtTokenProvider.generateToken(auth);
    }


}
