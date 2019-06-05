package eu.nimble.core.infrastructure.identity.utils;

public enum LogEvent {
    LOGIN_ATTEMPT("loginAttempt"), LOGIN_SUCCESS(("loginSuccess")), LOGIN_ERROR("loginError"),
    REGISTER_USER(("registerUser")), REGISTER_COMPANY(("registerCompany")), UPDATE_COMPANY(("updateCompany"));

    private String activity;

    LogEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
