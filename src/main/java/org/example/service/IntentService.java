package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class IntentService {

    private static final Logger log = LoggerFactory.getLogger(IntentService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    public enum Intent {
        AQI_NOW,        // "อากาศตอนนี้เป็นยังไง", "AQI วันนี้"
        AQI_FORECAST,   // "พรุ่งนี้อากาศดีมั้ย"
        BEST_DAY,       // "วันไหนอากาศดีที่สุดในสัปดาห์"
        SET_PROFILE,    // "ฉันเป็นโรคหอบหืด", "บ้านมีเด็กเล็ก"
        DELETE_PROFILE, // "ลบข้อมูลฉัน"
        UNKNOWN         // ข้อความอื่นๆ
    }

    private final String apiKey;
    private final HttpClient httpClient;

    public IntentService(@Value("${ANTHROPIC_API_KEY}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Intent classify(String userMessage) {
        try {
            String prompt = "จำแนกประเภทของข้อความนี้: \\\"" + escapeJson(userMessage) + "\\\"\\n\\n"
                    + "ตอบด้วยคำเดียวเท่านั้น (ห้ามมีข้อความอื่น):\\n"
                    + "- AQI_NOW: ถ้าถามเกี่ยวกับ AQI หรืออากาศตอนนี้/วันนี้\\n"
                    + "- AQI_FORECAST: ถ้าถามเกี่ยวกับอากาศพรุ่งนี้หรืออนาคต\\n"
                    + "- BEST_DAY: ถ้าถามว่าวันไหนอากาศดีที่สุดในสัปดาห์\\n"
                    + "- SET_PROFILE: ถ้าบอกข้อมูลสุขภาพหรือข้อมูลส่วนตัว\\n"
                    + "- DELETE_PROFILE: ถ้าต้องการลบหรือลืมข้อมูลสุขภาพ\\n"
                    + "- UNKNOWN: ถ้าไม่เข้าข้อใดข้างต้น";

            String requestBody = "{\"model\":\"claude-haiku-4-5-20251001\","
                    + "\"max_tokens\":20,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                int textStart = body.indexOf("\"text\":\"") + 8;
                int textEnd = body.indexOf("\"", textStart);
                String result = body.substring(textStart, textEnd).trim();
                log.info("Intent classified as: {} for message: {}", result, userMessage);
                return Intent.valueOf(result);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown intent from Claude for: {}", userMessage);
        } catch (Exception e) {
            log.error("Intent classification error", e);
        }
        return Intent.UNKNOWN;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
