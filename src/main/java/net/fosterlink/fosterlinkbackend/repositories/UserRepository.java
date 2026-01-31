package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.UserEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<UserEntity, Integer> {

    @Query(value = "SELECT user FROM UserEntity user")
    List<UserEntity> getAllUsers();

    UserEntity findByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

    UserEntity findByUsername(String username);

    @Query("""
            SELECT u.id, u.administrator, u.faqAuthor, u.verifiedAgencyRep, a.id, a.name,
                   u.firstName, u.lastName, u.username, u.profilePictureUrl, u.verifiedFoster, u.createdAt
            FROM UserEntity u
            LEFT JOIN AgencyEntity a ON a.agent = u
            WHERE u.id = :userId
            """)
    List<Object[]> getProfileMetadataRow(@Param("userId") int userId);
}
