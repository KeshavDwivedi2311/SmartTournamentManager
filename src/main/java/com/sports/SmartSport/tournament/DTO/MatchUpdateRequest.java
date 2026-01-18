package com.sports.SmartSport.tournament.DTO;

import com.sports.SmartSport.tournament.entity.MatchStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchUpdateRequest {
    private MatchStatus status;
    private Integer team1Score;
    private Integer team2Score;
    private Long winnerId;
    private String notes;
    private String courtNumber;
    private Long version; // Optimistic locking version - must match current version
}