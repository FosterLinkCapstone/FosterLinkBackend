package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;
import org.springframework.data.repository.CrudRepository;

public interface ThreadReplyRepository extends CrudRepository<ThreadReplyEntity, Integer> {
}
