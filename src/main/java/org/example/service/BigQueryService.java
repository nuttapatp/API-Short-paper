package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BigQueryService {

    private static final Logger log = LoggerFactory.getLogger(BigQueryService.class);
    private static final String DATASET = "currentapi";
    private static final String PROJECT = "air-quality-api-491405";

    private final BigQuery bigQuery;

    public BigQueryService(@Value("${BIGQUERY_CREDENTIALS:}") String credentialsJson) throws IOException {
        InputStream is;
        if (credentialsJson != null && credentialsJson.startsWith("/")) {
            // Render Secret File: value is a file path e.g. /etc/secrets/bigquery.json
            is = new java.io.FileInputStream(credentialsJson);
        } else if (credentialsJson != null && !credentialsJson.isBlank()) {
            // Render env var: value is JSON string content
            is = new ByteArrayInputStream(credentialsJson.getBytes());
        } else {
            // Local: use file from classpath
            is = getClass().getClassLoader()
                    .getResourceAsStream("air-quality-api-491405-7c36b2e10284.json");
        }
        if (is == null) throw new IOException("BigQuery credentials not found");
        GoogleCredentials credentials = GoogleCredentials.fromStream(is);
        this.bigQuery = BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(PROJECT)
                .build()
                .getService();
    }

    // Returns last 24 hours of AQI records for a city
    public List<AqiRecord> getRecentAqi(String city, int hours) {
        String query = String.format(
                "SELECT city, aqi, timestamp FROM `%s.%s.currentaqi` " +
                "WHERE city = '%s' AND timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL %d HOUR) " +
                "ORDER BY timestamp DESC LIMIT 50",
                PROJECT, DATASET, city, hours);

        return runQuery(query);
    }

    // Returns forecast records for a city
    public List<AqiRecord> getForecastAqi(String city) {
        String query = String.format(
                "SELECT city, aqi, timestamp FROM `%s.%s.forecastaqi` " +
                "WHERE city = '%s' AND timestamp >= CURRENT_TIMESTAMP() " +
                "ORDER BY timestamp ASC LIMIT 24",
                PROJECT, DATASET, city);

        return runQuery(query);
    }

    private List<AqiRecord> runQuery(String query) {
        List<AqiRecord> results = new ArrayList<>();
        try {
            QueryJobConfiguration config = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(config);
            for (FieldValueList row : result.iterateAll()) {
                String city = row.get("city").getStringValue();
                int aqi = (int) row.get("aqi").getLongValue();
                String timestamp = row.get("timestamp").getStringValue();
                results.add(new AqiRecord(city, aqi, timestamp));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("BigQuery query interrupted", e);
        } catch (Exception e) {
            log.error("BigQuery query failed", e);
        }
        return results;
    }

    public record AqiRecord(String city, int aqi, String timestamp) {}
}
