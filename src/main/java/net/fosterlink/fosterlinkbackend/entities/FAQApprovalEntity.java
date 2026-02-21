package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for the faq_approval table. Records approval or denial of an FAQ
 * by an administrator (approved flag and approver user ID).
 */
@Entity
@Data
@Table(name="faq_approval")
public class FAQApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /** ID of the FAQ that was approved or denied. */
    private int faqId;
    /** true if approved, false if denied. */
    private boolean approved;
    /** ID of the user (administrator) who performed the approval/denial. */
    private int approvedById;

}
