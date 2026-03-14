package net.fosterlink.fosterlinkbackend.service;

import net.fosterlink.fosterlinkbackend.entities.ThreadEntity;
import net.fosterlink.fosterlinkbackend.entities.ThreadReplyEntity;
import net.fosterlink.fosterlinkbackend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hard-deletes forum threads and replies that were user-soft-deleted 90+ days ago.
 * Mirrors the cascade deletion order in AccountDeletionService.clearAccountContent().
 */
@Service
public class PostCleanupService {

    @Autowired private ThreadRepository threadRepository;
    @Autowired private ThreadReplyRepository threadReplyRepository;
    @Autowired private ThreadLikeRepository threadLikeRepository;
    @Autowired private ThreadReplyLikeRepository threadReplyLikeRepository;
    @Autowired private ThreadTagRepository threadTagRepository;

    @Transactional
    public void purgeExpiredSoftDeletedPosts() {
        purgeExpiredThreads();
        purgeExpiredReplies();
    }

    /**
     * Phase 1: hard-delete threads whose post_metadata crossed the 90-day retention window.
     * Deletes all child content (replies, likes, tags) before removing the thread so FK
     * constraints are satisfied. JPA cascade on @ManyToOne(cascade=ALL) removes post_metadata
     * when the thread/reply entity itself is deleted.
     */
    private void purgeExpiredThreads() {
        List<Integer> threadIds = threadRepository.findIdsEligibleForHardDelete();
        if (threadIds.isEmpty()) return;

        // Collect all replies on these threads so we can delete their likes first.
        List<ThreadReplyEntity> replies = threadIds.stream()
                .flatMap(tid -> threadReplyRepository.findByThreadId(tid).stream())
                .toList();
        if (!replies.isEmpty()) {
            List<Integer> replyIds = replies.stream().map(ThreadReplyEntity::getId).toList();
            threadReplyLikeRepository.deleteByThreadIn(replyIds);
            // deleteAll triggers JPA cascade → also removes each reply's post_metadata row.
            threadReplyRepository.deleteAll(replies);
        }

        threadLikeRepository.deleteByThreadIdIn(threadIds);
        threadTagRepository.deleteByThreadIdIn(threadIds);

        // JOIN FETCH ensures postMetadata is initialised so cascade fires on deleteAll.
        List<ThreadEntity> threads = threadRepository.findAllByIdWithPostMetadata(threadIds);
        threadRepository.deleteAll(threads);
    }

    /**
     * Phase 2: hard-delete standalone replies whose post_metadata crossed the 90-day window.
     * Replies whose parent thread was already hard-deleted in Phase 1 have already been
     * removed (along with their post_metadata) and will not appear in this query.
     */
    private void purgeExpiredReplies() {
        List<Integer> replyIds = threadReplyRepository.findIdsEligibleForHardDelete();
        if (replyIds.isEmpty()) return;

        threadReplyLikeRepository.deleteByThreadIn(replyIds);
        // JOIN FETCH ensures metadata is initialised so cascade fires on deleteAll.
        List<ThreadReplyEntity> replies = threadReplyRepository.findAllByIdWithMetadata(replyIds);
        threadReplyRepository.deleteAll(replies);
    }
}
