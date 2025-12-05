package net.fosterlink.fosterlinkbackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="agency")
public class AgencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String missionStatement;
    private String websiteUrl;
    private Boolean approved;
    private Integer approved_by_id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="address")
    private LocationEntity address;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="agent", nullable = false)
    private UserEntity agent;

}
