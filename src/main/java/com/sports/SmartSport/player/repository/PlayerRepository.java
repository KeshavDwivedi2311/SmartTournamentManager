package com.sports.SmartSport.player.repository;

import com.sports.SmartSport.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamId(Long teamId);

//    @Query("SELECT p FROM Player p WHERE p.team.id = :teamId AND p.position = :position")
//    List<Player> findByTeamIdAndPosition(@Param("teamId") Long teamId, @Param("position") String position);
//
//    boolean existsByEmailAndTeamTournamentId(String email, Long tournamentId);
//
//    boolean existsByJerseyNumberAndTeamId(Integer jerseyNumber, Long teamId);
}