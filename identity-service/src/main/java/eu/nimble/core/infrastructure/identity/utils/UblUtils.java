package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.service.model.ubl.commonbasiccomponents.IdentifierType;

/**
 * Created by Johannes Innerbichler on 27/06/17.
 */
public class UblUtils {
    public static IdentifierType identifierType(String id) {
        IdentifierType idType = new IdentifierType();
        idType.setValue(id);
        return idType;
    }
    public static IdentifierType identifierType(Long id) {
        return identifierType(id.toString());
    }
}
