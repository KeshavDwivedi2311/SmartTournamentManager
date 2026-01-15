package com.sports.SmartSport.tournament.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sports.SmartSport.team.entity.Team;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "pools")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "pool", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("pool-teams")
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "pool", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Match> matches = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonBackReference("tournament-pools")
    private Tournament tournament;

    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column
    private String description;

    public Pool(String name) {
        this.name = name;
    }
    public Pool(String name, Tournament tournament) {
        this.name = name;
        this.tournament = tournament;
    }
}