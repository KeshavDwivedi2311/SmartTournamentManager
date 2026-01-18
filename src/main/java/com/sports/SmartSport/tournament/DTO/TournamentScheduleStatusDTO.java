package com.sports.SmartSport.tournament.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentScheduleStatusDTO {
    private int totalMatches;
    private int completedMatches;
    private int ongoingMatches;
    private int remainingMatches;
    private boolean ahead; // true if ahead of schedule, false if behind
    private int timeDifferenceMinutes; // Matches difference (positive = behind, negative = ahead)
    private int estimatedRemainingMinutes;
    private int completionPercentage;
    private boolean tournamentStarted; // Whether tournament has started
    private LocalDateTime tournamentStartTime; // Tournament start time
    private LocalDateTime courtBookingEndTime; // When court booking ends
    private int expectedCompletedMatches; // Expected number of matches completed by now
    private long courtTimeRemainingMinutes; // Minutes remaining in court booking
}
