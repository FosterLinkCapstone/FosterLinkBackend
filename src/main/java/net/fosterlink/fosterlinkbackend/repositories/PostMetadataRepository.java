package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.PostMetadataEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import org.springframework.data.repository.CrudRepository;

public interface PostMetadataRepository extends CrudRepository<PostMetadataEntity, Integer> {
}
