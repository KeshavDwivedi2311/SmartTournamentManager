package com.sports.SmartSport.tournament.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sports.SmartSport.team.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("tournament-pools")
    private List<Pool> pools = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        if (status == null) {
            status = "CREATED";
        }
    }
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("tournament-teams") // If you have direct tournament-team relationship
    private List<Team> teams = new ArrayList<>();

    // Helper method to check if pool name already exists
    public boolean hasPoolWithName(String poolName) {
        return pools.stream()
                .anyMatch(pool -> pool.getName().equalsIgnoreCase(poolName));
    }
}
