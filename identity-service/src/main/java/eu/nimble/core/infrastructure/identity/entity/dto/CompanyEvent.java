package eu.nimble.core.infrastructure.identity.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.google.api.client.util.DateTime;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * Created by Johannes Innerbichler on 25.09.18.
 */
public class CompanyEvent {

    @ApiModelProperty(value = "Name of event")
    private String name = null;

    @ApiModelProperty(value = "Location of event")
    private Address place = null;

    @ApiModelProperty(value = "Start date of event")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateFrom = null;

    @ApiModelProperty(value = "End date of event")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateTo = null;

    @ApiModelProperty(value = "Description of event")
    private String description = null;

    public CompanyEvent() {
    }

    public CompanyEvent(String name, Address place, Date dateFrom, Date dateTo, String description) {
        this.name = name;
        this.place = place;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getPlace() {
        return place;
    }

    public void setPlace(Address place) {
        this.place = place;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}