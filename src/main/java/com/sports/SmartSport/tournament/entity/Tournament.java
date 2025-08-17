package com.sports.SmartSport.tournament.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "venue")
    private String venue; // auto suggestion add this feature later

    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column(name = "tournament_type")
    private String tournamentType; // "KNOCKOUT", "ROUND_ROBIN", etc.

    @Column(name = "status")
    private String status; // "CREATED", "ONGOING", "COMPLETED"

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
//        status = "CREATED";
    }

}
