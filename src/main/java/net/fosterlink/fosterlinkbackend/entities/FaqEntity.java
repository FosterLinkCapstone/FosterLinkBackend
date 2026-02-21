package net.fosterlink.fosterlinkbackend.entities;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the faq table. Represents a frequently asked question with title,
 * summary, full content, and author. Approval is tracked separately in FAQApprovalEntity.
 */
@Entity
@Data
@Table(name="faq")
public class FaqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** FAQ title. */
    private String title;
    /** Full FAQ content. */
    private String content;
    /** Short summary for listings. */
    private String summary;
    /** When the FAQ was created. */
    private Date createdAt;
    /** When the FAQ was last updated. Null if never updated. */
    @Nullable
    private Date updatedAt;

    /** User who authored this FAQ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="author", nullable = false)
    private UserEntity author;

}
