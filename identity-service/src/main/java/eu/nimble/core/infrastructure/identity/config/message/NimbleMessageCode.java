package eu.nimble.core.infrastructure.identity.config.message;

public enum NimbleMessageCode {
    MAIL_SUBJECT_RESET_CREDENTIALS_LINK("MAIL_SUBJECT.reset_credentials_link"),
    MAIL_SUBJECT_INVITATION("MAIL_SUBJECT.invitation"),
    MAIL_SUBJECT_INVITATION_EXISTING_COMPANY("MAIL_SUBJECT.invitation_existing_company"),
    MAIL_SUBJECT_COMPANY_REGISTERED("MAIL_SUBJECT.company_registered"),
    MAIL_SUBJECT_COMPANY_VERIFIED("MAIL_SUBJECT.company_verified"),
    MAIL_SUBJECT_COMPANY_DELETED("MAIL_SUBJECT.company_deleted"),
    MAIL_SUBJECT_COMPANY_DATA_UPDATED("MAIL_SUBJECT.company_data_updated"),
    COMPANY_NAME("COMPANY_NAME"),
    BRAND_NAME("BRAND_NAME"),
    VAT_NUMBER("VAT_NUMBER"),
    VERIFICATION_INFO("VERIFICATION_INFO"),
    BUSINESS_TYPE("BUSINESS_TYPE"),
    ACTIVITY_SECTORS("ACTIVITY_SECTORS"),
    BUSINESS_KEYWORDS("BUSINESS_KEYWORDS"),
    YEAR_FOUNDATION("YEAR_FOUNDATION"),
    STREET("STREET"),
    BUILDING_NUMBER("BUILDING_NUMBER"),
    CITY_TOWN("CITY_TOWN"),
    STATE_PROVINCE("STATE_PROVINCE"),
    POSTAL_CODE("POSTAL_CODE"),
    COUNTRY("COUNTRY"),
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
