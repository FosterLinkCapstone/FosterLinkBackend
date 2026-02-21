package net.fosterlink.fosterlinkbackend.repositories;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadTagEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThreadTagRepository extends CrudRepository<ThreadTagEntity, Integer> {

    List<ThreadTagEntity> searchByName(String name);
    
    @Query("SELECT tt.thread.id, tt.name FROM ThreadTagEntity tt WHERE tt.thread.id IN :threadIds")
    List<Object[]> findTagsByThreadIds(@Param("threadIds") List<Integer> threadIds);
    
    @Query("SELECT DISTINCT tt.thread.id FROM ThreadTagEntity tt WHERE tt.name LIKE CONCAT('%', :name, '%')")
    List<Integer> findThreadIdsByName(@Param("name") String name);

    @org.springframework.transaction.annotation.Transactional
    void deleteByThread_Id(int threadId);

}
