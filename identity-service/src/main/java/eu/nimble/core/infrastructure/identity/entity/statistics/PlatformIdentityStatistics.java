package eu.nimble.core.infrastructure.identity.entity.statistics;

public class PlatformIdentityStatistics {

    private Long totalUsers;

    private Long totalCompanies;

    private PlatformIdentityStatistics() {
    }

    public PlatformIdentityStatistics(Long totalUsers, Long totalCompanies) {
        this.totalUsers = totalUsers;
        this.totalCompanies = totalCompanies;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public Long getTotalCompanies() {
        return totalCompanies;
    }
}