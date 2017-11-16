package eu.nimble.core.infrastructure.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
//@Table(uniqueConstraints = {
//        @UniqueConstraint(columnNames = {"email", "companyId"})
//})
public class UserInvitation {

    @Id
    @JsonIgnore
    @GeneratedValue
    long id;

    @Email
    @NotBlank
    @JsonProperty(required = true)
    private String email;

    @NotBlank
    @JsonProperty(required = true)
    private String companyId;

    @ManyToOne
    @JsonIgnore
    private UaaUser sender;

    protected UserInvitation() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public UserInvitation(String email, String companyId, UaaUser sender) {
        this.email = email;
        this.companyId = companyId;
        this.sender = sender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public UaaUser getSender() {
        return sender;
    }

    public void setSender(UaaUser sender) {
        this.sender = sender;
    }
}
