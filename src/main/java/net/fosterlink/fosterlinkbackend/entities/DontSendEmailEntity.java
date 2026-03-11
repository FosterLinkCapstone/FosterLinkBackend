package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dont_send_email", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email_type_id", "user_id"})
})
public class DontSendEmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "email_type_id", nullable = false)
    private int emailTypeId;

    @Column(name = "user_id", nullable = false)
    private int userId;

}
