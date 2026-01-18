package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.DTO.TournamentScheduleStatusDTO;
import com.sports.SmartSport.tournament.service.TournamentScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tournaments/{tournamentId}/schedule")
@CrossOrigin(origins = "*")
public class TournamentScheduleController {

    @Autowired
    private TournamentScheduleService scheduleService;

    @GetMapping("/status")
    public ResponseEntity<TournamentScheduleStatusDTO> getScheduleStatus(@PathVariable Long tournamentId) {
        try {
            TournamentScheduleStatusDTO status = scheduleService.getScheduleStatus(tournamentId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
