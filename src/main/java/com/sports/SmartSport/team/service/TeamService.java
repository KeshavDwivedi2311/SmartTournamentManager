package com.sports.SmartSport.team.service;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.tournament.Repository.TournamentRepository;
import com.sports.SmartSport.tournament.entity.Tournament;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {
    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    public Team createTeam(Team team, Long tournamentId){
        Optional<Tournament> tournament = tournamentRepository.findById(tournamentId);
        if (tournament.isPresent()) {
            // Check if team name already exists in this tournament
            if (teamRepository.existsByNameAndTournamentId(team.getName(), tournamentId)) {
                throw new RuntimeException("Team name already exists in this tournament");
            }

            team.setTournament(tournament.get());
            return teamRepository.save(team);
        } else {
            throw new RuntimeException("Tournament not found");
        }
    }
    public List<Team> getTeamsByTournament(Long tournamentId) {
        return teamRepository.findByTournamentId(tournamentId);
    }

    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    public Team updateTeam(Long id, Team teamDetails) {
        Optional<Team> optionalTeam = teamRepository.findById(id);
        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();
            team.setName(teamDetails.getName());
            return teamRepository.save(team);
        }
        return null;
    }

    public boolean deleteTeam(Long id) {
        if (teamRepository.existsById(id)) {
            teamRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Team> searchTeamsByName(Long tournamentId, String name) {
        return teamRepository.findByTournamentIdAndNameContaining(tournamentId, name);
    }

}
