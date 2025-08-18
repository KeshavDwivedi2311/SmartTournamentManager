package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.tournament.Repository.TournamentRepository;
import com.sports.SmartSport.tournament.entity.Tournament;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {
    @Autowired
    private TournamentRepository tournamentRepository;

    public Tournament createTournament(Tournament tournament){
        return tournamentRepository.save(tournament);
    }


    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return Optional.ofNullable(tournamentRepository.findById(id).orElse(null));
    }
}
