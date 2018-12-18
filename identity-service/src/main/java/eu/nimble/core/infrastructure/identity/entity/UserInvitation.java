package eu.nimble.core.infrastructure.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "companyId"})
})
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

    @Column
    @ElementCollection(targetClass = String.class)
    private List<String> roleIDs;

    @ManyToOne
    @JsonIgnore
    private UaaUser sender;

    private Boolean isPending;

    protected UserInvitation() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public UserInvitation(String email, String companyId, List<String> roleIDs, UaaUser sender) {
        this(email, companyId, roleIDs, sender, true);
    }

    public UserInvitation(String email, String companyId, List<String> roleIDs, UaaUser sender, Boolean isPending) {
        this.email = email;
        this.companyId = companyId;
        this.sender = sender;
        this.roleIDs = roleIDs;

        this.isPending = isPending;
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

    public List<String> getRoleIDs() {
        return roleIDs;
    }

    public void setRoleIDs(List<String> roleIDs) {
        this.roleIDs = roleIDs;
    }

    public UaaUser getSender() {
        return sender;
    }

    public void setSender(UaaUser sender) {
        this.sender = sender;
    }

    public Boolean getPending() {
        return isPending;
    }

    public void setPending(Boolean pending) {
        isPending = pending;
    }
}
