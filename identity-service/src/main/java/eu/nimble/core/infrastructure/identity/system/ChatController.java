package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.entity.dto.Credentials;
import eu.nimble.core.infrastructure.identity.entity.dto.FrontEndUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.service.RocketChatService;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.channel.CreateChannelRequest;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list.ChatUsers;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.register.RockerChatRegisterResponse;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nirojan Selvanathan on 05.06.19.
 */

@Controller
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RocketChatService chatService;

    @Value("${nimble.rocketChat.isEnabled}")
    private boolean isChatEnabled;


    @ApiOperation(value = "Create a communication createChannelRequest for the users to negotiate", response = FrontEndUser.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CreateChannelRequest Created"),
            @ApiResponse(code = 400, message = "Bad Request")})
    @RequestMapping(value = "/chat/createChannel", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<CreateChannelRequest> createNegotiationChannel(
            @ApiParam(value = "CreateChannelRequest creation request containing collaborating organization details", required = true) @RequestBody CreateChannelRequest createChannelRequest) {

        // Find the initiating party members email list
        PartyType initiatingCompany = partyRepository.findByHjid(Long.valueOf(createChannelRequest.getInitiatingPartyID()))
                .stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        List<PersonType> initiatingPeopleList = initiatingCompany.getPerson();

        // Find the responding party members email list
        PartyType respondingCompany = partyRepository.findByHjid(Long.valueOf(createChannelRequest.getRespondingPartyID()))
                .stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        List<PersonType> respondingPeopleList = respondingCompany.getPerson();

        List<String> initiatingCompanyMembersList = new ArrayList<>();

        Map<String, PersonType> emailToPersonTypeMap = new HashMap<>();

        // Add all the members who should be added to the channel to a map
        for (PersonType p : initiatingPeopleList) {
            initiatingCompanyMembersList.add(p.getContact().getElectronicMail());
            emailToPersonTypeMap.put(p.getContact().getElectronicMail(), p);
        }

        List<String> respondingCompanyMembersList = new ArrayList<>();
        for (PersonType p : respondingPeopleList) {
            respondingCompanyMembersList.add(p.getContact().getElectronicMail());
            emailToPersonTypeMap.put(p.getContact().getElectronicMail(), p);
        }

        // Create a unique name to the channel, Rocket.Chat channel cannot be created with whitespace
        String channelName = createChannelRequest.getProductName().replaceAll(" ", "_").toLowerCase()
                + ((int) (Math.random() * 100));
        createChannelRequest.setChannelName(channelName);

        List<String> channelMembersUsernameList = new ArrayList<>();

        // Get the list of the users in Rocket.Chat to obtain the user's Rocket.Chat username
        ChatUsers chatUsers = chatService.listUsers();

        // Find the users who are not yet registered in Rocket.Chat
        Map<String, String> map = chatService.getMissingEmails(chatUsers, initiatingCompanyMembersList, respondingCompanyMembersList);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (null == value) {
                // create a user in Rocket.Chat
                PersonType personType = emailToPersonTypeMap.get(key);
                FrontEndUser frontEndUser = new FrontEndUser();
                frontEndUser.setFirstname(personType.getFirstName());
                frontEndUser.setLastname(personType.getFamilyName());
                frontEndUser.setUsername(key);

                Credentials c = new Credentials();
                c.setUsername(key);

                RockerChatRegisterResponse rockerChatRegisterResponse = chatService.registerUser(frontEndUser, c, false);
                map.put(key, rockerChatRegisterResponse.getUser().getUsername());
                channelMembersUsernameList.add(rockerChatRegisterResponse.getUser().getUsername());
            }else {
                channelMembersUsernameList.add(value);
            }
        }

        CreateChannelRequest channel = chatService.createChannel(createChannelRequest, channelMembersUsernameList);
        return new ResponseEntity<>(channel, HttpStatus.OK);
    }
}
