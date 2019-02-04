package eu.nimble.core.infrastructure.identity.utils;

import com.google.common.collect.Sets;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualityIndicatorType;
import eu.nimble.service.model.ubl.extension.QualityIndicatorParameter;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Johannes Innerbichler on 27/06/17.
 */
public class UblUtils {
    public static String identifierType(String id) {
        return id;
    }

    public static String identifierType(Long id) {
        return identifierType(id.toString());
    }

    public static <V> V emptyUBLObject(V object) {
        try {
            Set<String> packages = Sets.newHashSet("eu.nimble.service.model.ubl");
            initialize(object, object, packages);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return object;
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
