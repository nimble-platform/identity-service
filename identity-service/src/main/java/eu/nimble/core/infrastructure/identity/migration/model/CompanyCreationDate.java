package eu.nimble.core.infrastructure.identity.migration.model;

public class CompanyCreationDate {
    private Long id;
    private String date;

    public CompanyCreationDate() {
    }

    public CompanyCreationDate(Long id, String date) {
        this.id = id;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long companyId) {
        this.id = companyId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}