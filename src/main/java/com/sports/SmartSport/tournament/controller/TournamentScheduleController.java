package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.DTO.TournamentScheduleStatusDTO;
import com.sports.SmartSport.tournament.service.TournamentScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tournaments/{tournamentId}/schedule")
@CrossOrigin(origins = "*")
public class TournamentScheduleController {

    private static final Logger log = LoggerFactory.getLogger(TournamentScheduleController.class);

    @Autowired
    private TournamentScheduleService scheduleService;

    @GetMapping("/status")
    public ResponseEntity<?> getScheduleStatus(@PathVariable Long tournamentId) {
        try {
            TournamentScheduleStatusDTO status = scheduleService.getScheduleStatus(tournamentId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting schedule status for tournament {}: {}", tournamentId, e.getMessage(), e);
            // Return a minimal valid DTO so the frontend doesn't break
            TournamentScheduleStatusDTO fallback = new TournamentScheduleStatusDTO();
            fallback.setTotalMatches(0);
            fallback.setCompletedMatches(0);
            fallback.setOngoingMatches(0);
            fallback.setRemainingMatches(0);
            fallback.setCompletionPercentage(0);
            fallback.setTournamentStarted(false);
            fallback.setExpectedCompletedMatches(0);
            fallback.setCourtTimeRemainingMinutes(0);
            return ResponseEntity.ok(fallback);
        }
    }
}
