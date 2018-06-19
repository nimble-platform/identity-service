package eu.nimble.core.infrastructure.identity.entity;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Created by Johannes Innerbichler on 29/05/17.
 * Entity storing user credentials.
 */
@Entity
public class UaaUser implements Serializable {

    @Id
    @Column(nullable = false, unique = true)
    private String externalID;

    private String username;

    @OneToOne
    private PersonType ublPerson;

    @ColumnDefault("false")
    private Boolean showWelcomeInfo = false;

    protected UaaUser() {
    }

    public UaaUser(String username, PersonType ublPerson, String externalID) {
        this.username = username;
        this.ublPerson = ublPerson;
        this.externalID = externalID;
        this.showWelcomeInfo = true;
    }

    public String getUsername() {
        return username;
    }

    public String getExternalID() {
        return externalID;
    }

    public PersonType getUBLPerson() {
        return this.ublPerson;
    }

    public Boolean getShowWelcomeInfo() {
        return showWelcomeInfo;
    }

    public void setShowWelcomeInfo(Boolean showWelcomeInfo) {
        this.showWelcomeInfo = showWelcomeInfo;
    }
}
