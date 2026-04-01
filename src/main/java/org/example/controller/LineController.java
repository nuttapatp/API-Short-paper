package org.example.controller;

import org.example.AirQualityApi;
import org.example.LineNotifier;
import org.example.Main;
import org.example.model.Location;
import org.example.service.ClaudeService;
import org.example.service.FirestoreService;
import org.example.service.BigQueryService;
import org.example.service.IntentService;
import org.example.service.RateLimitService;
import org.example.utils.UtilityMethods;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LineController {

    private static final Logger log = LoggerFactory.getLogger(LineController.class);

    private final ClaudeService claudeService;
    private final IntentService intentService;
    private final RateLimitService rateLimitService;
    private final BigQueryService bigQueryService;

    public LineController(ClaudeService claudeService, IntentService intentService,
                          RateLimitService rateLimitService, BigQueryService bigQueryService) {
        this.claudeService = claudeService;
        this.intentService = intentService;
        this.rateLimitService = rateLimitService;
        this.bigQueryService = bigQueryService;
    }

    @Value("${LINE_ACCESS_TOKEN}")
    private String lineToken;

    @Value("${GOOGLE_AIR_QUALITY_API_KEY}")
    private String airQualityApiKey;

    @GetMapping("/")
    public String healthCheck() {
        return "The service is up and running!";
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String body) {
        try {
            JSONObject payload = new JSONObject(body);
            JSONArray events = payload.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String eventType = event.getString("type");
                String userId = event.getJSONObject("source").getString("userId");

                if (!rateLimitService.isAllowed(userId)) {
                    log.warn("Rate limit hit for user {}, skipping event", userId);
                    continue;
                }

                if ("follow".equals(eventType)) {
                    FirestoreService.saveUserId(userId);
                    log.info("New user followed: {}", userId);
                    LineNotifier notifier = new LineNotifier(lineToken);
                    notifier.sendLineMessageToUser(getWelcomeMessage(), userId);

                } else if ("message".equals(eventType)) {
                    JSONObject message = event.getJSONObject("message");

                    if ("location".equals(message.getString("type"))) {
                        double latitude = message.getDouble("latitude");
                        double longitude = message.getDouble("longitude");
                        FirestoreService.saveUserLocation(userId, latitude, longitude);
                        log.info("Location saved for user {}: {}, {}", userId, latitude, longitude);
                        Main.sendNotificationToUser(userId, "", lineToken, airQualityApiKey, claudeService);

                    } else if ("text".equals(message.getString("type"))) {
                        String text = message.getString("text").trim();
                        handleTextMessage(userId, text);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling LINE webhook", e);
        }
        return ResponseEntity.ok("OK");
    }

    private void handleTextMessage(String userId, String text) throws Exception {
        LineNotifier notifier = new LineNotifier(lineToken);
        IntentService.Intent intent = intentService.classify(text);
        log.info("User {} sent: '{}' → intent: {}", userId, text, intent);

        switch (intent) {
            case AQI_NOW -> {
                Location location = FirestoreService.getUserLatestLocation(userId);
                if (location == null) {
                    notifier.sendLineMessageToUser("ยังไม่มีข้อมูล location ของคุณ\nกรุณาส่ง Location มาก่อนนะครับ 📍", userId);
                } else {
                    AirQualityApi api = new AirQualityApi(airQualityApiKey);
                    String response = api.fetchData(location.getLatitude(), location.getLongitude());
                    double pm25 = api.extractPM25Value(response);
                    int aqi = UtilityMethods.convertPM25ToAQI(pm25);
                    String healthProfile = FirestoreService.getUserHealthProfile(userId);
                    String reply = claudeService.generateAqiNotification(aqi, healthProfile);
                    notifier.sendLineMessageToUser(reply, userId);
                }
            }
            case AQI_FORECAST -> {
                Location location = FirestoreService.getUserLatestLocation(userId);
                String city = location != null
                        ? resolveCity(location.getLatitude(), location.getLongitude())
                        : "Bangkok";
                var recent = bigQueryService.getRecentAqi(city, 24);
                var forecast = bigQueryService.getForecastAqi(city);
                String analysis = claudeService.analyzeForecast(city, recent, forecast);
                notifier.sendLineMessageToUser(analysis, userId);
            }

            case BEST_DAY -> notifier.sendLineMessageToUser(
                    "ขณะนี้ยังไม่มีข้อมูลรายสัปดาห์ครับ\nสามารถส่ง Location เพื่อเช็ค AQI ตอนนี้ได้เลย 📍", userId);

            case SET_PROFILE -> {
                FirestoreService.saveUserHealthProfile(userId, text);
                log.info("Health profile saved for user {}: {}", userId, text);
                notifier.sendLineMessageToUser("บันทึกข้อมูลสุขภาพของคุณแล้ว\nจะแจ้งเตือน AQI ตามข้อมูลนี้ครับ", userId);
            }
            case DELETE_PROFILE -> {
                FirestoreService.deleteUserHealthProfile(userId);
                log.info("Health profile deleted for user {}", userId);
                notifier.sendLineMessageToUser("ลบข้อมูลสุขภาพของคุณแล้วครับ", userId);
            }
            default -> notifier.sendLineMessageToUser(getHelpMessage(), userId);
        }
    }

    private String resolveCity(double lat, double lon) {
        if (lat >= 17.0) return "Chiang Mai";
        if (lat <= 9.0) return "Phuket";
        if (lon >= 101.0) return "Chonburi";
        return "Bangkok";
    }

    private String getWelcomeMessage() {
        return "สวัสดีครับ! ยินดีต้อนรับสู่ AQI Notification\n\n"
                + "สามารถใช้งานได้ดังนี้:\n"
                + "📍 ส่ง Location เพื่อรับการแจ้งเตือน AQI\n"
                + "💬 พิมพ์ \"อากาศวันนี้เป็นยังไง\"\n"
                + "🏥 พิมพ์ข้อมูลสุขภาพ เช่น \"ฉันเป็นโรคหอบหืด\"\n"
                + "🗑️ พิมพ์ \"ลบข้อมูลฉัน\" เพื่อลบข้อมูลสุขภาพ";
    }

    private String getHelpMessage() {
        return "สามารถถามฉันได้ว่า:\n\n"
                + "📍 ส่ง Location เพื่อรับ AQI ณ ตำแหน่งนั้น\n"
                + "💬 \"อากาศวันนี้เป็นยังไง\"\n"
                + "🏥 \"ฉันเป็นโรคภูมิแพ้\" (บันทึกข้อมูลสุขภาพ)\n"
                + "🗑️ \"ลบข้อมูลฉัน\"";
    }
}
