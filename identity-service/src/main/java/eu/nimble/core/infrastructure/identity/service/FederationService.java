package eu.nimble.core.infrastructure.identity.service;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.system.dto.oauth.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class FederationService {

    @Value("${nimble.oauth.federationClient.accessTokenUri}")
    private String accessTokenUri;

    @Value("${nimble.oauth.federationClient.clientId}")
    private String clientId;

    @Value("${nimble.oauth.federationClient.clientSecret}")
    private String clientSecret;

    @Value("${nimble.oauth.federationClient.redirectUri}")
    private String redirectUri;

    public Token exchangeToken(String trustedToken) {
        // TODO verify the signature
        long exp = JWT.decode(trustedToken).getClaim(GlobalConstants.JWT_EXPIRY_ATTRIBUTE_STRING).asLong();

        return getAccessToken(null, GlobalConstants.CLIENT_CREDENTIALS_FLOW, null);
    }

    public boolean verifyToken(String accessTokenUri) {
        try {
            long exp = JWT.decode(accessTokenUri).getClaim(GlobalConstants.JWT_EXPIRY_ATTRIBUTE_STRING).asLong();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Token getAccessToken(String code, String grantType, String refreshToken) {
        Token token = new Token();
        String url = accessTokenUri;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();

        if (GlobalConstants.AUTHORIZATION_CODE_FLOW.equals(grantType)) {
            map.add("grant_type", GlobalConstants.AUTHORIZATION_CODE_FLOW);
            map.add("code", code);
            map.add("redirect_uri", redirectUri);
        } else if (GlobalConstants.CLIENT_CREDENTIALS_FLOW.equals(grantType)) {
            map.add("grant_type", GlobalConstants.CLIENT_CREDENTIALS_FLOW);
        } else if (GlobalConstants.REFRESH_TOKEN_FLOW.equals(grantType)) {
            map.add("grant_type", GlobalConstants.REFRESH_TOKEN_FLOW);
            map.add(GlobalConstants.REFRESH_TOKEN_FLOW, refreshToken);
        }
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        try {

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            token = mapper.readValue(response.getBody(), Token.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return token;
    }
}
