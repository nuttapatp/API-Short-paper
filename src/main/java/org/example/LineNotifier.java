package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LineNotifier {

    private static final Logger log = LoggerFactory.getLogger(LineNotifier.class);
    private final String accessToken;

    public LineNotifier(String accessToken) {
        this.accessToken = accessToken;
    }

    public void sendLineMessageToUser(String message, String userId) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String jsonPayload = createJsonPayload(message, userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.line.me/v2/bot/message/push"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("LINE response for user {}: {}", userId, response.body());
    }

    private String createJsonPayload(String message, String userId) {
        String escaped = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"to\": \"" + userId + "\", \"messages\": [{\"type\": \"text\", \"text\": \"" + escaped + "\"}]}";
    }


}
