package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.MailingListMemberEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MailingListMemberRepository extends CrudRepository<MailingListMemberEntity, Integer> {

    /**
     * Returns the user IDs of every member belonging to the mailing list with the given name.
     */
    @Query(value = """
            SELECT mlm.user_id FROM mailing_list_member mlm
            JOIN mailing_list ml ON ml.id = mlm.mailing_list_id
            WHERE ml.name = :mailingListName
            """, nativeQuery = true)
    List<Integer> findUserIdsByMailingListName(@Param("mailingListName") String mailingListName);

    /**
     * Returns the mailing list IDs the given user belongs to.
     */
    @Query(value = "SELECT mlm.mailing_list_id FROM mailing_list_member mlm WHERE mlm.user_id = :userId", nativeQuery = true)
    List<Integer> findMailingListIdsByUserId(@Param("userId") int userId);

    /**
     * Removes all mailing list memberships for the given user.
     * Uses a native query because the entity is @Immutable and cannot be deleted via JPA lifecycle methods.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM mailing_list_member WHERE user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") int userId);
}
