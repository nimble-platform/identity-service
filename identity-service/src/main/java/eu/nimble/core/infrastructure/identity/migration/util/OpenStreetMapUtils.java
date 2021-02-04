package eu.nimble.core.infrastructure.identity.migration.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CoordinateType;
import org.apache.log4j.Logger;

public class OpenStreetMapUtils {

    public final static Logger log = Logger.getLogger(OpenStreetMapUtils.class);

    private static OpenStreetMapUtils instance = null;
    private JsonParser jsonParser;

    public OpenStreetMapUtils() {
        jsonParser = new JsonParser();
    }

    public static OpenStreetMapUtils getInstance() {
        if (instance == null) {
            instance = new OpenStreetMapUtils();
        }
        return instance;
    }

    private String getRequest(String url) throws Exception {
        HttpResponse<String> response = Unirest.get(url)
                .header("accept", "*/*")
                .asString();

        if (response.getStatus() != 200) {
            return null;
        }

        return response.getBody();
    }

    public CoordinateType getCoordinates(String address) {
        CoordinateType coordinate = new CoordinateType();
        // create the query
        String[] split = address.split(" ");
        if (split.length == 0) {
            return null;
        }

        StringBuilder query = new StringBuilder();

        query.append("http://nominatim.openstreetmap.org/search?q=");

        for (int i = 0; i < split.length; i++) {
            query.append(split[i]);
            if (i < (split.length - 1)) {
                query.append("+");
            }
        }
        query.append("&format=json&addressdetails=1");

        String response = null;
        // get results
        try {
            response = getRequest(query.toString());
        } catch (Exception e) {
            log.error("Error while retrieving data with the following query " + query);
        }

        if (response == null) {
            return null;
        }
        // parse the response
        JsonArray obj = jsonParser.parse(response).getAsJsonArray();

        if (obj.size() > 0) {
            JsonObject jsonObject = obj.get(0).getAsJsonObject();

            coordinate.setLatitude(jsonObject.get("lat").getAsBigDecimal());
            coordinate.setLongitude(jsonObject.get("lon").getAsBigDecimal());

        }

        return coordinate;
    }
}