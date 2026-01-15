package com.sports.SmartSport.tournament.Repository;

import com.sports.SmartSport.tournament.entity.Pool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolRepository extends JpaRepository<Pool, Long> {
    // Check if pool with same name exists in tournament
    boolean existsByNameAndTournamentId(String name, Long tournamentId);

    // Find all pools for a tournament
    List<Pool> findByTournamentId(Long tournamentId);

    // Find pool by name and tournament
    Optional<Pool> findByNameAndTournamentId(String name, Long tournamentId);

    @Query("SELECT p FROM Pool p LEFT JOIN FETCH p.teams WHERE p.tournament.id = :tournamentId")
    List<Pool> findByTournamentIdWithTeams(@Param("tournamentId") Long tournamentId);


    @Query("SELECT COUNT(t) FROM Team t WHERE t.pool.id = :poolId")
    Long countTeamsByPoolId(@Param("poolId") Long poolId);

    // Custom query to check case-insensitive duplicate
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pool p " +
            "WHERE LOWER(p.name) = LOWER(:name) AND p.tournament.id = :tournamentId")
    boolean existsByNameIgnoreCaseAndTournamentId(@Param("name") String name, @Param("tournamentId") Long tournamentId);

    // Add this method to your PoolRepository interface:
    void deleteByTournamentId(Long tournamentId);
}
