package org.example;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AirQualityApi {
    private final String apiKey;

    public AirQualityApi() {
        this.apiKey = System.getenv("GOOGLE_AIR_QUALITY_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new IllegalStateException("GOOGLE_AIR_QUALITY_API_KEY environment variable is not set");
        }
    }

    public String fetchData(double latitude, double longitude) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = createJsonPayloadForAQIRequest(latitude, longitude);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://airquality.googleapis.com/v1/currentConditions:lookup?key=" + this.apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String createJsonPayloadForAQIRequest(double latitude, double longitude) {
        return "{"
                + "\"location\": {"
                + "\"latitude\": " + latitude + ","
                + "\"longitude\": " + longitude
                + "},"
                + "\"extraComputations\": ["
                + "\"HEALTH_RECOMMENDATIONS\","
                + "\"POLLUTANT_CONCENTRATION\""
                + "],"
                + "\"uaqiColorPalette\": \"COLOR_PALETTE_UNSPECIFIED\","
                + "\"universalAqi\": true,"
                + "\"languageCode\": \"en\""
                + "}";
    }


    public double extractPM25Value(String jsonResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // Check for an error in the response
        if (jsonObject.has("error")) {
            String errorMessage = jsonObject.getJSONObject("error").getString("message");
            throw new JSONException("API Error: " + errorMessage);
        }

        // Check if the pollutants key exists
        if (!jsonObject.has("pollutants")) {
            throw new JSONException("JSONObject['pollutants'] not found.");
        }

        JSONArray pollutants = jsonObject.getJSONArray("pollutants");

        for (int i = 0; i < pollutants.length(); i++) {
            JSONObject pollutant = pollutants.getJSONObject(i);
            if ("pm25".equals(pollutant.getString("code"))) {
                return pollutant.getJSONObject("concentration").getDouble("value");
            }
        }
        return -1; // or handle this case appropriately
    }

}
