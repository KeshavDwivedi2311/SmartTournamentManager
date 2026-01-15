package com.sports.SmartSport.tournament.DTO;

import com.sports.SmartSport.tournament.entity.MatchStatus;
import com.sports.SmartSport.tournament.entity.MatchType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private Long team1Id;
    private String team1Name;
    private Long team2Id;
    private String team2Name;
    private Long poolId;
    private String poolName;
    private LocalDateTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MatchStatus status;
    private int matchOrder;
    private Integer team1Score;
    private Integer team2Score;
    private Long winnerId;
    private String winnerName;
    private String notes;
    private String courtNumber;
    private MatchType matchType;
    private String matchName;
    private Integer roundNumber;

}
