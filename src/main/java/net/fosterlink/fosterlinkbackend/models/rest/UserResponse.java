package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.Data;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

import java.util.Date;

@Data
public class UserResponse
{

    public UserResponse(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.username = userEntity.getUsername();
        this.fullName = userEntity.getFirstName() + " " + userEntity.getLastName();
        this.profilePictureUrl = userEntity.getProfilePictureUrl();
        this.verified = userEntity.isVerifiedFoster() || userEntity.isFaqAuthor() || userEntity.isVerifiedAgencyRep();
        this.createdAt = userEntity.getCreatedAt();
    }
    private int id;
    private String fullName;
    private String username;
    private String profilePictureUrl;
    private boolean verified; // faqAuthor, foster, or agency rep (UI should combine all to one)
    private Date createdAt;

}
