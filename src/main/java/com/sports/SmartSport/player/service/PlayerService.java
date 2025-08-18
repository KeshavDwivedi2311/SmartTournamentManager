package com.sports.SmartSport.player.service;

import com.sports.SmartSport.player.entity.Player;
import com.sports.SmartSport.player.repository.PlayerRepository;
import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    public Player createPlayer(Player player, Long teamId) {
        Optional<Team> team = teamRepository.findById(teamId);
        if (team.isPresent()) {
            player.setTeam(team.get());
            return playerRepository.save(player);
        } else {
            throw new RuntimeException("Team not found");
        }
    }

    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    public Player updatePlayer(Long id, Player playerDetails) {
        Optional<Player> optionalPlayer = playerRepository.findById(id);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            player.setName(playerDetails.getName());
            return playerRepository.save(player);
        }
        return null;
    }

    public boolean deletePlayer(Long id) {
        if (playerRepository.existsById(id)) {
            playerRepository.deleteById(id);
            return true;
        }
        return false;
    }

//    public List<Player> getPlayersByPosition(Long teamId, String position) {
//        return playerRepository.findByTeamIdAndPosition(teamId, position);
//    }
}