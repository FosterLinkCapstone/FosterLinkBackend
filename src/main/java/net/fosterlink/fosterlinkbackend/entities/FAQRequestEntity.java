package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * JPA entity for the faq_request table. Represents a user-submitted suggestion
 * for a new FAQ topic; FAQ authors and admins can review and address these.
 */
@Entity
@Data
@Table(name="faq_request")
public class FAQRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** ID of the user who submitted the suggestion. */
    @Column(name = "requested_by")
    private int requestedById;
    /** The suggested FAQ topic or question. */
    private String suggestedTopic;
    /** When the suggestion was submitted. */
    private Date createdAt;
}
