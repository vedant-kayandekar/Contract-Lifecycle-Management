package com.seventhray.contract_backend.model;


/**
 * Contract lifecycle statuses.
 * Contains embedded state machine logic to validate legal transitions.
 */
public enum ContractStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    /**
     * Enforces the CLM State Machine rules.
     * Returns true if transition is legally allowed; false otherwise.
     */
    public boolean canTransitionTo(ContractStatus target) {
        // If the target is the exact same status, it's a no-op (allowed)
        if (this == target) {
            return true;
        }

        // Modern Java switch expression evaluating permitted state paths:
        return switch (this) {
            // A DRAFT can only move forward into UNDER_REVIEW
            case DRAFT -> target == UNDER_REVIEW;

            // UNDER_REVIEW can be APPROVED, REJECTED, or sent back to DRAFT
            case UNDER_REVIEW -> target == APPROVED || target == REJECTED || target == DRAFT;

            // An APPROVED contract can only be moved to REJECTED (disputed) or re-reviewed
            case APPROVED -> target == REJECTED || target == UNDER_REVIEW;

            // A REJECTED contract can be moved back to DRAFT for edits or resubmitted for review
            case REJECTED -> target == DRAFT || target == UNDER_REVIEW;
        };
    }
}