package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Full user information for admin review, including roles, stats, and punishment status.")
public class AdminUserResponse {

    @Schema(description = "Internal user ID")
    private int id;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Phone number")
    @Nullable
    private String phoneNumber;

    @Schema(description = "Profile picture URL")
    @Nullable
    private String profilePictureUrl;

    // --- Role flags ---
    @Schema(description = "Whether the user is an administrator")
    private boolean administrator;

    @Schema(description = "Whether the user is an FAQ author")
    private boolean faqAuthor;

    @Schema(description = "Whether the user is a verified agency representative")
    private boolean verifiedAgencyRep;

    @Schema(description = "Whether the user is a verified foster parent")
    private boolean verifiedFoster;

    @Schema(description = "Whether the user's ID has been verified")
    private boolean idVerified;

    // --- Punishment status ---
    @Schema(description = "Timestamp when user was banned; null if not banned")
    @Nullable
    private Date bannedAt;

    @Schema(description = "Timestamp when user was restricted; null if not restricted")
    @Nullable
    private Date restrictedAt;

    @Schema(description = "Timestamp until which the user is restricted; null if indefinitely restricted or not restricted")
    @Nullable
    private Date restrictedUntil;

    // --- Activity stats ---
    @Schema(description = "Number of forum threads posted")
    private int postCount;

    @Schema(description = "Number of forum replies posted")
    private int replyCount;

    @Schema(description = "Number of agencies the user represents")
    private int agencyCount;

    @Schema(description = "Number of approved FAQ answers authored")
    private int faqAnswerCount;

    @Schema(description = "Number of FAQ topic suggestions submitted")
    private int faqSuggestionCount;
}
