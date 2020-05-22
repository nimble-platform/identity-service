package eu.nimble.core.infrastructure.identity.utils;

import com.google.common.collect.Sets;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualityIndicatorType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.extension.QualityIndicatorParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Johannes Innerbichler on 27/06/17.
 */
@Component
public class UblUtils {

    @Autowired NimbleConfigurationProperties nimbleConfiguration;

        public static <V> V emptyUBLObject(V object) {
        try {
            Set<String> packages = Sets.newHashSet("eu.nimble.service.model.ubl");
            initialize(object, object, packages);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return object;
    }

    public static String getText(Collection<TextType> textType, NimbleConfigurationProperties.LanguageID languageID) {
        // get text for the given language id
        String text = textType.stream().filter(tt -> tt.getLanguageID().equals(languageID.toString())).map(TextType::getValue).findFirst().orElse(null);
        // if it's not available, then get the text for English
        if(text == null){
            text = textType.stream().filter(tt -> tt.getLanguageID().equals(NimbleConfigurationProperties.LanguageID.ENGLISH.toString())).map(TextType::getValue).findFirst().orElse(null);
            // if a text is not available for english as well, simply return the first one
            if(text == null){
                return getText(textType);
            }
        }
        return text;
    }

    public static String getText(Collection<TextType> textType) {
        return textType.stream().filter(tt -> !tt.getValue().isEmpty()).map(TextType::getValue).findFirst().orElse(null);
    }

    public static String getName(Collection<PartyNameType> partyNameTypes, NimbleConfigurationProperties.LanguageID languageID) {
        List<TextType> textTypes = partyNameTypes.stream().map(PartyNameType::getName).collect(Collectors.toList());
        return getText(textTypes, languageID);
    }

    public static String getName(Collection<PartyNameType> partyNameTypes) {
        List<TextType> textTypes = partyNameTypes.stream().map(PartyNameType::getName).collect(Collectors.toList());
        return getText(textTypes);
    }


    /**
     * This method will return the party name (legal name) of a company, preference will be given to the default language ID
     * @param partyType
     * @return
     */
    public String getName(PartyType partyType) {
        String name = getName(partyType.getPartyName(), nimbleConfiguration.getDefaultLanguageID());
        if(name == null){
            name = getName(partyType.getPartyName());
        }
        return name;
    }

    public static PartyType setID(PartyType party, String identifier ) {
        party.getPartyIdentification().clear();
        party.getPartyIdentification().add(UblAdapter.adaptPartyIdentifier(identifier));
        return party;
    }

    private static void initialize(Object object, Object rootObject, Set<String> packages) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldClass = field.getType();

            // skip same object as root to avoid infinite loops
            if( fieldClass.equals(rootObject.getClass()))
                continue;

            // skip primitives
            if (fieldClass.isPrimitive())
                continue;

            // skip if not in packages
            boolean inPackage = false;
            for (String pack : packages) {
                if (fieldClass.getPackage().getName().startsWith(pack)) {
                    inPackage = true;
                }
            }

            if (!inPackage)
                continue;

            // allow access to private fields
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);

            Object fieldValue = field.get(object);
            if (fieldValue == null) {
                try {
                    field.set(object, fieldClass.newInstance());
                } catch (IllegalArgumentException | IllegalAccessException
                        | InstantiationException e) {
                    System.err.println("Could not initialize " + fieldName + " "
                            + fieldClass.getSimpleName());
                }
            }

            fieldValue = field.get(object);

            // reset accessible
            field.setAccessible(isAccessible);

            // recursive call for sub-objects
            initialize(fieldValue, rootObject, packages);
        }
    }

    public static Optional<QualityIndicatorType> extractQualityIndicator(PartyType party, QualityIndicatorParameter parameter) {
        return party.getQualityIndicator().stream()
                .filter(qi -> parameter.toString().equals(qi.getQualityParameter()))
                .findFirst();
    }

    public static <V> List<V> toModifyableList(V... objects) {
        return new ArrayList<>(Arrays.asList(objects));
    }
}
