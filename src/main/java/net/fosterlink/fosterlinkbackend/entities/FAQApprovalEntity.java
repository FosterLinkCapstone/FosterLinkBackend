package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="faq_approval")
public class FAQApprovalEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int id;
    private int faqId;
    private boolean approved;
    private int approvedById;

}
