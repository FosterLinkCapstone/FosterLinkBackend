package net.fosterlink.fosterlinkbackend.models.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(description = "Contact information for an agency representative",
        requiredProperties = {"id", "email", "phoneNumber"})
public class AgentInfoResponse {

    public AgentInfoResponse(UserEntity agent) {
        this.id = agent.getId();
        this.email = agent.getEmail();
        this.phoneNumber = agent.getPhoneNumber();
    }

    @Schema(description = "The internal ID of the user (agency representative)")
    private int id; // user id
    @Schema(description = "The email address of the agency representative")
    private String email;
    @Schema(description = "The phone number of the agency representative")
    private String phoneNumber;

}
