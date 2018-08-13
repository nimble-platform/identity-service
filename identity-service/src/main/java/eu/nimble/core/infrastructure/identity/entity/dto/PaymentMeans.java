package eu.nimble.core.infrastructure.identity.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Johannes Innerbichler on 06/07/17.
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PaymentMeans {

    private String instructionNote;

    public PaymentMeans() {
    }

    public PaymentMeans(String instructionNote) {
        this.instructionNote = instructionNote;
    }

    public String getInstructionNote() {
        return instructionNote;
    }

    public void setInstructionNote(String instructionNote) {
        this.instructionNote = instructionNote;
    }

}
