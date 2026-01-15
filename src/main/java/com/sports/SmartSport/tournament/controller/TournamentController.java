package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.tournament.DTO.CreateTournamentRequest;
import com.sports.SmartSport.tournament.DTO.PoolDTO;
import com.sports.SmartSport.tournament.DTO.PoolStandingsDTO;
import com.sports.SmartSport.tournament.entity.Match;
import com.sports.SmartSport.tournament.entity.Pool;
import com.sports.SmartSport.tournament.entity.Tournament;
import com.sports.SmartSport.tournament.service.PoolService;
import com.sports.SmartSport.tournament.service.TournamentService;
import org.hibernate.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/tournaments")
@CrossOrigin(origins = "*")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;
//    @Operation(summary = "Create a new pool", description = "Creates a new pool with the specified name for a tournament")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Pool created successfully"),
//            @ApiResponse(responseCode = "400", description = "Invalid pool name or duplicate pool"),
//            @ApiResponse(responseCode = "404", description = "Tournament not found")
//    })

    @PostMapping
    public ResponseEntity<Tournament> createTournament(@RequestBody Tournament tournament) {
        try{
            Tournament createdTournament = tournamentService.createTournament(tournament);
            return ResponseEntity.ok(createdTournament);
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PostMapping("/{tournamentId}/pools")
    public ResponseEntity<?> createPool(
//            @Parameter(description = "ID of the tournament", required = true)
            @PathVariable Long tournamentId,
//            @Parameter(description = "Name of the pool to create", required = true)
            @RequestParam String poolName) {
        try {
            Pool pool = tournamentService.createPool(poolName, tournamentId);
            return ResponseEntity.ok(pool);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

//    @Operation(summary = "Get all pools for a tournament", description = "Retrieves all pools for the specified tournament")
    @GetMapping("/{tournamentId}/pools")
    public ResponseEntity<?> getPoolsByTournament(
//            @Parameter(description = "ID of the tournament", required = true)
            @PathVariable Long tournamentId) {
        try {
            List<Pool> pools = tournamentService.getPoolsByTournament(tournamentId);
            return ResponseEntity.ok(pools);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tournamentId}/pools/standings")
    public ResponseEntity<List<PoolStandingsDTO>> getPoolStandings(@PathVariable Long tournamentId) {
        try {

            List<PoolStandingsDTO> poolStandings = tournamentService.getPoolStandings(tournamentId);
            return ResponseEntity.ok(poolStandings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/pools/{poolId}/schedule")
    public ResponseEntity<List<Match>> scheduleMatches(@PathVariable Long poolId) {
        List<Match> matches = tournamentService.scheduleMatches(poolId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/pools/{poolId}/optimized-schedule")
    public ResponseEntity<List<Match>> getOptimizedSchedule(@PathVariable Long poolId) {
        List<Match> matches = tournamentService.getOptimizedSchedule(poolId);
        return ResponseEntity.ok(matches);
    }


    @PostMapping("/pools/{poolId}/teams")
    public ResponseEntity<Team> addTeam(@PathVariable Long poolId, @RequestParam String teamName) {
        Team team = tournamentService.addTeamToPool(teamName, poolId);
        return ResponseEntity.ok(team);
    }



    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        try {
            List<Tournament> tournaments = tournamentService.getAllTournaments();
            return ResponseEntity.ok(tournaments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
}

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable Long id) {
        try {
            Optional<Tournament> tournament = tournamentService.getTournamentById(id);
            if (tournament.isPresent()) {
                return ResponseEntity.ok(tournament.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error fetching tournament: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/create-complete")
    public ResponseEntity<?> createCompleteTournament(@RequestBody CreateTournamentRequest request) {
        try {
            Tournament createdTournament = tournamentService.createCompleteTournament(request);
            return ResponseEntity.ok(createdTournament);
        } catch (Exception e) {
            e.printStackTrace(); // Add this to see the full stack trace
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}")
    public ResponseEntity<?> deleteTournament(@PathVariable Long tournamentId) {
        try {
            boolean deleted = tournamentService.deleteTournament(tournamentId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Tournament deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete tournament"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/{tournamentId}/pools/{poolId}")
    public ResponseEntity<?> deletePool(
            @PathVariable Long tournamentId,
            @PathVariable Long poolId) {
        try {
            boolean deleted = tournamentService.deletePool(poolId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Pool deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete pool"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/pools/{poolId}/teams/{teamId}")
    public ResponseEntity<?> deleteTeamFromPool(
            @PathVariable Long poolId,
            @PathVariable Long teamId) {
        try {
            boolean deleted = tournamentService.deleteTeamFromPool(teamId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Team deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete team"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<?> deleteMatch(@PathVariable Long matchId) {
        try {
            boolean deleted = tournamentService.deleteMatch(matchId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Match deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to delete match"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/pools/{poolId}/matches")
    public ResponseEntity<?> deleteAllPoolMatches(@PathVariable Long poolId) {
        try {
            int deletedCount = tournamentService.deleteAllPoolMatches(poolId);
            return ResponseEntity.ok(Map.of(
                    "message", "Pool matches deleted successfully",
                    "deletedCount", deletedCount
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }
}
