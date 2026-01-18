package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.entity.TournamentConfig;
import com.sports.SmartSport.tournament.service.TournamentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tournaments/{tournamentId}/config")
@CrossOrigin(origins = "*")
public class TournamentConfigController {

    @Autowired
    private TournamentConfigService configService;

    @GetMapping
    public ResponseEntity<TournamentConfig> getConfig(@PathVariable Long tournamentId) {
        try {
            TournamentConfig config = configService.getConfig(tournamentId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping
    public ResponseEntity<TournamentConfig> updateConfig(
            @PathVariable Long tournamentId,
            @RequestBody TournamentConfig updatedConfig) {
        try {
            TournamentConfig config = configService.updateConfig(tournamentId, updatedConfig);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
