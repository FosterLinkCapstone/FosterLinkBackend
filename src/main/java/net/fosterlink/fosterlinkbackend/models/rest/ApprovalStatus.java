package net.fosterlink.fosterlinkbackend.models.rest;

public enum ApprovalStatus {

    APPROVED,
    DENIED,
    PENDING;

    public static ApprovalStatus fromDbVal(int approvalStatus) {
        return switch (approvalStatus) {
            case 1 -> APPROVED;
            case 2 -> DENIED;
            default -> PENDING;
        };
    }

}
