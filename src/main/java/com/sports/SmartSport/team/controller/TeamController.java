package com.sports.SmartSport.team.controller;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.team.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/teams")
@CrossOrigin(origins = "*")
public class TeamController {
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    TeamService teamService;
    @PostMapping("/tournament/{tournamentId}")
    public ResponseEntity<Team> createTeam(@RequestBody Team team, @PathVariable Long tournamentId) {
        try {
            Team createdTeam = teamService.createTeam(team, tournamentId);
            return ResponseEntity.ok(createdTeam);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<Team>> getTeamsByTournament(@PathVariable Long tournamentId) {
        try {
            List<Team> teams = teamService.getTeamsByTournament(tournamentId);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Add endpoint for getting teams by pool
    @GetMapping("/pool/{poolId}")
    public ResponseEntity<List<Team>> getTeamsByPool(@PathVariable Long poolId) {
        try {
            List<Team> teams = teamService.getTeamsByPool(poolId);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        try {
            Optional<Team> team = teamService.getTeamById(id);
            if (team.isPresent()) {
                return ResponseEntity.ok(team.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable Long id, @RequestBody Team team) {
        try {
            Team updatedTeam = teamService.updateTeam(id, team);
            if (updatedTeam != null) {
                return ResponseEntity.ok(updatedTeam);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        try {
            boolean deleted = teamService.deleteTeam(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
