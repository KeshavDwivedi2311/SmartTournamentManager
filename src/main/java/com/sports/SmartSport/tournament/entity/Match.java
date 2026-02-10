package com.sports.SmartSport.tournament.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sports.SmartSport.team.entity.Team;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    private Team team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    private Team team2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id")
    @JsonBackReference("pool-matches")
    private Pool pool;

    private LocalDateTime scheduledTime;
    private int matchOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MatchStatus status = MatchStatus.SCHEDULED;

    // Scoring fields
    private Integer team1Score = 0;
    private Integer team2Score = 0;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Team winner;

    @Column(name = "notes")
    private String notes;

    // Court/Field assignment
    @Column(name = "court_number")
    private String courtNumber;

    // Add this field to your Match entity:
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type")
    private MatchType matchType = MatchType.LEAGUE;

    @Column(name = "match_name")
    private String matchName; // For custom matches like "3rd Place Playoff"

    @Column(name = "round_number")
    private Integer roundNumber; // For tracking knockout rounds

    // Optimistic locking version field - prevents concurrent update conflicts
    // Note: "version" is a reserved function name in H2 2.x, so we use "match_version"
    @Version
    @Column(name = "match_version")
    private Long version = 0L;

    public Match(Team team1, Team team2, Pool pool, int matchOrder) {
        this.team1 = team1;
        this.team2 = team2;
        this.pool = pool;
        this.matchOrder = matchOrder;
        this.status = MatchStatus.SCHEDULED;
    }

    public boolean isCompleted() {
        return status == MatchStatus.COMPLETED;
    }

    public boolean isOngoing() {
        return status == MatchStatus.ONGOING;
    }

    public boolean isScheduled() {
        return status == MatchStatus.SCHEDULED;
    }

    public void startMatch() {
        this.status = MatchStatus.ONGOING;
        this.startTime = LocalDateTime.now();
    }

    public void completeMatch(Integer team1Score, Integer team2Score, Team winner) {
        this.status = MatchStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.winner = winner;
    }

    public void markAsReady() {
        this.status = MatchStatus.READY;
    }

    // Add getters and setters
    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
}
