package eu.nimble.core.infrastructure.identity.uaa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenIdConnectUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private List<String> realmRoles;
    private List<String> resourcesRoles;
    private List<GrantedAuthority> grantedAuthorities;

    @SuppressWarnings("unchecked")
    public OpenIdConnectUserDetails(String oidToken) throws IOException {

        Jwt tokenDecoded = JwtHelper.decode(oidToken);
        Map<String, Object> authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);

        this.userId = (String) authInfo.get("sub");
        this.username = (String) authInfo.get("email");


        realmRoles = (List<String>) ((Map<String, List>) authInfo.get("realm_access")).get("roles");
        resourcesRoles = (List<String>) ((Map<String, Map<String, List>>) authInfo.get("resource_access")).get("account").get("roles");
        grantedAuthorities = realmRoles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    public static OpenIdConnectUserDetails fromBearer(String bearer) throws IOException {
        return new OpenIdConnectUserDetails(bearer.replace("Bearer ", ""));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    public Boolean hasRole(String role) {
        return grantedAuthorities.stream().map(GrantedAuthority::getAuthority).anyMatch(r -> r.equals(role));
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public List<String> getResourcesRoles() {
        return resourcesRoles;
    }
}