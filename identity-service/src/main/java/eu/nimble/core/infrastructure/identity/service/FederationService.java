package eu.nimble.core.infrastructure.identity.service;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.system.dto.oauth.Token;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
@Service
public class FederationService {

    private final Logger logger = LoggerFactory.getLogger(FederationService.class);

    @Value("${nimble.oauth.federationClient.accessTokenUri}")
    private String accessTokenUri;

    @Value("${nimble.oauth.federationClient.clientId}")
    private String clientId;

    @Value("${nimble.oauth.federationClient.clientSecret}")
    private String clientSecret;

    @Value("${nimble.oauth.federationClient.redirectUri}")
    private String redirectUri;

    @Value("${nimble.oauth.eFactoryClient.clientId}")
    private String eFactoryClientId;

    @Value("${nimble.oauth.eFactoryClient.clientSecret}")
    private String eFactoryClientSecret;

    @Value("${nimble.oauth.eFactoryClient.accessTokenUri}")
    private String eFactoryAccessTokenUri;

    @Value("${nimble.oauth.eFactoryClient.userDetailsUri}")
    private String eFactoryUserDetailsUri;

    public Token exchangeToken(String trustedToken) {
        // TODO verify the signature
        long exp = JWT.decode(trustedToken).getClaim(GlobalConstants.JWT_EXPIRY_ATTRIBUTE_STRING).asLong();

        return getAccessToken(null, GlobalConstants.CLIENT_CREDENTIALS_FLOW, null, redirectUri);
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

    public Token getAccessToken(String code, String grantType, String refreshToken, String redirectURL) {
        Token token = new Token();
        String url = accessTokenUri;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();

        if (GlobalConstants.AUTHORIZATION_CODE_FLOW.equals(grantType)) {
            map.add("grant_type", GlobalConstants.AUTHORIZATION_CODE_FLOW);
            map.add("code", code);
            if (redirectURL == null) {
                map.add("redirect_uri", redirectUri);
            }else {
                map.add("redirect_uri", redirectURL);
            }
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
        } catch (HttpClientErrorException e) {
            logger.error("Failed to retrieve access token: {} ",e.getResponseBodyAsString());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return token;
    }


    public String getEFactoryUserVatAttribute(String eFactoryUserId){
        Token token = new Token();
        String url = eFactoryAccessTokenUri;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();

        map.add("grant_type", GlobalConstants.CLIENT_CREDENTIALS_FLOW);
        map.add("client_id", eFactoryClientId);
        map.add("client_secret", eFactoryClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            token = mapper.readValue(response.getBody(), Token.class);

            HttpResponse<String> httpResponse = Unirest.get(eFactoryUserDetailsUri + "/"+eFactoryUserId)
                    .header("Authorization","Bearer "+token.getAccess_token()).asString();
            UserRepresentation userRepresentation  = mapper.readValue(httpResponse.getBody(), UserRepresentation.class);
            if(userRepresentation.getAttributes() != null) {
                List<String> vatNumbers = userRepresentation.getAttributes().get("vatin");
                if(vatNumbers != null && vatNumbers.size() > 0){
                    return vatNumbers.get(0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
