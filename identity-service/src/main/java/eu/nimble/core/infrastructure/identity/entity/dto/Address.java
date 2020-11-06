package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;
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
    @ApiModelProperty(value = "Name of the district")
    private String district;
    @ApiModelProperty(value = "Name of the region")
    private String region;

    public Address() {
    }

    public Address(String streetName, String buildingNumber, String cityName, String postalCode, String country) {
        this.streetName = streetName;
        this.buildingNumber = buildingNumber;
        this.cityName = cityName;
        this.postalCode = postalCode;
        this.country = country;
    }

    public Address(String streetName, String buildingNumber, String cityName, String postalCode, String country, String district, String region) {
        this.streetName = streetName;
        this.buildingNumber = buildingNumber;
        this.cityName = cityName;
        this.postalCode = postalCode;
        this.country = country;
        this.district = district;
        this.region = region;
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

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(streetName, address.streetName) &&
                Objects.equals(buildingNumber, address.buildingNumber) &&
                Objects.equals(cityName, address.cityName) &&
                Objects.equals(postalCode, address.postalCode) &&
                Objects.equals(country, address.country) &&
                Objects.equals(district, address.district) &&
                Objects.equals(region, address.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetName, buildingNumber, cityName, postalCode, country, district, region);
    }
}
