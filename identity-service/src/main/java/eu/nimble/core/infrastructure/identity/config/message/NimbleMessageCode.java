package eu.nimble.core.infrastructure.identity.config.message;

public enum NimbleMessageCode {
    MAIL_SUBJECT_RESET_CREDENTIALS_LINK("MAIL_SUBJECT.reset_credentials_link"),
    MAIL_SUBJECT_INVITATION("MAIL_SUBJECT.invitation"),
    MAIL_SUBJECT_INVITATION_EXISTING_COMPANY("MAIL_SUBJECT.invitation_existing_company"),
    MAIL_SUBJECT_COMPANY_REGISTERED("MAIL_SUBJECT.company_registered"),
    MAIL_SUBJECT_COMPANY_VERIFIED("MAIL_SUBJECT.company_verified"),
    MAIL_SUBJECT_COMPANY_DELETED("MAIL_SUBJECT.company_deleted")
    ;

    private String value;

    NimbleMessageCode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
