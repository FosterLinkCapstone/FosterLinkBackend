package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<UserEntity, Integer> {

    @Query(value = "SELECT user FROM UserEntity user")
    List<UserEntity> getAllUsers();

    UserEntity findByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

}
