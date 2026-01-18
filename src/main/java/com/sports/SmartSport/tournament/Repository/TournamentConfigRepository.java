package com.sports.SmartSport.tournament.Repository;

import com.sports.SmartSport.tournament.entity.Tournament;
import com.sports.SmartSport.tournament.entity.TournamentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentConfigRepository extends JpaRepository<TournamentConfig, Long> {
    Optional<TournamentConfig> findByTournamentId(Long tournamentId);
    Optional<TournamentConfig> findByTournament(Tournament tournament);
}
