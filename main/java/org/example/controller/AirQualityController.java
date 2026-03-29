package org.example.controller;
import org.example.Main;


//import org.example.model.Location;
import org.example.model.Location;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AirQualityController {

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchDataAndNotify(@RequestBody Location location) {
        Main.fetchAndNotifyUsers(location);
        return ResponseEntity.ok("Data fetched and users notified successfully.");
    }



}
