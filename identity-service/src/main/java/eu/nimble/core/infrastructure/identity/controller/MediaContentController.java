package eu.nimble.core.infrastructure.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Johannes Innerbichler on 2019-01-07.
 */
@Controller
@RequestMapping(path = "/media")
public class MediaContentController {

    private static Logger logger = LoggerFactory.getLogger(MediaContentController.class);

    @Autowired
    private BinaryContentService binaryContentService;

    @Value("${nimble.binary-content.url}")
    private String binaryContentUrl;

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/{mediaID}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getContent(@PathVariable(value = "mediaID") String mediaID,
                           HttpServletResponse response) {
        String uri = binaryContentUrl + mediaID;

        try {
            logger.info("Request to retrieve raw binary content for uri: {}", uri);

            BinaryObjectType result = binaryContentService.retrieveContent(uri);
            // check whether the binary content exists or not
            if (result == null) {
                String msg = String.format("There does not exist a binary content for uri: %s", uri);
                logger.error(msg);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                try {
                    response.getOutputStream().write(msg.getBytes());
                } catch (IOException e1) {
                    logger.error("Failed to write the error message to the output stream", e1);
                }
            }
            try {
                response.setHeader("Content-disposition", "attachment; filename=" + result.getFileName());
                response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                IOUtils.write(result.getValue(), response.getOutputStream());
                response.flushBuffer();

                logger.debug("Completed the request to retrieve raw binary content for uri: {}", uri);

            } catch (IOException e) {
                String msg = String.format("Failed to write the raw binary content to the response output stream for uri: %s\n%s", uri, e.getMessage());
                logger.error(msg, e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                try {
                    response.getOutputStream().write(msg.getBytes());
                } catch (IOException e1) {
                    logger.error("Failed to write the error message to the output stream", e);
                }
            }

        } catch (Exception e) {
            String msg = String.format("Unexpected error while retrieving the raw content for uri: %s\n%s", uri, e.getMessage());
            logger.error(msg, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getOutputStream().write(msg.getBytes());
            } catch (IOException e1) {
                logger.error("Failed to write the error message to the output stream", e);
            }
        }
    }
}