package eu.nimble.core.infrastructure.identity.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;

import java.util.Map;

/**
 * Created by Johannes Innerbichler on 06/07/17.
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PaymentMeans {

    private Map<NimbleConfigurationProperties.LanguageID, String> instructionNote;

    public PaymentMeans() {
    }

    public PaymentMeans(Map<NimbleConfigurationProperties.LanguageID, String> instructionNote) {
        this.instructionNote = instructionNote;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getInstructionNote() {
        return instructionNote;
    }

    public void setInstructionNote(Map<NimbleConfigurationProperties.LanguageID, String> instructionNote) {
        this.instructionNote = instructionNote;
    }

}
