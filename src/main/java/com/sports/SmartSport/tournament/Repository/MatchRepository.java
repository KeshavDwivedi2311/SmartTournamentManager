
package com.sports.SmartSport.tournament.Repository;

import com.sports.SmartSport.tournament.entity.Match;
import com.sports.SmartSport.tournament.entity.MatchStatus;
import com.sports.SmartSport.tournament.entity.MatchType; // Add this import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByPoolIdOrderByMatchOrder(Long poolId);
    List<Match> findByPoolIdAndStatusOrderByMatchOrder(Long poolId, MatchStatus status);

    List<Match> findByStatusOrderByScheduledTimeAsc(MatchStatus status);

    List<Match> findByPoolIdAndStatusInOrderByMatchOrder(Long poolId, List<MatchStatus> statuses);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Match m " +
            "WHERE m.pool.id = :poolId AND " +
            "((m.team1.id = :team1Id AND m.team2.id = :team2Id) OR " +
            "(m.team1.id = :team2Id AND m.team2.id = :team1Id))")
    boolean existsMatchBetweenTeams(@Param("poolId") Long poolId,
                                    @Param("team1Id") Long team1Id,
                                    @Param("team2Id") Long team2Id);
    
    @Query("SELECT m FROM Match m WHERE m.pool.tournament.id = :tournamentId ORDER BY m.matchOrder")
    List<Match> findByTournamentIdOrderByMatchOrder(@Param("tournamentId") Long tournamentId);

    // Get matches by tournament and status
    @Query("SELECT m FROM Match m WHERE m.pool.tournament.id = :tournamentId AND m.status = :status ORDER BY m.matchOrder")
    List<Match> findByTournamentIdAndStatusOrderByMatchOrder(@Param("tournamentId") Long tournamentId, @Param("status") MatchStatus status);

    // Count matches by status for a pool
    long countByPoolIdAndStatus(Long poolId, MatchStatus status);

    // Knockout phase queries
    List<Match> findByPoolIdInAndMatchTypeOrderByRoundNumberAscMatchOrderAsc(List<Long> poolIds, MatchType matchType);

    List<Match> findByPoolIdInAndMatchTypeAndStatusOrderByRoundNumberAscMatchOrderAsc(
        List<Long> poolIds, MatchType matchType, MatchStatus status);

    List<Match> findByPoolIdAndStatus(Long poolId, MatchStatus matchStatus);

    // Get matches by tournament and match type
    @Query("SELECT m FROM Match m WHERE m.pool.tournament.id = :tournamentId AND m.matchType = :matchType")
    List<Match> findMatchesByTournamentAndType(@Param("tournamentId") Long tournamentId, @Param("matchType") MatchType matchType);

    // Get matches by tournament, match type and status
    @Query("SELECT m FROM Match m WHERE m.pool.tournament.id = :tournamentId AND m.matchType = :matchType AND m.status = :status")
    List<Match> findMatchesByTournamentTypeAndStatus(@Param("tournamentId") Long tournamentId, @Param("matchType") MatchType matchType, @Param("status") MatchStatus status);

    // Bulk delete query
    @Modifying
    @Query("DELETE FROM Match m WHERE m.pool.tournament.id = :tournamentId AND m.matchType = :matchType")
    int deleteByTournamentIdAndMatchType(@Param("tournamentId") Long tournamentId, @Param("matchType") MatchType matchType);

    // Add these methods to your MatchRepository interface:
    boolean existsByPoolIdAndStatus(Long poolId, MatchStatus status);
    void deleteByPoolId(Long poolId);
    void deleteByTeam1IdOrTeam2Id(Long team1Id, Long team2Id);
    List<Match> findByPoolId(Long poolId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Match m WHERE (m.team1.id = :teamId OR m.team2.id = :teamId) AND m.status = :status")
    boolean existsByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") MatchStatus status);
}
