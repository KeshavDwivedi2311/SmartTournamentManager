package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.DTO.BulkMatchUpdateRequest;
import com.sports.SmartSport.tournament.DTO.MatchDTO;
import com.sports.SmartSport.tournament.DTO.MatchUpdateRequest;
import com.sports.SmartSport.tournament.entity.MatchStatus;
import com.sports.SmartSport.tournament.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tournaments/matches")
@CrossOrigin(origins = "*")
@Tag(name = "Match Management", description = "APIs for managing match status, scoring, and real-time updates")
public class MatchController {

    @Autowired
    private MatchService matchManagementService;

    @Operation(summary = "Get upcoming matches", description = "Get all scheduled and ready matches for a pool")
    @GetMapping("/pools/{poolId}/upcoming")
    public ResponseEntity<?> getUpcomingMatches(@PathVariable Long poolId) {
        try {
            List<MatchDTO> matches = matchManagementService.getUpcomingMatches(poolId);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get ongoing matches", description = "Get all currently ongoing matches for a pool")
    @GetMapping("/pools/{poolId}/ongoing")
    public ResponseEntity<?> getOngoingMatches(@PathVariable Long poolId) {
        try {
            List<MatchDTO> matches = matchManagementService.getOngoingMatches(poolId);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get completed matches", description = "Get all completed matches with scores for a pool")
    @GetMapping("/pools/{poolId}/completed")
    public ResponseEntity<?> getCompletedMatches(@PathVariable Long poolId) {
        try {
            List<MatchDTO> matches = matchManagementService.getCompletedMatches(poolId);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get next matches to play", description = "Get the next N matches that are ready to be played")
    @GetMapping("/pools/{poolId}/next")
    public ResponseEntity<?> getNextMatches(
            @PathVariable Long poolId,
            @RequestParam(defaultValue = "2") int limit) {
        try {
            List<MatchDTO> matches = matchManagementService.getNextMatchesToPlay(poolId, limit);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Mark match as ready", description = "Mark a scheduled match as ready to start")
    @PostMapping("/{matchId}/ready")
    public ResponseEntity<?> markMatchAsReady(@PathVariable Long matchId) {
        try {
            MatchDTO match = matchManagementService.markMatchAsReady(matchId);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Start match", description = "Start a ready match and assign court")
    @PostMapping("/{matchId}/start")
    public ResponseEntity<?> startMatch(
            @PathVariable Long matchId,
            @RequestParam(required = false) String courtNumber) {
        try {
            MatchDTO match = matchManagementService.startMatch(matchId, courtNumber);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Complete match", description = "Complete an ongoing match with final scores")
    @PostMapping("/{matchId}/complete")
    public ResponseEntity<?> completeMatch(
            @PathVariable Long matchId,
            @RequestBody MatchUpdateRequest request) {
        try {
            MatchDTO match = matchManagementService.completeMatch(matchId, request);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update match", description = "Update match details, status, or scores")
    @PutMapping("/{matchId}")
    public ResponseEntity<?> updateMatch(
            @PathVariable Long matchId,
            @RequestBody MatchUpdateRequest request) {
        try {
            MatchDTO match = matchManagementService.updateMatch(matchId, request);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update match status", description = "Update only the status of a match")
    @PatchMapping("/{matchId}/status")
    public ResponseEntity<?> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestParam MatchStatus status) {
        try {
            MatchDTO match = matchManagementService.updateMatchStatus(matchId, status);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Mark match as next", description = "Mark a scheduled match as next to play")
    @PutMapping("/{matchId}/mark-next")
    public ResponseEntity<?> markAsNext(@PathVariable Long matchId) {
        try {
            MatchDTO updatedMatch = matchManagementService.markAsNext(matchId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Match marked as next successfully",
                "data", updatedMatch
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", e.getMessage()
                    ));
        }
    }

    @Operation(summary = "Unmark match as next", description = "Move a next match back to scheduled")
    @PutMapping("/{matchId}/unmark-next")
    public ResponseEntity<?> unmarkAsNext(@PathVariable Long matchId) {
        try {
            MatchDTO updatedMatch = matchManagementService.updateMatchStatus(matchId, MatchStatus.SCHEDULED);
            return ResponseEntity.ok(updatedMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error unmarking match as next: " + e.getMessage()));
        }
    }

    @Operation(summary = "Move match to scheduled", description = "Move any match back to scheduled status")
    @PutMapping("/{matchId}/move-to-scheduled")
    public ResponseEntity<?> moveToScheduled(@PathVariable Long matchId) {
        try {
            // Use updateMatch to clear court number and set status
            MatchUpdateRequest request = new MatchUpdateRequest();
            request.setStatus(MatchStatus.SCHEDULED);
            request.setCourtNumber(null); // Clear court assignment
            
            MatchDTO updatedMatch = matchManagementService.updateMatch(matchId, request);
            return ResponseEntity.ok(updatedMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error moving match to scheduled: " + e.getMessage()));
        }
    }

    @Operation(summary = "Move ongoing match to next", description = "Move an ongoing match to next status")
    @PutMapping("/{matchId}/move-to-next")
    public ResponseEntity<?> moveToNext(@PathVariable Long matchId) {
        try {
            MatchDTO updatedMatch = matchManagementService.moveToNext(matchId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Match moved to next successfully",
                "data", updatedMatch
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", e.getMessage()
                    ));
        }
    }

    @Operation(summary = "Revert completed match to ongoing", description = "Revert a completed match back to ongoing status")
    @PutMapping("/{matchId}/revert-to-ongoing")
    public ResponseEntity<?> revertToOngoing(@PathVariable Long matchId) {
        try {
            MatchDTO updatedMatch = matchManagementService.updateMatchStatus(matchId, MatchStatus.ONGOING);
            return ResponseEntity.ok(updatedMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error reverting match to ongoing: " + e.getMessage()));
        }
    }

    @Operation(summary = "Revert completed match to scheduled", description = "Revert a completed match back to scheduled status and clear scores")
    @PutMapping("/{matchId}/revert-to-scheduled")
    public ResponseEntity<?> revertToScheduled(@PathVariable Long matchId) {
        try {
            // Use updateMatch to clear scores, court, and set status
            MatchUpdateRequest request = new MatchUpdateRequest();
            request.setStatus(MatchStatus.SCHEDULED);
            request.setCourtNumber(null);
            request.setTeam1Score(0);
            request.setTeam2Score(0);
            
            MatchDTO updatedMatch = matchManagementService.updateMatch(matchId, request);
            return ResponseEntity.ok(updatedMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error reverting match to scheduled: " + e.getMessage()));
        }
    }

    @Operation(summary = "Revert completed match", description = "Revert a completed match back to ongoing status")
    @PutMapping("/{matchId}/revert-completion")
    public ResponseEntity<?> revertFromCompleted(@PathVariable Long matchId) {
        try {
            MatchDTO updatedMatch = matchManagementService.updateMatchStatus(matchId, MatchStatus.ONGOING);
            return ResponseEntity.ok(updatedMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error reverting completed match: " + e.getMessage()));
        }
    }

    @Operation(summary = "Complete multiple matches", description = "Complete multiple ongoing matches with final scores")
    @PostMapping("/complete-bulk")
    public ResponseEntity<?> completeMatchesBulk(@RequestBody List<BulkMatchUpdateRequest> requests) {
        try {
            List<MatchDTO> completedMatches = matchManagementService.completeMatchesBulk(requests);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Matches completed successfully",
                "completedMatches", completedMatches.size(),
                "data", completedMatches
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", e.getMessage()
                    ));
        }
    }
}
