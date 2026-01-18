package com.sports.SmartSport.tournament.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Tournament configuration for match settings and court schedule
 */
@Entity
@Table(name = "tournament_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "tournament_id", unique = true)
    private Tournament tournament;

    // Match points configuration
    @Column(name = "pool_match_points", nullable = false)
    private Integer poolMatchPoints = 15; // Default: 15 points for pool matches

    @Column(name = "knockout_match_points", nullable = false)
    private Integer knockoutMatchPoints = 21; // Default: 21 points for knockout matches

    // Court schedule configuration (JSON stored as string, or use separate entity)
    // Format: List of time slots with number of courts
    // Example: [{"startTime": "09:00", "courts": 4}, {"startTime": "10:00", "courts": 4}, ...]
    @Column(name = "court_schedule", columnDefinition = "TEXT")
    private String courtSchedule; // JSON string: [{"startTime": "09:00", "courts": 4}, ...]

    // Break time between matches (in minutes)
    @Column(name = "break_time_minutes", nullable = false)
    private Integer breakTimeMinutes = 5; // Default: 5 minutes between matches

    // Estimated match duration (in minutes) - can be calculated or set manually
    @Column(name = "estimated_match_duration_minutes")
    private Integer estimatedMatchDurationMinutes = 30; // Default: 30 minutes per match

    // Tournament start time
    @Column(name = "tournament_start_time")
    private java.time.LocalDateTime tournamentStartTime;

    // Court booking end time (when courts are no longer available)
    @Column(name = "court_booking_end_time")
    private java.time.LocalDateTime courtBookingEndTime;

    // Number of teams that qualify from each pool (configurable)
    @Column(name = "qualifiers_per_pool", nullable = false)
    private Integer qualifiersPerPool = 4; // Default: 4 teams per pool

    // Helper method to get points for a match type
    public Integer getPointsForMatchType(MatchType matchType) {
        if (matchType == MatchType.LEAGUE) {
            return poolMatchPoints;
        } else {
            return knockoutMatchPoints; // QUALIFIER, SEMIFINAL, FINAL, etc.
        }
    }
}
