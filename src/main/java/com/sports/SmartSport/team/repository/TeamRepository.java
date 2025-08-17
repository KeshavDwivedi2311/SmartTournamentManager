package com.sports.SmartSport.team.repository;

import com.sports.SmartSport.team.entity.Team;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team>findByName(String name);
    List<Team> findByTournamentId(Long tournamentId);
    void deleteByName(String name);
    @Query("SELECT t FROM Team t WHERE t.tournament.id = :tournamentId AND t.name LIKE %:name%")
    List<Team> findByTournamentIdAndNameContaining(@Param("tournamentId") Long tournamentId, @Param("name") String name);
    boolean existsByNameAndTournamentId(String name, Long tournamentId);

}
