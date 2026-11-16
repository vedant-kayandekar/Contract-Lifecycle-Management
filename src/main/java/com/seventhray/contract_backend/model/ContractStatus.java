package com.seventhray.contract_backend.model;

public enum ContractStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    REJECTED;

    public boolean canTransitionTo(ContractStatus target){

        if(this == target){
            return true;
        }

        return switch (this) {

             case DRAFT -> target == UNDER_REVIEW;

             case UNDER_REVIEW -> target == APPROVED || target == REJECTED || target == DRAFT;

             case APPROVED -> target == REJECTED || target == UNDER_REVIEW;

             case REJECTED -> target == DRAFT || target == UNDER_REVIEW;
        };
    }
}
