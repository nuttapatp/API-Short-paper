package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirQualityApiTest {

    @Test
    void constructor_throwsWhenApiKeyIsNull() {
        assertThrows(IllegalStateException.class, () -> new AirQualityApi(null));
    }

    @Test
    void constructor_throwsWhenApiKeyIsEmpty() {
        assertThrows(IllegalStateException.class, () -> new AirQualityApi(""));
    }

    @Test
    void constructor_succeedsWithValidApiKey() {
        assertDoesNotThrow(() -> new AirQualityApi("test-key-123"));
    }

    @Test
    void extractPM25Value_parsesValidResponse() throws Exception {
        AirQualityApi api = new AirQualityApi("test-key");
        String json = """
                {
                  "pollutants": [
                    {
                      "code": "pm25",
                      "concentration": {
                        "value": 35.5,
                        "units": "MICROGRAMS_PER_CUBIC_METER"
                      }
                    }
                  ]
                }
                """;
        double pm25 = api.extractPM25Value(json);
        assertEquals(35.5, pm25, 0.001);
    }

    @Test
    void extractPM25Value_returnsMinus1WhenPm25NotFound() throws Exception {
        AirQualityApi api = new AirQualityApi("test-key");
        String json = """
                {
                  "pollutants": [
                    {
                      "code": "o3",
                      "concentration": { "value": 10.0 }
                    }
                  ]
                }
                """;
        double result = api.extractPM25Value(json);
        assertEquals(-1, result);
    }

    @Test
    void extractPM25Value_throwsOnApiError() {
        AirQualityApi api = new AirQualityApi("test-key");
        String json = """
                {
                  "error": {
                    "code": 403,
                    "message": "API key invalid"
                  }
                }
                """;
        assertThrows(Exception.class, () -> api.extractPM25Value(json));
    }
}
