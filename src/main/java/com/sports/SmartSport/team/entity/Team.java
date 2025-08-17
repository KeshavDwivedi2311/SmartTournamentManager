package com.sports.SmartSport.team.entity;

import com.sports.SmartSport.player.entity.Player;
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
    private Tournament tournament;

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
