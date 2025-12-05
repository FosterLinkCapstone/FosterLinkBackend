package net.fosterlink.fosterlinkbackend.models.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalCheckResponse {

    private int countPending;
    private int countDenied;

}
