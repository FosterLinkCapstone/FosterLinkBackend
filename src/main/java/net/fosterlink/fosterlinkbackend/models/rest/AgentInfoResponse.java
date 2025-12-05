package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.fosterlink.fosterlinkbackend.entities.UserEntity;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AgentInfoResponse {

    public AgentInfoResponse(UserEntity agent) {
        this.id = agent.getId();
        this.email = agent.getEmail();
        this.phoneNumber = agent.getPhoneNumber();
    }

    private int id; // user id
    private String email;
    private String phoneNumber;

}
