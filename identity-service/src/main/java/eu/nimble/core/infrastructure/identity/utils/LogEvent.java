package eu.nimble.core.infrastructure.identity.utils;

public enum LogEvent {
    LOGIN_ATTEMPT("loginAttempt"), LOGIN_SUCCESS("loginSuccess"), LOGIN_ERROR("loginError"),
    REGISTER_USER("registerUser"), REGISTER_USER_ERROR("registerUserError"), REGISTER_COMPANY("registerCompany"), UPDATE_COMPANY("updateCompany"),
    VERIFY_COMPANY("verifyCompany"), DELETE_COMPANY("deleteCompany") ,  DELETE_USER("deleteUser"), REVERT_COMPANY("revertCompany"),
    INVITED_USER_REGISTRATION("invitedUserRegistration");

    private String activity;

    LogEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
