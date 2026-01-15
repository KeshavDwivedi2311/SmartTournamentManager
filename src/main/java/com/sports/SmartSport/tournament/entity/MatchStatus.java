package com.sports.SmartSport.tournament.entity;

public enum MatchStatus {
    SCHEDULED,  // Match is scheduled but not ready yet
    READY,      // Match is ready to start (teams are prepared)
    ONGOING,    // Match is currently being played
    COMPLETED,  // Match is finished with results
    CANCELLED,  // Match was cancelled
    POSTPONED,   // Match was postponed
    NEXT
}
