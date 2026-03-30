package org.example.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UtilityMethodsTest {

    @Test
    void goodAirQuality_returnsAQIBelow50() {
        int aqi = UtilityMethods.convertPM25ToAQI(0);
        assertEquals(0, aqi);
    }

    @Test
    void pm25AtUpperGoodBound_returns50() {
        int aqi = UtilityMethods.convertPM25ToAQI(12);
        assertEquals(50, aqi);
    }

    @Test
    void pm25InModerateRange_returnsAQI51to100() {
        int aqi = UtilityMethods.convertPM25ToAQI(20);
        assertTrue(aqi >= 51 && aqi <= 100, "Expected AQI 51-100 but got " + aqi);
    }

    @Test
    void pm25InUnhealthySensitiveRange_returnsAQI101to150() {
        int aqi = UtilityMethods.convertPM25ToAQI(45);
        assertTrue(aqi >= 101 && aqi <= 150, "Expected AQI 101-150 but got " + aqi);
    }

    @Test
    void pm25InUnhealthyRange_returnsAQI151to200() {
        int aqi = UtilityMethods.convertPM25ToAQI(100);
        assertTrue(aqi >= 151 && aqi <= 200, "Expected AQI 151-200 but got " + aqi);
    }

    @Test
    void pm25InVeryUnhealthyRange_returnsAQI201to300() {
        int aqi = UtilityMethods.convertPM25ToAQI(200);
        assertTrue(aqi >= 201 && aqi <= 300, "Expected AQI 201-300 but got " + aqi);
    }

    @Test
    void pm25AboveScale_returns500() {
        int aqi = UtilityMethods.convertPM25ToAQI(600);
        assertEquals(500, aqi);
    }

    @ParameterizedTest(name = "PM2.5={0} should give AQI={1}")
    @CsvSource({
        "0, 0",
        "12, 50",
        "35.4, 100",
        "55.4, 150",
        "150.4, 200"
    })
    void boundaryValues_returnExpectedAQI(double pm25, int expectedAqi) {
        int aqi = UtilityMethods.convertPM25ToAQI(pm25);
        assertEquals(expectedAqi, aqi, "PM2.5=" + pm25 + " expected AQI=" + expectedAqi);
    }
}
