package com.sports.SmartSport.team.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sports.SmartSport.player.entity.Player;
import com.sports.SmartSport.tournament.entity.Pool;
import com.sports.SmartSport.tournament.entity.Tournament;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Represents a sports team entity.
 * This class is used to store information about a team, including its ID and name.
 */
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonBackReference("tournament-teams")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id")
    @JsonBackReference("pool-teams")
    private Pool pool;

    private int gamesPlayed = 0;
    private boolean isActive = true;

    public Team(String name, Pool pool) {
        this.name = name;
        this.pool = pool;
        this.tournament = pool != null ? pool.getTournament() : null;
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public boolean hasCompletedAllGames(int poolSize) {
        return gamesPlayed >= (poolSize - 1);
    }

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Player> players = new ArrayList<>();

    public void addPlayer(Player player) {
        player.setTeam(this);
        players.add(player);
    }
    public void removePlayer(Player player) {
        players.remove(player);
        player.setTeam(null);
    }
}
