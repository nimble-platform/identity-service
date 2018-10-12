package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModelProperty;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
public class Address {

    @ApiModelProperty(value = "Name of the street")
    private String streetName;
    @ApiModelProperty(value = "Number of the building")
    private String buildingNumber;
    @ApiModelProperty(value = "Name of the city")
    private String cityName;
    @ApiModelProperty(value = "Postal code of the city")
    private String postalCode;
    @ApiModelProperty(value = "Name of the country")
    private String country;

    public Address() {
    }

    public Address(String streetName, String buildingNumber, String cityName, String postalCode, String country) {
        this.streetName = streetName;
        this.buildingNumber = buildingNumber;
        this.cityName = cityName;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

}
