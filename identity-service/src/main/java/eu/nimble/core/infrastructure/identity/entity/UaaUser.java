package eu.nimble.core.infrastructure.identity.entity;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Created by Johannes Innerbichler on 29/05/17.
 * Entity storing user credentials.
 */
@Entity
public class UaaUser implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToOne
    private PersonType ublPerson;


    protected UaaUser() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public UaaUser(String username, String password, PersonType ublPerson) {
        this.username = username;
        this.password = password;
        this.ublPerson = ublPerson;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public PersonType getUBLPerson() {
        return this.ublPerson;
    }
}
