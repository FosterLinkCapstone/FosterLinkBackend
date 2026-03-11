package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.MailingListMemberEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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
}
