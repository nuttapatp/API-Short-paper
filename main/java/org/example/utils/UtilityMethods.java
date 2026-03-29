package org.example.utils;

public class UtilityMethods {

    public static int convertPM25ToAQI(double pm25) {
        if (pm25 >= 0 && pm25 <= 12) {
            return calculateAQI(pm25, 0, 12, 0, 50);
        } else if (pm25 > 12 && pm25 <= 35.4) {
            return calculateAQI(pm25, 12.1, 35.4, 51, 100);
        } else if (pm25 > 35.4 && pm25 <= 55.4) {
            return calculateAQI(pm25, 35.5, 55.4, 101, 150);
        } else if (pm25 > 55.4 && pm25 <= 150.4) {
            return calculateAQI(pm25, 55.5, 150.4, 151, 200);
        } else if (pm25 > 150.4 && pm25 <= 250.4) {
            return calculateAQI(pm25, 150.5, 250.4, 201, 300);
        } else if (pm25 > 250.4 && pm25 <= 350.4) {
            return calculateAQI(pm25, 250.5, 350.4, 301, 400);
        } else if (pm25 > 350.4 && pm25 <= 500.4) {
            return calculateAQI(pm25, 350.5, 500.4, 401, 500);
        } else {
            // PM2.5 concentrations above 500.4 µg/m3 are beyond the AQI scale
            return 500;
        }
    }

    private static int calculateAQI(double C, double Clow, double Chigh, int Ilow, int Ihigh) {
        double I = (Ihigh - Ilow) / (Chigh - Clow) * (C - Clow) + Ilow;
        return (int) Math.round(I);
    }
}