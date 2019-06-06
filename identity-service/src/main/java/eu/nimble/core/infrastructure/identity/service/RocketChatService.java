package eu.nimble.core.infrastructure.identity.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.core.infrastructure.identity.entity.dto.Credentials;
import eu.nimble.core.infrastructure.identity.entity.dto.FrontEndUser;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.RockerChatRegisterResponse;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.RocketChatLoginResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Created by Nirojan Selvanathan on 28.05.18.
 */
@Service
public class RocketChatService {

    @Value("${nimble.rocketChat.url}")
    private String rocketChatURL;


    public void registerUser(FrontEndUser frontEndUser, Credentials credentials, boolean userNameAlreadyExists) {
        new Thread(() -> {
            RestTemplate rs = new RestTemplate();
            String uri = rocketChatURL + "/api/v1/users.register";

            String temp = credentials.getUsername().substring(credentials.getUsername().indexOf("@") + 1); // e.g. @google.com
            String companyEmailName = temp.substring(0, temp.indexOf("."));

            String rocketChatUserName = frontEndUser.getFirstname() + "." + frontEndUser.getLastname() + "." + companyEmailName;
            if (userNameAlreadyExists) {
                rocketChatUserName = rocketChatUserName + String.valueOf((int) (Math.random() * 10));
            }

            JSONObject request = new JSONObject();
            request.put("email", credentials.getUsername());
            request.put("pass", credentials.getPassword());
            request.put("username", rocketChatUserName);
            request.put("name", frontEndUser.getFirstname() + " " + frontEndUser.getLastname());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

            ResponseEntity<String> loginResponse = rs.exchange(uri, HttpMethod.POST, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            RockerChatRegisterResponse rockerChatRegisterResponse = new RockerChatRegisterResponse();
            try {
                rockerChatRegisterResponse= mapper.readValue(loginResponse.getBody(), RockerChatRegisterResponse.class);
                if (rockerChatRegisterResponse.isSuccess() && rockerChatRegisterResponse.getError().equals("Username is already in use")) {
                    registerUser(frontEndUser, credentials, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String loginUser(FrontEndUser frontEndUser, Credentials credentials) {

        RestTemplate rs = new RestTemplate();
        String uri = rocketChatURL + "/api/v1/login";

        JSONObject request = new JSONObject();
        request.put("email", credentials.getUsername());
        request.put("password", credentials.getPassword());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RocketChatLoginResponse rcs;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

        try {
            ResponseEntity<String> loginResponse = rs.exchange(uri, HttpMethod.POST, entity, String.class);
            rcs= mapper.readValue(loginResponse.getBody(), RocketChatLoginResponse.class);
            return rcs.getData().getAuthToken();
        } catch (HttpStatusCodeException exception){
            if(exception.getStatusCode().value() == 401){
                registerUser(frontEndUser, credentials, false);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
