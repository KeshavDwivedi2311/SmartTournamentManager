package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.DTO.TeamDTO;
import com.sports.SmartSport.tournament.DTO.TeamStandingDTO;
import com.sports.SmartSport.tournament.entity.Pool;
import com.sports.SmartSport.tournament.service.KnockoutService;
import com.sports.SmartSport.tournament.service.PoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pools")
@CrossOrigin(origins = "*")
public class PoolController {

    @Autowired
    private PoolService poolService;

    @Autowired
    private KnockoutService knockoutService;

    /**
     * Get basic team rankings for a pool
     * @param poolId The pool ID
     * @param count Optional parameter to limit number of teams returned
     * @return List of teams ordered by ranking
     */
    @GetMapping("/{poolId}/rankings")
    public ResponseEntity<List<TeamDTO>> getPoolRankings(
            @PathVariable Long poolId,
            @RequestParam(required = false) Integer count) {
        try {
            List<TeamDTO> rankings = knockoutService.getPoolRankings(poolId, count);
            return ResponseEntity.ok(rankings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get detailed standings with statistics for a pool
     * @param poolId The pool ID
     * @return List of team standings with detailed statistics
     */
    @GetMapping("/{poolId}/standings")
    public ResponseEntity<List<TeamStandingDTO>> getDetailedPoolStandings(
            @PathVariable Long poolId) {
        try {
            List<TeamStandingDTO> standings = knockoutService.getDetailedPoolRankings(poolId);
            return ResponseEntity.ok(standings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get top N teams from a pool (commonly used for qualification)
     * @param poolId The pool ID
     * @param count Number of top teams to return
     * @return List of top teams
     */
    @GetMapping("/{poolId}/top/{count}")
    public ResponseEntity<List<TeamDTO>> getTopTeams(
            @PathVariable Long poolId,
            @PathVariable Integer count) {
        try {
            if (count <= 0) {
                return ResponseEntity.badRequest().build();
            }

            List<TeamDTO> topTeams = knockoutService.getPoolRankings(poolId, count);
            return ResponseEntity.ok(topTeams);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/tournament/{tournamentId}")
    public ResponseEntity<Pool> createPool(@RequestBody Pool pool, @PathVariable Long tournamentId) {
        try {
            Pool createdPool = poolService.createPool(pool, tournamentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPool);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<Pool>> getPoolsByTournament(@PathVariable Long tournamentId) {
        try {
            List<Pool> pools = poolService.getPoolsByTournament(tournamentId);
            return ResponseEntity.ok(pools);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{poolId}")
    public ResponseEntity<Pool> getPoolById(@PathVariable Long poolId) {
        try {
            Optional<Pool> pool = poolService.getPoolById(poolId);
            if (pool.isPresent()) {
                return ResponseEntity.ok(pool.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{poolId}")
    public ResponseEntity<Pool> updatePool(@PathVariable Long poolId, @RequestBody Pool pool) {
        try {
            Pool updatedPool = poolService.updatePool(poolId, pool);
            return ResponseEntity.ok(updatedPool);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{poolId}")
    public ResponseEntity<Void> deletePool(@PathVariable Long poolId) {
        try {
            boolean deleted = poolService.deletePool(poolId);
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

    @PutMapping("/{poolId}/teams/{teamId}")
    public ResponseEntity<Pool> assignTeamToPool(@PathVariable Long poolId, @PathVariable Long teamId) {
        try {
            Pool pool = poolService.assignTeamToPool(poolId, teamId);
            return ResponseEntity.ok(pool);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{poolId}/teams/{teamId}")
    public ResponseEntity<Pool> removeTeamFromPool(@PathVariable Long poolId, @PathVariable Long teamId) {
        try {
            Pool pool = poolService.removeTeamFromPool(poolId, teamId);
            return ResponseEntity.ok(pool);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}