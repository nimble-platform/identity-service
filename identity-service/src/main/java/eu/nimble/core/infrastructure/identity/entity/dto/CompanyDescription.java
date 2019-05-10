package eu.nimble.core.infrastructure.identity.entity.dto;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Johannes Innerbichler on 25.09.18.
 */
@ApiModel(value = "CompanyDescription")
public class CompanyDescription {

    @ApiModelProperty(value = "Statement of the company")
    private Map<NimbleConfigurationProperties.LanguageID, String> companyStatement = new HashMap<>();

    @ApiModelProperty(value = "Main website of the company")
    private String website = null;

    @ApiModelProperty(value = "Identifiers of company photos")
    private List<String> companyPhotoList = new ArrayList<>();

    @ApiModelProperty(value = "Identifiers of company logo image")
    private String logoImageId = null;

    @ApiModelProperty(value = "List of social media references (e.g. Facebook)")
    private List<String> socialMediaList = new ArrayList<>();

    @ApiModelProperty(value = "List of company related events")
    private List<CompanyEvent> events = new ArrayList<>();

    private List<String> externalResources = new ArrayList<>();

    public CompanyDescription() {
    }

    public CompanyDescription(Map<NimbleConfigurationProperties.LanguageID, String> companyStatement, String website, List<String> companyPhotoList, String logoImageId,
                              List<String> socialMediaList, List<CompanyEvent> events, List<String> externalResources) {
        this.companyStatement = companyStatement;
        this.website = website;
        this.companyPhotoList = companyPhotoList;
        this.logoImageId = logoImageId;
        this.socialMediaList = socialMediaList;
        this.events = events;
        this.externalResources = externalResources;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getCompanyStatement() {
        return companyStatement;
    }

    public void setCompanyStatement(Map<NimbleConfigurationProperties.LanguageID, String> companyStatement) {
        this.companyStatement = companyStatement;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<String> getCompanyPhotoList() {
        return companyPhotoList;
    }

    public void setCompanyPhotoList(List<String> companyPhotoList) {
        this.companyPhotoList = companyPhotoList;
    }

    public String getLogoImageId() {
        return logoImageId;
    }

    public void setLogoImageId(String logoImageId) {
        this.logoImageId = logoImageId;
    }

    public List<String> getSocialMediaList() {
        return socialMediaList;
    }

    public void setSocialMediaList(List<String> socialMediaList) {
        this.socialMediaList = socialMediaList;
    }

    public List<CompanyEvent> getEvents() {
        return events;
    }

    public void setEvents(List<CompanyEvent> events) {
        this.events = events;
    }

    public List<String> getExternalResources() {
        return externalResources;
    }

    public void setExternalResources(List<String> externalResources) {
        this.externalResources = externalResources;
    }
}
