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
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    private final String apiKey;
    private final HttpClient httpClient;

    public ClaudeService(@Value("${ANTHROPIC_API_KEY}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String generateAqiNotification(int aqi, String healthProfile) {
        try {
            String prompt = buildPrompt(aqi, healthProfile);
            String requestBody = String.format("""
                    {
                      "model": "claude-haiku-4-5-20251001",
                      "max_tokens": 300,
                      "messages": [
                        {"role": "user", "content": %s}
                      ]
                    }
                    """, toJsonString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromResponse(response.body());
            } else {
                log.error("Claude API error status {}: {}", response.statusCode(), response.body());
                return buildFallbackMessage(aqi);
            }
        } catch (Exception e) {
            log.error("Claude API error, falling back to default message", e);
            return buildFallbackMessage(aqi);
        }
    }

    private String extractTextFromResponse(String responseBody) {
        // Parse: {"content":[{"type":"text","text":"..."}],...}
        int textStart = responseBody.indexOf("\"text\":\"") + 8;
        int textEnd = responseBody.indexOf("\"", textStart);
        if (textStart > 8 && textEnd > textStart) {
            return responseBody.substring(textStart, textEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        }
        return buildFallbackMessage(-1);
    }

    private String buildPrompt(int aqi, String healthProfile) {
        String aqiLevel = getAqiLevel(aqi);
        String profileContext = (healthProfile != null && !healthProfile.isBlank())
                ? "ข้อมูลสุขภาพผู้ใช้: " + healthProfile
                : "ไม่มีข้อมูลสุขภาพพิเศษ";

        return String.format(
                "คุณคือผู้ช่วยแจ้งคุณภาพอากาศ ส่งข้อความแจ้งเตือนสั้นกระชับผ่าน LINE\n\n" +
                "ข้อมูล:\n- AQI: %d (%s)\n- %s\n\n" +
                "สร้างข้อความแจ้งเตือนเป็นภาษาไทย ความยาวไม่เกิน 5 บรรทัด:\n" +
                "1. บอกระดับ AQI และความหมาย\n" +
                "2. คำแนะนำตามสุขภาพผู้ใช้ (ถ้ามี)\n" +
                "3. สิ่งที่ควรทำหรือหลีกเลี่ยง",
                aqi, aqiLevel, profileContext);
    }

    private String toJsonString(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String buildFallbackMessage(int aqi) {
        if (aqi < 0) return "ไม่สามารถดึงข้อมูลคุณภาพอากาศได้ในขณะนี้";
        return String.format("คุณภาพอากาศ AQI: %d (%s)\nกรุณาระวังสุขภาพด้วยนะครับ", aqi, getAqiLevel(aqi));
    }

    private String getAqiLevel(int aqi) {
        if (aqi <= 50) return "ดี";
        else if (aqi <= 100) return "ปานกลาง";
        else if (aqi <= 150) return "ไม่ดีสำหรับกลุ่มเสี่ยง";
        else if (aqi <= 200) return "ไม่ดีต่อสุขภาพ";
        else if (aqi <= 300) return "อันตราย";
        else return "อันตรายมาก";
    }
}
