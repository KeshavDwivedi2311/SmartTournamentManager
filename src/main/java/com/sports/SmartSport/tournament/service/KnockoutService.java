
package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.tournament.entity.*;
import com.sports.SmartSport.tournament.Repository.*;
import com.sports.SmartSport.tournament.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnockoutService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PoolRepository poolRepository;

    public Map<String, List<MatchDTO>> getKnockoutMatches(Long tournamentId) {
        Map<String, List<MatchDTO>> knockoutData = new HashMap<>();

        knockoutData.put("qualifiers", getMatchesByType(tournamentId, MatchType.QUALIFIER));
        knockoutData.put("semifinals", getMatchesByType(tournamentId, MatchType.SEMIFINAL));
        knockoutData.put("finals", getMatchesByType(tournamentId, MatchType.FINAL));
        knockoutData.put("customMatches", getMatchesByType(tournamentId, MatchType.CUSTOM));

        return knockoutData;
    }

    private List<MatchDTO> getMatchesByType(Long tournamentId, MatchType matchType) {
        // Get all pools for this tournament
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Long> poolIds = pools.stream().map(Pool::getId).collect(Collectors.toList());

        List<Match> matches = matchRepository.findByPoolIdInAndMatchTypeOrderByRoundNumberAscMatchOrderAsc(poolIds, matchType);
        return matches.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<TeamDTO> getQualifiedTeams(Long tournamentId, int teamsPerPool) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Team> qualifiedTeams = new ArrayList<>();

        for (Pool pool : pools) {
            int poolSize = pool.getTeams().size();
            int qualifiedCount = Math.min(teamsPerPool, poolSize); // Don't exceed pool size

            if (qualifiedCount > 0) {
                List<Team> poolTeams = getTopTeamsFromPool(pool.getId(), qualifiedCount);
                qualifiedTeams.addAll(poolTeams);
            }
        }

        return qualifiedTeams.stream().map(this::convertTeamToDTO).collect(Collectors.toList());
    }

    // Keep the original method as a default (uses 2 teams per pool)
    public List<TeamDTO> getQualifiedTeams(Long tournamentId) {
        return getQualifiedTeams(tournamentId, 2); // Default to 2 teams per pool
    }

    public void generateQualifierMatches(Long tournamentId, int teamsPerPool) {
        List<TeamDTO> qualifiedTeams = getQualifiedTeams(tournamentId, teamsPerPool);

        if (qualifiedTeams.size() < 2) {
            throw new RuntimeException("Need at least 2 qualified teams to generate qualifiers");
        }

        // Create qualifier matches (cross-pool matchups)
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Get the first pool to associate knockout matches with
        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        // Shuffle teams for random matchups
        Collections.shuffle(qualifiedTeams);

        int matchOrder = 1;
        for (int i = 0; i < qualifiedTeams.size(); i += 2) {
            if (i + 1 < qualifiedTeams.size()) {
                Match qualifierMatch = new Match();
                qualifierMatch.setTeam1(teamRepository.findById(qualifiedTeams.get(i).getId()).get());
                qualifierMatch.setTeam2(teamRepository.findById(qualifiedTeams.get(i + 1).getId()).get());
                qualifierMatch.setPool(firstPool);
                qualifierMatch.setMatchType(MatchType.QUALIFIER);
                qualifierMatch.setMatchName("Qualifier " + ((i / 2) + 1));
                qualifierMatch.setRoundNumber(1);
                qualifierMatch.setMatchOrder(matchOrder++);
                qualifierMatch.setStatus(MatchStatus.SCHEDULED);

                matchRepository.save(qualifierMatch);
            }
        }
    }

    // Keep the original method as a default
    public void generateQualifierMatches(Long tournamentId) {
        generateQualifierMatches(tournamentId, 2); // Default to 2 teams per pool
    }

    public void generateSemifinalMatches(Long tournamentId) {
        // Get completed qualifier matches
        List<Match> qualifierMatches = getCompletedMatchesByType(tournamentId, MatchType.QUALIFIER);

        if (qualifierMatches.size() < 2) {
            throw new RuntimeException("Need at least 2 completed qualifier matches to generate semifinals");
        }

        List<Team> winners = qualifierMatches.stream()
            .map(Match::getWinner)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (winners.size() < 4) {
            throw new RuntimeException("Need at least 4 qualifier winners to generate semifinals");
        }

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        // Create semifinal matches
        for (int i = 0; i < winners.size(); i += 2) {
            if (i + 1 < winners.size()) {
                Match semifinalMatch = new Match();
                semifinalMatch.setTeam1(winners.get(i));
                semifinalMatch.setTeam2(winners.get(i + 1));
                semifinalMatch.setPool(firstPool);
                semifinalMatch.setMatchType(MatchType.SEMIFINAL);
                semifinalMatch.setMatchName("Semifinal " + ((i / 2) + 1));
                semifinalMatch.setRoundNumber(2);
                semifinalMatch.setMatchOrder(i / 2 + 1);
                semifinalMatch.setStatus(MatchStatus.SCHEDULED);

                matchRepository.save(semifinalMatch);
            }
        }
    }

    public void generateFinalMatches(Long tournamentId) {
        List<Match> semifinalMatches = getCompletedMatchesByType(tournamentId, MatchType.SEMIFINAL);

        if (semifinalMatches.size() < 2) {
            throw new RuntimeException("Need 2 completed semifinal matches to generate finals");
        }

        List<Team> winners = semifinalMatches.stream()
            .map(Match::getWinner)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<Team> losers = semifinalMatches.stream()
            .map(match -> {
                if (match.getWinner().equals(match.getTeam1())) {
                    return match.getTeam2();
                } else {
                    return match.getTeam1();
                }
            })
            .collect(Collectors.toList());

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        // Create 3rd place playoff
        if (losers.size() >= 2) {
            Match thirdPlaceMatch = new Match();
            thirdPlaceMatch.setTeam1(losers.get(0));
            thirdPlaceMatch.setTeam2(losers.get(1));
            thirdPlaceMatch.setPool(firstPool);
            thirdPlaceMatch.setMatchType(MatchType.THIRD_PLACE);
            thirdPlaceMatch.setMatchName("3rd Place Playoff");
            thirdPlaceMatch.setRoundNumber(3);
            thirdPlaceMatch.setMatchOrder(1);
            thirdPlaceMatch.setStatus(MatchStatus.SCHEDULED);

            matchRepository.save(thirdPlaceMatch);
        }

        // Create final
        if (winners.size() >= 2) {
            Match finalMatch = new Match();
            finalMatch.setTeam1(winners.get(0));
            finalMatch.setTeam2(winners.get(1));
            finalMatch.setPool(firstPool);
            finalMatch.setMatchType(MatchType.FINAL);
            finalMatch.setMatchName("Final");
            finalMatch.setRoundNumber(3);
            finalMatch.setMatchOrder(2);
            finalMatch.setStatus(MatchStatus.SCHEDULED);

            matchRepository.save(finalMatch);
        }
    }

    public MatchDTO createCustomMatch(Long tournamentId, CustomMatchRequest request) {
        Team team1 = teamRepository.findById(request.getTeam1Id())
            .orElseThrow(() -> new RuntimeException("Team 1 not found"));
        Team team2 = teamRepository.findById(request.getTeam2Id())
            .orElseThrow(() -> new RuntimeException("Team 2 not found"));

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        Match customMatch = new Match();
        customMatch.setTeam1(team1);
        customMatch.setTeam2(team2);
        customMatch.setPool(firstPool);
        customMatch.setMatchType(MatchType.CUSTOM);
        customMatch.setMatchName(request.getMatchName());
        customMatch.setRoundNumber(99); // High number for custom matches
        customMatch.setMatchOrder(1);
        customMatch.setStatus(MatchStatus.SCHEDULED);

        Match savedMatch = matchRepository.save(customMatch);
        return convertToDTO(savedMatch);
    }

    private List<Match> getCompletedMatchesByType(Long tournamentId, MatchType matchType) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Long> poolIds = pools.stream().map(Pool::getId).collect(Collectors.toList());

        return matchRepository.findByPoolIdInAndMatchTypeAndStatusOrderByRoundNumberAscMatchOrderAsc(
            poolIds, matchType, MatchStatus.COMPLETED);
    }


    private List<Team> getTopTeamsFromPool(Long poolId, int count) {
        // Get completed matches for this pool
        List<Match> poolMatches = matchRepository.findByPoolIdAndStatus(poolId, MatchStatus.COMPLETED);

        // Get all teams in the pool
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));
        List<Team> allTeams = pool.getTeams();

        if (allTeams.isEmpty()) {
            return new ArrayList<>();
        }

        // Calculate team standings for badminton
        Map<Long, BadmintonStanding> teamStandings = new HashMap<>();

        // Initialize all teams with 0 stats
        for (Team team : allTeams) {
            teamStandings.put(team.getId(), new BadmintonStanding(team));
        }

        // Process completed matches
        for (Match match : poolMatches) {
            Team team1 = match.getTeam1();
            Team team2 = match.getTeam2();

            BadmintonStanding standing1 = teamStandings.get(team1.getId());
            BadmintonStanding standing2 = teamStandings.get(team2.getId());

            if (standing1 != null && standing2 != null) {
                // Update matches played
                standing1.matchesPlayed++;
                standing2.matchesPlayed++;

                // Get match scores (points scored in the game)
                int team1Score = match.getTeam1Score() != null ? match.getTeam1Score() : 0;
                int team2Score = match.getTeam2Score() != null ? match.getTeam2Score() : 0;

                // Update points scored/conceded
                standing1.pointsScored += team1Score;
                standing1.pointsConceded += team2Score;
                standing2.pointsScored += team2Score;
                standing2.pointsConceded += team1Score;

                // Update match wins/losses and tournament points
                if (match.getWinner() != null) {
                    if (match.getWinner().equals(team1)) {
                        standing1.matchWins++;
                        standing1.tournamentPoints += 3;
                        standing2.matchLosses++;
                    } else if (match.getWinner().equals(team2)) {
                        standing2.matchWins++;
                        standing2.tournamentPoints += 3;
                        standing1.matchLosses++;
                    }
                }
            }
        }

        // Sort teams by badminton ranking criteria and return top teams
        return teamStandings.entrySet().stream()
                .sorted((e1, e2) -> {
                    BadmintonStanding s1 = e1.getValue();
                    BadmintonStanding s2 = e2.getValue();

                    // Primary: Tournament points (match wins * 3) - descending
                    int tournamentPointsCompare = Integer.compare(s2.tournamentPoints, s1.tournamentPoints);
                    if (tournamentPointsCompare != 0) return tournamentPointsCompare;

                    // Secondary: Point difference (descending)
                    int pointDiffCompare = Integer.compare(s2.getPointDifference(), s1.getPointDifference());
                    if (pointDiffCompare != 0) return pointDiffCompare;

                    // Tertiary: Net ratio (descending)
                    int netRatioCompare = Double.compare(s2.getNetRatio(), s1.getNetRatio());
                    if (netRatioCompare != 0) return netRatioCompare;

                    // Quaternary: Points scored (descending)
                    int pointsScoredCompare = Integer.compare(s2.pointsScored, s1.pointsScored);
                    if (pointsScoredCompare != 0) return pointsScoredCompare;

                    // Final: Team name (alphabetical)
                    return s1.team.getName().compareTo(s2.team.getName());
                })
                .limit(count)
                .map(entry -> entry.getValue().team)
                .collect(Collectors.toList());
    }

    // Add a method to get full standings (for pool standings API)
    public List<TeamStandingDTO> getPoolStandingsFromKnockoutService(Long poolId) {
        // Get completed matches for this pool
        List<Match> poolMatches = matchRepository.findByPoolIdAndStatus(poolId, MatchStatus.COMPLETED);

        // Get all teams in the pool
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));
        List<Team> allTeams = pool.getTeams();

        if (allTeams.isEmpty()) {
            return new ArrayList<>();
        }

        // Calculate team standings for badminton
        Map<Long, BadmintonStanding> teamStandings = new HashMap<>();

        // Initialize all teams with 0 stats
        for (Team team : allTeams) {
            teamStandings.put(team.getId(), new BadmintonStanding(team));
        }

        // Process completed matches (same logic as getTopTeamsFromPool)
        for (Match match : poolMatches) {
            Team team1 = match.getTeam1();
            Team team2 = match.getTeam2();

            BadmintonStanding standing1 = teamStandings.get(team1.getId());
            BadmintonStanding standing2 = teamStandings.get(team2.getId());

            if (standing1 != null && standing2 != null) {
                // Update matches played
                standing1.matchesPlayed++;
                standing2.matchesPlayed++;

                // Get match scores
                int team1Score = match.getTeam1Score() != null ? match.getTeam1Score() : 0;
                int team2Score = match.getTeam2Score() != null ? match.getTeam2Score() : 0;

                // Update points scored/conceded
                standing1.pointsScored += team1Score;
                standing1.pointsConceded += team2Score;
                standing2.pointsScored += team2Score;
                standing2.pointsConceded += team1Score;

                // Update match wins/losses and tournament points
                if (match.getWinner() != null) {
                    if (match.getWinner().equals(team1)) {
                        standing1.matchWins++;
                        standing1.tournamentPoints += 2;
                        standing2.matchLosses++;
                    } else if (match.getWinner().equals(team2)) {
                        standing2.matchWins++;
                        standing2.tournamentPoints += 2;
                        standing1.matchLosses++;
                    }
                }
            }
        }

        // Convert to DTOs and sort
        List<TeamStandingDTO> standings = teamStandings.values().stream()
                .map(this::convertBadmintonStandingToDTO)
                .sorted((s1, s2) -> {
                    // Same sorting logic as above
                    int tournamentPointsCompare = Integer.compare(s2.getTournamentPoints(), s1.getTournamentPoints());
                    if (tournamentPointsCompare != 0) return tournamentPointsCompare;

                    int pointDiffCompare = Integer.compare(s2.getPointDifference(), s1.getPointDifference());
                    if (pointDiffCompare != 0) return pointDiffCompare;

                    int netRatioCompare = Double.compare(s2.getNetRatio(), s1.getNetRatio());
                    if (netRatioCompare != 0) return netRatioCompare;

                    int pointsScoredCompare = Integer.compare(s2.getPointsScored(), s1.getPointsScored());
                    if (pointsScoredCompare != 0) return pointsScoredCompare;

                    return s1.getTeamName().compareTo(s2.getTeamName());
                })
                .collect(Collectors.toList());

        // Set positions
        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
            standings.get(i).setForm(calculateTeamForm(standings.get(i).getTeamId(), poolMatches));
        }

        return standings;
    }
    private String calculateTeamForm(Long teamId, List<Match> poolMatches) {
        // Filter matches for this specific team and sort by match order/time
        List<Match> teamMatches = poolMatches.stream()
                .filter(match -> match.getTeam1().getId().equals(teamId) ||
                        match.getTeam2().getId().equals(teamId))
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .sorted((m1, m2) -> {
                    // Sort by match order first, then by end time if available
                    int orderCompare = Integer.compare(
                            m1.getMatchOrder() != 0 ? m1.getMatchOrder() : 0,
                            m2.getMatchOrder() != 0 ? m2.getMatchOrder() : 0
                    );
                    if (orderCompare != 0) return orderCompare;

                    if (m1.getEndTime() != null && m2.getEndTime() != null) {
                        return m1.getEndTime().compareTo(m2.getEndTime());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // Build form string from recent matches (last 5)
        StringBuilder form = new StringBuilder();
        int matchesToShow = Math.min(5, teamMatches.size());

        // Get the last N matches
        List<Match> recentMatches = teamMatches.subList(
                Math.max(0, teamMatches.size() - matchesToShow),
                teamMatches.size()
        );

        for (Match match : recentMatches) {
            if (match.getWinner() != null) {
                if (match.getWinner().getId().equals(teamId)) {
                    form.append("W");
                } else {
                    form.append("L");
                }
            } else {
                // In case of no winner (shouldn't happen in badminton, but just in case)
                form.append("D");
            }
        }

        return form.toString();
    }

    // Helper method to convert BadmintonStanding to TeamStandingDTO
    private TeamStandingDTO convertBadmintonStandingToDTO(BadmintonStanding standing) {
        TeamStandingDTO dto = new TeamStandingDTO(
                standing.team.getId(),
                standing.team.getName(),
                standing.team.getPool().getId(),
                standing.team.getPool().getName()
        );

        dto.setMatchesPlayed(standing.matchesPlayed);
        dto.setWins(standing.matchWins);
        dto.setLosses(standing.matchLosses);
        dto.setPointsScored(standing.pointsScored);
        dto.setPointsConceded(standing.pointsConceded);
        dto.setTournamentPoints(standing.tournamentPoints);
        dto.calculateDifferencesAndRatio();

        return dto;
    }

    // Updated BadmintonStanding helper class
    private static class BadmintonStanding {
        Team team;
        int matchesPlayed = 0;
        int matchWins = 0;
        int matchLosses = 0;
        int pointsScored = 0;
        int pointsConceded = 0;
        int tournamentPoints = 0; // 3 points per match win

        BadmintonStanding(Team team) {
            this.team = team;
        }

        int getPointDifference() {
            return pointsScored - pointsConceded;
        }

        double getNetRatio() {
            return pointsConceded > 0 ? (double) pointsScored / pointsConceded :
                    pointsScored > 0 ? Double.MAX_VALUE : 0.0;
        }

        @Override
        public String toString() {
            return String.format("%s: P=%d MW=%d ML=%d PS=%d PC=%d PD=%d NR=%.2f TP=%d",
                    team.getName(), matchesPlayed, matchWins, matchLosses,
                    pointsScored, pointsConceded, getPointDifference(), getNetRatio(), tournamentPoints);
        }
    }

    private MatchDTO convertToDTO(Match match) {
        // Use your existing convertToDTO method from MatchService
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setTeam1Id(match.getTeam1().getId());
        dto.setTeam1Name(match.getTeam1().getName());
        dto.setTeam2Id(match.getTeam2().getId());
        dto.setTeam2Name(match.getTeam2().getName());
        dto.setPoolId(match.getPool().getId());
        dto.setPoolName(match.getPool().getName());
        dto.setScheduledTime(match.getScheduledTime());
        dto.setStartTime(match.getStartTime());
        dto.setEndTime(match.getEndTime());
        dto.setStatus(match.getStatus());
        dto.setMatchOrder(match.getMatchOrder());
        dto.setTeam1Score(match.getTeam1Score());
        dto.setTeam2Score(match.getTeam2Score());
        dto.setNotes(match.getNotes());
        dto.setCourtNumber(match.getCourtNumber());
        dto.setMatchType(match.getMatchType());
        dto.setMatchName(match.getMatchName());
        dto.setRoundNumber(match.getRoundNumber());

        if (match.getWinner() != null) {
            dto.setWinnerId(match.getWinner().getId());
            dto.setWinnerName(match.getWinner().getName());
        }

        return dto;
    }

    private TeamDTO convertTeamToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());

        // Add pool information if available
        if (team.getPool() != null) {
            dto.setPoolId(team.getPool().getId());
            dto.setPoolName(team.getPool().getName());

            // Add tournament information
            if (team.getPool().getTournament() != null) {
                dto.setTournamentId(team.getPool().getTournament().getId());
                dto.setTournamentName(team.getPool().getTournament().getTitle());
            }
        }

        return dto;
    }

    @Transactional
    public int deleteQualifierMatches(Long tournamentId) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Find all qualifier matches for this tournament
        List<Match> qualifierMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.QUALIFIER);

        if (qualifierMatches.isEmpty()) {
            throw new RuntimeException("No qualifier matches found to delete");
        }

        // Check if any qualifier matches are completed
        boolean hasCompletedMatches = qualifierMatches.stream()
                .anyMatch(match -> match.getStatus() == MatchStatus.COMPLETED);

        if (hasCompletedMatches) {
            // You can either prevent deletion or allow it - your choice
            // Option 1: Prevent deletion
            // throw new RuntimeException("Cannot delete qualifier matches: Some matches are already completed");

            // Option 2: Allow deletion but warn (current implementation)
            System.out.println("Warning: Deleting completed qualifier matches for tournament " + tournamentId);
        }

        int deletedCount = qualifierMatches.size();
        matchRepository.deleteAll(qualifierMatches);

        System.out.println("Deleted " + deletedCount + " qualifier matches for tournament " + tournamentId);
        return deletedCount;
    }

    @Transactional
    public int deleteSemifinalMatches(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> semifinalMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.SEMIFINAL);

        if (semifinalMatches.isEmpty()) {
            throw new RuntimeException("No semifinal matches found to delete");
        }

        int deletedCount = semifinalMatches.size();
        matchRepository.deleteAll(semifinalMatches);

        System.out.println("Deleted " + deletedCount + " semifinal matches for tournament " + tournamentId);
        return deletedCount;
    }

    @Transactional
    public int deleteFinalMatches(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> finalMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.FINAL);

        if (finalMatches.isEmpty()) {
            throw new RuntimeException("No final matches found to delete");
        }

        int deletedCount = finalMatches.size();
        matchRepository.deleteAll(finalMatches);

        System.out.println("Deleted " + deletedCount + " final matches for tournament " + tournamentId);
        return deletedCount;
    }

    @Transactional
    public Map<String, Integer> deleteAllKnockoutMatches(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Get counts before deletion
        List<Match> qualifierMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.QUALIFIER);
        List<Match> semifinalMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.SEMIFINAL);
        List<Match> finalMatches = matchRepository.findMatchesByTournamentAndType(
                tournamentId, MatchType.FINAL);

        int qualifierCount = qualifierMatches.size();
        int semifinalCount = semifinalMatches.size();
        int finalCount = finalMatches.size();

        // Delete all knockout matches
        List<Match> allKnockoutMatches = new ArrayList<>();
        allKnockoutMatches.addAll(qualifierMatches);
        allKnockoutMatches.addAll(semifinalMatches);
        allKnockoutMatches.addAll(finalMatches);

        if (!allKnockoutMatches.isEmpty()) {
            matchRepository.deleteAll(allKnockoutMatches);
        }

        Map<String, Integer> deletedCounts = new HashMap<>();
        deletedCounts.put("qualifiers", qualifierCount);
        deletedCounts.put("semifinals", semifinalCount);
        deletedCounts.put("finals", finalCount);
        deletedCounts.put("total", qualifierCount + semifinalCount + finalCount);

        System.out.println("Deleted all knockout matches for tournament " + tournamentId +
                          ": " + deletedCounts.get("total") + " total matches");

        return deletedCounts;
    }
    
    // Add this public method to your KnockoutService class:

    public List<TeamDTO> getPoolRankings(Long poolId, Integer count) {
        // If count is not specified, return all teams
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));
        
        int teamCount = count != null ? count : pool.getTeams().size();
        
        List<Team> topTeams = getTopTeamsFromPool(poolId, teamCount);
        return topTeams.stream().map(this::convertTeamToDTO).collect(Collectors.toList());
    }

    public List<TeamStandingDTO> getDetailedPoolRankings(Long poolId) {
        // This uses the existing getPoolStandingsFromKnockoutService method
        // which provides more detailed statistics
        return getPoolStandingsFromKnockoutService(poolId);
    }
}
