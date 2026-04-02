package org.example.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveCityTest {

    // Test resolveCity via reflection (private method)
    private String resolveCity(double lat, double lon) throws Exception {
        LineController controller = new LineController(null, null, null, null);
        Method method = LineController.class.getDeclaredMethod("resolveCity", double.class, double.class);
        method.setAccessible(true);
        return (String) method.invoke(controller, lat, lon);
    }

    @ParameterizedTest(name = "lat={0}, lon={1} → {2}")
    @CsvSource({
        "18.79, 98.99, Chiang Mai",   // Chiang Mai city center
        "19.50, 99.00, Chiang Mai",   // Northern Chiang Mai
        "7.88,  98.39, Phuket",       // Phuket city
        "13.00, 100.90, Chonburi",    // Chonburi / Pattaya area
        "13.75, 100.50, Bangkok",     // Bangkok city center
        "14.00, 100.20, Bangkok",     // Central Thailand — Bangkok
    })
    void resolveCity_returnsCorrectCity(double lat, double lon, String expectedCity) throws Exception {
        assertEquals(expectedCity, resolveCity(lat, lon));
    }

    @Test
    void resolveCity_unknownLocation_defaultsBangkok() throws Exception {
        assertEquals("Bangkok", resolveCity(16.0, 103.0)); // Khon Kaen — not covered
    }
}
