
package com.sports.SmartSport.tournament.controller;

import com.sports.SmartSport.tournament.service.KnockoutService;
import com.sports.SmartSport.tournament.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/tournaments")
@CrossOrigin(origins = "*")
public class KnockoutController {

    @Autowired
    private KnockoutService knockoutService;

    @GetMapping("/{tournamentId}/knockout")
    public ResponseEntity<?> getKnockoutMatches(@PathVariable Long tournamentId) {
        try {
            Map<String, List<MatchDTO>> knockoutData = knockoutService.getKnockoutMatches(tournamentId);
            return ResponseEntity.ok(knockoutData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tournamentId}/qualified-teams")
    public ResponseEntity<?> getQualifiedTeams(
            @PathVariable Long tournamentId,
            @RequestParam(defaultValue = "2") int teamsPerPool) {
        try {
            List<TeamDTO> teams = knockoutService.getQualifiedTeams(tournamentId, teamsPerPool);
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * NEW: Flexible qualifier generation with request body.
     * Supports per-pool qualifier counts, pool selection, and pairing strategies.
     */
    @PostMapping("/{tournamentId}/generate-qualifiers")
    public ResponseEntity<?> generateQualifiers(
            @PathVariable Long tournamentId,
            @RequestBody(required = false) QualifierGenerationRequest request,
            @RequestParam(required = false) Integer teamsPerPool) {
        try {
            // If no request body provided, build one from query params (backward compat)
            if (request == null) {
                request = new QualifierGenerationRequest();
                request.setTeamsPerPool(teamsPerPool != null ? teamsPerPool : 2);
            }
            knockoutService.generateQualifierMatches(tournamentId, request);
            return ResponseEntity.ok(Map.of("message", "Qualifier matches generated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/generate-quarterfinals")
    public ResponseEntity<?> generateQuarterfinals(@PathVariable Long tournamentId) {
        try {
            knockoutService.generateQuarterfinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of("message", "Quarterfinal matches generated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/generate-semifinals")
    public ResponseEntity<?> generateSemifinals(@PathVariable Long tournamentId) {
        try {
            knockoutService.generateSemifinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of("message", "Semifinal matches generated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/generate-finals")
    public ResponseEntity<?> generateFinals(@PathVariable Long tournamentId) {
        try {
            knockoutService.generateFinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of("message", "Final matches generated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/custom-match")
    public ResponseEntity<?> createCustomMatch(
            @PathVariable Long tournamentId,
            @RequestBody CustomMatchRequest request) {
        try {
            MatchDTO match = knockoutService.createCustomMatch(tournamentId, request);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/custom-qualifier-pairings")
    public ResponseEntity<?> createCustomQualifierPairings(
            @PathVariable Long tournamentId,
            @RequestBody CustomPairingRequest request) {
        try {
            List<MatchDTO> matches = knockoutService.createCustomQualifierPairings(tournamentId, request);
            return ResponseEntity.ok(Map.of(
                "message", "Custom qualifier pairings created successfully",
                "matches", matches
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tournamentId}/custom-semifinal-pairings")
    public ResponseEntity<?> createCustomSemifinalPairings(
            @PathVariable Long tournamentId,
            @RequestBody CustomPairingRequest request) {
        try {
            List<MatchDTO> matches = knockoutService.createCustomSemifinalPairings(tournamentId, request);
            return ResponseEntity.ok(Map.of(
                "message", "Custom semifinal pairings created successfully",
                "matches", matches
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/knockout/qualifiers")
    public ResponseEntity<?> deleteQualifierMatches(@PathVariable Long tournamentId) {
        try {
            int deletedCount = knockoutService.deleteQualifierMatches(tournamentId);
            return ResponseEntity.ok(Map.of(
                "message", "Qualifier matches deleted successfully",
                "deletedMatches", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/knockout/quarterfinals")
    public ResponseEntity<?> deleteQuarterfinalMatches(@PathVariable Long tournamentId) {
        try {
            int deletedCount = knockoutService.deleteQuarterfinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of(
                "message", "Quarterfinal matches deleted successfully",
                "deletedMatches", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/knockout/semifinals")
    public ResponseEntity<?> deleteSemifinalMatches(@PathVariable Long tournamentId) {
        try {
            int deletedCount = knockoutService.deleteSemifinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of(
                "message", "Semifinal matches deleted successfully",
                "deletedMatches", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/knockout/finals")
    public ResponseEntity<?> deleteFinalMatches(@PathVariable Long tournamentId) {
        try {
            int deletedCount = knockoutService.deleteFinalMatches(tournamentId);
            return ResponseEntity.ok(Map.of(
                "message", "Final matches deleted successfully",
                "deletedMatches", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/knockout/all")
    public ResponseEntity<?> deleteAllKnockoutMatches(@PathVariable Long tournamentId) {
        try {
            Map<String, Integer> deletedCounts = knockoutService.deleteAllKnockoutMatches(tournamentId);
            return ResponseEntity.ok(Map.of(
                "message", "All knockout matches deleted successfully",
                "deletedCounts", deletedCounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
