
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
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;

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

    @Autowired
    private TournamentConfigService tournamentConfigService;

    // =====================================================================
    // GET KNOCKOUT DATA
    // =====================================================================

    public Map<String, List<MatchDTO>> getKnockoutMatches(Long tournamentId) {
        Map<String, List<MatchDTO>> knockoutData = new LinkedHashMap<>();

        knockoutData.put("qualifiers", getMatchesByType(tournamentId, MatchType.QUALIFIER));
        knockoutData.put("quarterfinals", getMatchesByType(tournamentId, MatchType.QUARTERFINAL));
        knockoutData.put("semifinals", getMatchesByType(tournamentId, MatchType.SEMIFINAL));
        knockoutData.put("finals", getMatchesByType(tournamentId, MatchType.FINAL));
        knockoutData.put("thirdPlace", getMatchesByType(tournamentId, MatchType.THIRD_PLACE));
        knockoutData.put("customMatches", getMatchesByType(tournamentId, MatchType.CUSTOM));

        return knockoutData;
    }

    private List<MatchDTO> getMatchesByType(Long tournamentId, MatchType matchType) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Long> poolIds = pools.stream().map(Pool::getId).collect(Collectors.toList());
        if (poolIds.isEmpty()) return new ArrayList<>();

        List<Match> matches = matchRepository.findByPoolIdInAndMatchTypeOrderByRoundNumberAscMatchOrderAsc(poolIds, matchType);
        return matches.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // =====================================================================
    // QUALIFIED TEAMS
    // =====================================================================

    public List<TeamDTO> getQualifiedTeams(Long tournamentId, int teamsPerPool) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Team> qualifiedTeams = new ArrayList<>();

        for (Pool pool : pools) {
            int poolSize = pool.getTeams().size();
            int qualifiedCount = Math.min(teamsPerPool, poolSize);

            if (qualifiedCount > 0) {
                List<Team> poolTeams = getTopTeamsFromPool(pool.getId(), qualifiedCount);
                qualifiedTeams.addAll(poolTeams);
            }
        }

        return qualifiedTeams.stream().map(this::convertTeamToDTO).collect(Collectors.toList());
    }

    public List<TeamDTO> getQualifiedTeams(Long tournamentId) {
        TournamentConfig config = tournamentConfigService.getConfig(tournamentId);
        int qualifiersPerPool = config != null && config.getQualifiersPerPool() != null 
            ? config.getQualifiersPerPool() 
            : 4;
        return getQualifiedTeams(tournamentId, qualifiersPerPool);
    }

    // =====================================================================
    // QUALIFIER GENERATION — FLEXIBLE
    // =====================================================================

    /**
     * NEW: Flexible qualifier generation supporting per-pool counts, pool selection,
     * and multiple pairing strategies (CROSS_POOL, RANKING_BASED, RANDOM).
     */
    @Transactional
    public void generateQualifierMatches(Long tournamentId, QualifierGenerationRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // 1. Determine which pools to use
        List<Pool> pools;
        if (request.getSelectedPoolIds() != null && !request.getSelectedPoolIds().isEmpty()) {
            pools = new ArrayList<>();
            for (Long poolId : request.getSelectedPoolIds()) {
                poolRepository.findById(poolId).ifPresent(pools::add);
            }
            // Verify all selected pools belong to this tournament
            pools = pools.stream()
                .filter(p -> p.getTournament().getId().equals(tournamentId))
                .collect(Collectors.toList());
        } else {
            pools = poolRepository.findByTournamentId(tournamentId);
        }

        if (pools.isEmpty()) {
            throw new RuntimeException("No pools found for qualifier generation");
        }

        // 2. Get qualifying teams from each pool with per-pool or uniform counts
        List<QualifiedTeam> allQualifiedTeams = new ArrayList<>();
        for (Pool pool : pools) {
            int count = request.getTeamsForPool(pool.getId());
            int poolSize = pool.getTeams().size();
            int qualifyCount = Math.min(count, poolSize);

            if (qualifyCount > 0) {
                // Get standings for global ranking data
                List<TeamStandingDTO> standings = getPoolStandingsFromKnockoutService(pool.getId());
                List<Team> topTeams = getTopTeamsFromPool(pool.getId(), qualifyCount);

                for (int i = 0; i < topTeams.size(); i++) {
                    Team team = topTeams.get(i);
                    TeamStandingDTO standing = (i < standings.size()) ? standings.get(i) : null;
                    allQualifiedTeams.add(new QualifiedTeam(team, pool.getId(), i + 1, standing));
                }
            }
        }

        if (allQualifiedTeams.size() < 2) {
            throw new RuntimeException("Need at least 2 qualified teams to generate qualifiers. Got: " + allQualifiedTeams.size());
        }

        // Use first pool to store qualifier matches (they are tournament-wide)
        Pool firstPool = pools.get(0);

        // 3. Create pairings based on selected strategy
        String strategy = request.getPairingStrategy();
        List<Match> matches;

        switch (strategy) {
            case "RANKING_BASED":
                matches = createRankingBasedPairings(allQualifiedTeams, firstPool);
                break;
            case "RANDOM":
                matches = createRandomPairings(allQualifiedTeams, request.getAvoidSamePool(), firstPool);
                break;
            case "CROSS_POOL":
            default:
                matches = createCrossPoolPairings(allQualifiedTeams, pools.size(), firstPool);
                break;
        }

        // 4. Save all matches
        for (Match match : matches) {
            matchRepository.save(match);
        }
    }

    /** Backward-compatible overload */
    public void generateQualifierMatches(Long tournamentId, int teamsPerPool) {
        QualifierGenerationRequest request = new QualifierGenerationRequest(teamsPerPool);
        generateQualifierMatches(tournamentId, request);
    }

    /** Backward-compatible overload */
    public void generateQualifierMatches(Long tournamentId) {
        generateQualifierMatches(tournamentId, 2);
    }

    // =====================================================================
    // PAIRING STRATEGIES
    // =====================================================================

    /**
     * RANKING_BASED: Rank all qualified teams globally by pool standings,
     * then seed into a bracket: #1 vs #N, #2 vs #(N-1), etc.
     * This ensures the best teams don't meet until later rounds.
     */
    private List<Match> createRankingBasedPairings(List<QualifiedTeam> teams, Pool pool) {
        // Sort all teams globally by their performance metrics
        teams.sort((t1, t2) -> {
            if (t1.standing != null && t2.standing != null) {
                // Primary: Tournament points (descending)
                int tpCompare = Integer.compare(t2.standing.getTournamentPoints(), t1.standing.getTournamentPoints());
                if (tpCompare != 0) return tpCompare;

                // Secondary: Point difference (descending)
                int pdCompare = Integer.compare(t2.standing.getPointDifference(), t1.standing.getPointDifference());
                if (pdCompare != 0) return pdCompare;

                // Tertiary: Net ratio (descending)
                int nrCompare = Double.compare(t2.standing.getNetRatio(), t1.standing.getNetRatio());
                if (nrCompare != 0) return nrCompare;

                // Quaternary: Points scored (descending)
                int psCompare = Integer.compare(t2.standing.getPointsScored(), t1.standing.getPointsScored());
                if (psCompare != 0) return psCompare;
            }
            // Final: pool rank
            return Integer.compare(t1.poolRank, t2.poolRank);
        });

        // Standard bracket seeding: #1 vs #last, #2 vs #(last-1), etc.
        List<Match> matches = new ArrayList<>();
        int n = teams.size();
        int matchOrder = 1;

        for (int i = 0; i < n / 2; i++) {
            QualifiedTeam team1 = teams.get(i);
            QualifiedTeam team2 = teams.get(n - 1 - i);
            matches.add(createQualifierMatchFromQT(team1, team2, matchOrder++, pool));
        }

        return matches;
    }

    /**
     * CROSS_POOL: Pair teams from different pools.
     * For 2 pools: A1 vs B4, A2 vs B3, B1 vs A4, B2 vs A3.
     * For 3+ pools: rank globally, seed into bracket, then swap if same-pool matchup.
     */
    private List<Match> createCrossPoolPairings(List<QualifiedTeam> teams, int poolCount, Pool pool) {
        if (poolCount < 2) {
            // Single pool: just use ranking-based
            return createRankingBasedPairings(teams, pool);
        }

        if (poolCount == 2) {
            // Classic 2-pool cross pairing
            return createTwoPoolCrossPairings(teams, pool);
        }

        // 3+ pools: Use ranking-based seeding, then swap to avoid same-pool
        return createMultiPoolCrossPairings(teams, pool);
    }

    /**
     * Classic 2-pool cross pairing: A1 vs B(last), A2 vs B(last-1), etc.
     * Then B1 vs A(last), B2 vs A(last-1), etc.
     */
    private List<Match> createTwoPoolCrossPairings(List<QualifiedTeam> teams, Pool pool) {
        // Group by pool
        Map<Long, List<QualifiedTeam>> byPool = new LinkedHashMap<>();
        for (QualifiedTeam t : teams) {
            byPool.computeIfAbsent(t.poolId, k -> new ArrayList<>()).add(t);
        }

        List<Map.Entry<Long, List<QualifiedTeam>>> entries = new ArrayList<>(byPool.entrySet());
        if (entries.size() < 2) {
            return createRankingBasedPairings(teams, pool);
        }

        List<QualifiedTeam> poolA = entries.get(0).getValue();
        List<QualifiedTeam> poolB = entries.get(1).getValue();

        List<Match> matches = new ArrayList<>();
        int matchOrder = 1;
        Set<Long> pairedTeamIds = new HashSet<>();

        // A[i] vs B[maxPairs-1-i] (A's best vs B's worst qualifier)
        int maxPairs = Math.min(poolA.size(), poolB.size());
        for (int i = 0; i < maxPairs; i++) {
            int oppositeIndex = maxPairs - 1 - i;
            QualifiedTeam team1 = poolA.get(i);
            QualifiedTeam team2 = poolB.get(oppositeIndex);

            if (!pairedTeamIds.contains(team1.team.getId()) && !pairedTeamIds.contains(team2.team.getId())) {
                matches.add(createQualifierMatchFromQT(team1, team2, matchOrder++, pool));
                pairedTeamIds.add(team1.team.getId());
                pairedTeamIds.add(team2.team.getId());
            }
        }

        // B[i] vs A[maxPairs-1-i] (B's best vs A's worst qualifier)
        for (int i = 0; i < maxPairs; i++) {
            int oppositeIndex = maxPairs - 1 - i;
            QualifiedTeam team1 = poolB.get(i);
            QualifiedTeam team2 = poolA.get(oppositeIndex);

            if (!pairedTeamIds.contains(team1.team.getId()) && !pairedTeamIds.contains(team2.team.getId())) {
                matches.add(createQualifierMatchFromQT(team1, team2, matchOrder++, pool));
                pairedTeamIds.add(team1.team.getId());
                pairedTeamIds.add(team2.team.getId());
            }
        }

        // Handle any remaining unpaired teams
        pairRemainingTeams(teams, pairedTeamIds, matches, matchOrder, pool, true);

        return matches;
    }

    /**
     * Multi-pool (3+) cross pairing:
     * 1. Rank all teams globally
     * 2. Create seeded bracket (#1 vs #N, #2 vs #(N-1), ...)
     * 3. Swap pairings if same-pool matchup detected
     */
    private List<Match> createMultiPoolCrossPairings(List<QualifiedTeam> teams, Pool pool) {
        // Sort globally by performance
        teams.sort((t1, t2) -> {
            if (t1.standing != null && t2.standing != null) {
                int tpCompare = Integer.compare(t2.standing.getTournamentPoints(), t1.standing.getTournamentPoints());
                if (tpCompare != 0) return tpCompare;
                int pdCompare = Integer.compare(t2.standing.getPointDifference(), t1.standing.getPointDifference());
                if (pdCompare != 0) return pdCompare;
                int nrCompare = Double.compare(t2.standing.getNetRatio(), t1.standing.getNetRatio());
                if (nrCompare != 0) return nrCompare;
                int psCompare = Integer.compare(t2.standing.getPointsScored(), t1.standing.getPointsScored());
                if (psCompare != 0) return psCompare;
            }
            return Integer.compare(t1.poolRank, t2.poolRank);
        });

        int n = teams.size();
        // Build initial bracket pairings: [0 vs n-1], [1 vs n-2], ...
        int[][] pairings = new int[n / 2][2];
        for (int i = 0; i < n / 2; i++) {
            pairings[i][0] = i;
            pairings[i][1] = n - 1 - i;
        }

        // Attempt to fix same-pool matchups by swapping opponents between pairs
        for (int i = 0; i < pairings.length; i++) {
            QualifiedTeam t1 = teams.get(pairings[i][0]);
            QualifiedTeam t2 = teams.get(pairings[i][1]);

            if (t1.poolId.equals(t2.poolId)) {
                // Try to swap with another pair
                boolean swapped = false;
                for (int j = i + 1; j < pairings.length && !swapped; j++) {
                    QualifiedTeam s1 = teams.get(pairings[j][0]);
                    QualifiedTeam s2 = teams.get(pairings[j][1]);

                    // Try swapping t2 with s1
                    if (!t1.poolId.equals(s1.poolId) && !t2.poolId.equals(s2.poolId)) {
                        // Swap: pair[i] becomes [t1, s1], pair[j] becomes [t2, s2]
                        int temp = pairings[i][1];
                        pairings[i][1] = pairings[j][0];
                        pairings[j][0] = temp;
                        swapped = true;
                    }
                    // Try swapping t2 with s2
                    else if (!t1.poolId.equals(s2.poolId) && !t2.poolId.equals(s1.poolId)) {
                        int temp = pairings[i][1];
                        pairings[i][1] = pairings[j][1];
                        pairings[j][1] = temp;
                        swapped = true;
                    }
                }
                // If no swap possible, leave the same-pool matchup (rare edge case)
            }
        }

        // Build matches from pairings
        List<Match> matches = new ArrayList<>();
        int matchOrder = 1;
        for (int[] pair : pairings) {
            QualifiedTeam team1 = teams.get(pair[0]);
            QualifiedTeam team2 = teams.get(pair[1]);
            matches.add(createQualifierMatchFromQT(team1, team2, matchOrder++, pool));
        }

        return matches;
    }

    /**
     * RANDOM: Shuffle teams and pair them, with optional same-pool avoidance.
     */
    private List<Match> createRandomPairings(List<QualifiedTeam> teams, boolean avoidSamePool, Pool pool) {
        List<QualifiedTeam> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);

        if (avoidSamePool) {
            shuffled = shuffleAvoidingSamePool(shuffled);
        }

        List<Match> matches = new ArrayList<>();
        int matchOrder = 1;

        for (int i = 0; i < shuffled.size() - 1; i += 2) {
            matches.add(createQualifierMatchFromQT(shuffled.get(i), shuffled.get(i + 1), matchOrder++, pool));
        }

        return matches;
    }

    /**
     * Attempts to arrange teams so that consecutive pairs are from different pools.
     * Uses a simple greedy approach: pick from different pools alternately.
     */
    private List<QualifiedTeam> shuffleAvoidingSamePool(List<QualifiedTeam> teams) {
        // Group by pool
        Map<Long, LinkedList<QualifiedTeam>> byPool = new LinkedHashMap<>();
        for (QualifiedTeam t : teams) {
            byPool.computeIfAbsent(t.poolId, k -> new LinkedList<>()).add(t);
        }

        // Shuffle within each pool
        for (LinkedList<QualifiedTeam> poolTeams : byPool.values()) {
            Collections.shuffle(poolTeams);
        }

        List<QualifiedTeam> result = new ArrayList<>();
        Long lastPoolId = null;
        int remaining = teams.size();

        while (remaining > 0) {
            boolean added = false;

            // Try to pick a team from a different pool than the last one
            for (Map.Entry<Long, LinkedList<QualifiedTeam>> entry : byPool.entrySet()) {
                if (!entry.getValue().isEmpty() && !entry.getKey().equals(lastPoolId)) {
                    QualifiedTeam t = entry.getValue().poll();
                    result.add(t);
                    lastPoolId = t.poolId;
                    remaining--;
                    added = true;
                    break;
                }
            }

            // If all remaining teams are from the same pool, just add them
            if (!added) {
                for (LinkedList<QualifiedTeam> poolTeams : byPool.values()) {
                    if (!poolTeams.isEmpty()) {
                        result.add(poolTeams.poll());
                        remaining--;
                        added = true;
                        break;
                    }
                }
            }

            if (!added) break; // safety
        }

        return result;
    }

    /**
     * Helper: pair any remaining unpaired teams.
     */
    private void pairRemainingTeams(List<QualifiedTeam> teams, Set<Long> pairedTeamIds,
                                     List<Match> matches, int matchOrder, Pool pool, boolean avoidSamePool) {
        List<QualifiedTeam> unpaired = teams.stream()
            .filter(t -> !pairedTeamIds.contains(t.team.getId()))
            .collect(Collectors.toList());

        for (int i = 0; i < unpaired.size() - 1; i += 2) {
            QualifiedTeam t1 = unpaired.get(i);
            QualifiedTeam t2 = unpaired.get(i + 1);

            // If avoidSamePool, try to find a better partner
            if (avoidSamePool && t1.poolId.equals(t2.poolId)) {
                for (int j = i + 2; j < unpaired.size(); j++) {
                    if (!t1.poolId.equals(unpaired.get(j).poolId)) {
                        // Swap
                        QualifiedTeam temp = unpaired.get(i + 1);
                        unpaired.set(i + 1, unpaired.get(j));
                        unpaired.set(j, temp);
                        t2 = unpaired.get(i + 1);
                        break;
                    }
                }
            }

            matches.add(createQualifierMatchFromQT(t1, t2, matchOrder++, pool));
            pairedTeamIds.add(t1.team.getId());
            pairedTeamIds.add(t2.team.getId());
        }
    }

    // =====================================================================
    // QUARTERFINAL GENERATION
    // =====================================================================

    /**
     * Generate quarterfinal matches from qualifier winners.
     * Used when there are more than 4 qualifier winners (e.g., 8+ qualifying teams).
     * Pairs: QF winner 1 vs QF winner 8, QF winner 2 vs QF winner 7, etc.
     */
    @Transactional
    public void generateQuarterfinalMatches(Long tournamentId) {
        List<Match> qualifierMatches = getCompletedMatchesByType(tournamentId, MatchType.QUALIFIER);

        List<Team> winners = qualifierMatches.stream()
            .sorted(Comparator.comparingInt(Match::getMatchOrder))
            .map(Match::getWinner)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (winners.size() < 4) {
            throw new RuntimeException("Need at least 4 qualifier winners to generate quarterfinals. Got: " + winners.size());
        }

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        // Bracket seeding: #1 vs #last, #2 vs #(last-1), ...
        int n = winners.size();
        int matchOrder = 1;

        for (int i = 0; i < n / 2; i++) {
            int team1Index = i;
            int team2Index = n - 1 - i;

            if (team1Index < team2Index) {
                Match qfMatch = new Match();
                qfMatch.setTeam1(winners.get(team1Index));
                qfMatch.setTeam2(winners.get(team2Index));
                qfMatch.setPool(firstPool);
                qfMatch.setMatchType(MatchType.QUARTERFINAL);
                qfMatch.setMatchName("Quarterfinal " + matchOrder);
                qfMatch.setRoundNumber(2);
                qfMatch.setMatchOrder(matchOrder);
                qfMatch.setStatus(MatchStatus.SCHEDULED);

                matchRepository.save(qfMatch);
                matchOrder++;
            }
        }
    }

    // =====================================================================
    // SEMIFINAL GENERATION — NOW FLEXIBLE
    // =====================================================================

    /**
     * Generate semifinal matches.
     * Automatically detects the previous round:
     * - If quarterfinals exist and are completed → use quarterfinal winners
     * - Otherwise → use qualifier winners
     * Handles any even number of winners (2, 4, 6, ...).
     */
    @Transactional
    public void generateSemifinalMatches(Long tournamentId) {
        // Check for completed quarterfinals first
        List<Match> quarterfinalMatches = getCompletedMatchesByType(tournamentId, MatchType.QUARTERFINAL);

        List<Team> winners;
        if (!quarterfinalMatches.isEmpty()) {
            // Use quarterfinal winners
            winners = quarterfinalMatches.stream()
                .sorted(Comparator.comparingInt(Match::getMatchOrder))
                .map(Match::getWinner)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } else {
            // Use qualifier winners
            List<Match> qualifierMatches = getCompletedMatchesByType(tournamentId, MatchType.QUALIFIER);

            if (qualifierMatches.size() < 2) {
                throw new RuntimeException("Need at least 2 completed matches from the previous round to generate semifinals");
            }

            winners = qualifierMatches.stream()
                .sorted(Comparator.comparingInt(Match::getMatchOrder))
                .map(Match::getWinner)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        if (winners.size() < 2) {
            throw new RuntimeException("Need at least 2 winners from the previous round to generate semifinals. Got: " + winners.size());
        }

        // For 4 winners: 2 semifinals (standard).
        // For 2 winners: 1 semifinal.
        // For more: bracket seeding.
        if (winners.size() > 4) {
            throw new RuntimeException("Too many winners (" + winners.size() + ") for semifinals. " +
                "Generate quarterfinals first to narrow down to 4 teams, or use custom pairings.");
        }

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);

        int n = winners.size();
        int matchOrder = 1;

        for (int i = 0; i < n / 2; i++) {
            int team1Index = i;
            int team2Index = n - 1 - i;

            if (team1Index < team2Index) {
                Match semifinalMatch = new Match();
                semifinalMatch.setTeam1(winners.get(team1Index));
                semifinalMatch.setTeam2(winners.get(team2Index));
                semifinalMatch.setPool(firstPool);
                semifinalMatch.setMatchType(MatchType.SEMIFINAL);
                semifinalMatch.setMatchName("Semifinal " + matchOrder);
                semifinalMatch.setRoundNumber(3);
                semifinalMatch.setMatchOrder(matchOrder);
                semifinalMatch.setStatus(MatchStatus.SCHEDULED);

                matchRepository.save(semifinalMatch);
                matchOrder++;
            }
        }
    }

    // =====================================================================
    // FINAL GENERATION — NOW FLEXIBLE
    // =====================================================================

    /**
     * Generate final and 3rd place matches from semifinal winners/losers.
     * Works with any number of completed semifinals (≥2).
     */
    @Transactional
    public void generateFinalMatches(Long tournamentId) {
        List<Match> semifinalMatches = getCompletedMatchesByType(tournamentId, MatchType.SEMIFINAL);

        if (semifinalMatches.size() < 2) {
            throw new RuntimeException("Need at least 2 completed semifinal matches to generate finals. Got: " + semifinalMatches.size());
        }

        // Sort by match order to ensure consistent bracket pairing
        semifinalMatches.sort(Comparator.comparingInt(Match::getMatchOrder));

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
            thirdPlaceMatch.setRoundNumber(4);
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
            finalMatch.setRoundNumber(4);
            finalMatch.setMatchOrder(2);
            finalMatch.setStatus(MatchStatus.SCHEDULED);

            matchRepository.save(finalMatch);
        }
    }

    // =====================================================================
    // CUSTOM MATCHES
    // =====================================================================

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
        customMatch.setRoundNumber(99);
        customMatch.setMatchOrder(1);
        customMatch.setStatus(MatchStatus.SCHEDULED);

        Match savedMatch = matchRepository.save(customMatch);
        return convertToDTO(savedMatch);
    }

    @Transactional
    public List<MatchDTO> createCustomQualifierPairings(Long tournamentId, CustomPairingRequest request) {
        List<Match> existingQualifiers = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.QUALIFIER);
        if (!existingQualifiers.isEmpty()) {
            matchRepository.deleteAll(existingQualifiers);
        }

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);
        List<MatchDTO> createdMatches = new ArrayList<>();

        for (CustomPairingRequest.Pairing pairing : request.getPairings()) {
            Team team1 = teamRepository.findById(pairing.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Team 1 not found: " + pairing.getTeam1Id()));
            Team team2 = teamRepository.findById(pairing.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Team 2 not found: " + pairing.getTeam2Id()));

            Match qualifierMatch = new Match();
            qualifierMatch.setTeam1(team1);
            qualifierMatch.setTeam2(team2);
            qualifierMatch.setPool(firstPool);
            qualifierMatch.setMatchType(MatchType.QUALIFIER);
            qualifierMatch.setMatchName(pairing.getMatchName() != null ? pairing.getMatchName() : "Qualifier " + pairing.getMatchOrder());
            qualifierMatch.setRoundNumber(1);
            qualifierMatch.setMatchOrder(pairing.getMatchOrder() != null ? pairing.getMatchOrder() : 1);
            qualifierMatch.setStatus(MatchStatus.SCHEDULED);

            Match savedMatch = matchRepository.save(qualifierMatch);
            createdMatches.add(convertToDTO(savedMatch));
        }

        return createdMatches;
    }

    @Transactional
    public List<MatchDTO> createCustomSemifinalPairings(Long tournamentId, CustomPairingRequest request) {
        List<Match> existingSemifinals = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.SEMIFINAL);
        if (!existingSemifinals.isEmpty()) {
            matchRepository.deleteAll(existingSemifinals);
        }

        Pool firstPool = poolRepository.findByTournamentId(tournamentId).get(0);
        List<MatchDTO> createdMatches = new ArrayList<>();

        for (CustomPairingRequest.Pairing pairing : request.getPairings()) {
            Team team1 = teamRepository.findById(pairing.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Team 1 not found: " + pairing.getTeam1Id()));
            Team team2 = teamRepository.findById(pairing.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Team 2 not found: " + pairing.getTeam2Id()));

            Match semifinalMatch = new Match();
            semifinalMatch.setTeam1(team1);
            semifinalMatch.setTeam2(team2);
            semifinalMatch.setPool(firstPool);
            semifinalMatch.setMatchType(MatchType.SEMIFINAL);
            semifinalMatch.setMatchName(pairing.getMatchName() != null ? pairing.getMatchName() : "Semifinal " + pairing.getMatchOrder());
            semifinalMatch.setRoundNumber(3);
            semifinalMatch.setMatchOrder(pairing.getMatchOrder() != null ? pairing.getMatchOrder() : 1);
            semifinalMatch.setStatus(MatchStatus.SCHEDULED);

            Match savedMatch = matchRepository.save(semifinalMatch);
            createdMatches.add(convertToDTO(savedMatch));
        }

        return createdMatches;
    }

    // =====================================================================
    // DELETE METHODS
    // =====================================================================

    @Transactional
    public int deleteQualifierMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> qualifierMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.QUALIFIER);
        if (qualifierMatches.isEmpty()) {
            throw new RuntimeException("No qualifier matches found to delete");
        }

        int deletedCount = qualifierMatches.size();
        matchRepository.deleteAll(qualifierMatches);
        return deletedCount;
    }

    @Transactional
    public int deleteQuarterfinalMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> qfMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.QUARTERFINAL);
        if (qfMatches.isEmpty()) {
            throw new RuntimeException("No quarterfinal matches found to delete");
        }

        int deletedCount = qfMatches.size();
        matchRepository.deleteAll(qfMatches);
        return deletedCount;
    }

    @Transactional
    public int deleteSemifinalMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> semifinalMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.SEMIFINAL);
        if (semifinalMatches.isEmpty()) {
            throw new RuntimeException("No semifinal matches found to delete");
        }

        int deletedCount = semifinalMatches.size();
        matchRepository.deleteAll(semifinalMatches);
        return deletedCount;
    }

    @Transactional
    public int deleteFinalMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Delete both FINAL and THIRD_PLACE matches
        List<Match> finalMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.FINAL);
        List<Match> thirdPlaceMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.THIRD_PLACE);

        List<Match> allFinalMatches = new ArrayList<>();
        allFinalMatches.addAll(finalMatches);
        allFinalMatches.addAll(thirdPlaceMatches);

        if (allFinalMatches.isEmpty()) {
            throw new RuntimeException("No final matches found to delete");
        }

        int deletedCount = allFinalMatches.size();
        matchRepository.deleteAll(allFinalMatches);
        return deletedCount;
    }

    @Transactional
    public Map<String, Integer> deleteAllKnockoutMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        List<Match> qualifierMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.QUALIFIER);
        List<Match> qfMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.QUARTERFINAL);
        List<Match> semifinalMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.SEMIFINAL);
        List<Match> finalMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.FINAL);
        List<Match> thirdPlaceMatches = matchRepository.findMatchesByTournamentAndType(tournamentId, MatchType.THIRD_PLACE);

        List<Match> allKnockoutMatches = new ArrayList<>();
        allKnockoutMatches.addAll(qualifierMatches);
        allKnockoutMatches.addAll(qfMatches);
        allKnockoutMatches.addAll(semifinalMatches);
        allKnockoutMatches.addAll(finalMatches);
        allKnockoutMatches.addAll(thirdPlaceMatches);

        if (!allKnockoutMatches.isEmpty()) {
            matchRepository.deleteAll(allKnockoutMatches);
        }

        Map<String, Integer> deletedCounts = new LinkedHashMap<>();
        deletedCounts.put("qualifiers", qualifierMatches.size());
        deletedCounts.put("quarterfinals", qfMatches.size());
        deletedCounts.put("semifinals", semifinalMatches.size());
        deletedCounts.put("finals", finalMatches.size());
        deletedCounts.put("thirdPlace", thirdPlaceMatches.size());
        deletedCounts.put("total", allKnockoutMatches.size());

        return deletedCounts;
    }

    // =====================================================================
    // POOL RANKINGS & STANDINGS
    // =====================================================================

    public List<TeamDTO> getPoolRankings(Long poolId, Integer count) {
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));

        int teamCount = count != null ? count : pool.getTeams().size();
        List<Team> topTeams = getTopTeamsFromPool(poolId, teamCount);
        return topTeams.stream().map(this::convertTeamToDTO).collect(Collectors.toList());
    }

    public List<TeamStandingDTO> getDetailedPoolRankings(Long poolId) {
        return getPoolStandingsFromKnockoutService(poolId);
    }

    private List<Team> getTopTeamsFromPool(Long poolId, int count) {
        List<Match> poolMatches = matchRepository.findPoolMatchesByPoolIdAndStatus(poolId, MatchStatus.COMPLETED);

        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));
        List<Team> allTeams = pool.getTeams();

        if (allTeams.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, BadmintonStanding> teamStandings = new HashMap<>();
        for (Team team : allTeams) {
            teamStandings.put(team.getId(), new BadmintonStanding(team));
        }

        for (Match match : poolMatches) {
            Team team1 = match.getTeam1();
            Team team2 = match.getTeam2();

            BadmintonStanding standing1 = teamStandings.get(team1.getId());
            BadmintonStanding standing2 = teamStandings.get(team2.getId());

            if (standing1 != null && standing2 != null) {
                standing1.matchesPlayed++;
                standing2.matchesPlayed++;

                int team1Score = match.getTeam1Score() != null ? match.getTeam1Score() : 0;
                int team2Score = match.getTeam2Score() != null ? match.getTeam2Score() : 0;

                standing1.pointsScored += team1Score;
                standing1.pointsConceded += team2Score;
                standing2.pointsScored += team2Score;
                standing2.pointsConceded += team1Score;

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

        return teamStandings.entrySet().stream()
                .sorted((e1, e2) -> {
                    BadmintonStanding s1 = e1.getValue();
                    BadmintonStanding s2 = e2.getValue();

                    int tournamentPointsCompare = Integer.compare(s2.tournamentPoints, s1.tournamentPoints);
                    if (tournamentPointsCompare != 0) return tournamentPointsCompare;

                    int pointDiffCompare = Integer.compare(s2.getPointDifference(), s1.getPointDifference());
                    if (pointDiffCompare != 0) return pointDiffCompare;

                    int netRatioCompare = Double.compare(s2.getNetRatio(), s1.getNetRatio());
                    if (netRatioCompare != 0) return netRatioCompare;

                    int pointsScoredCompare = Integer.compare(s2.pointsScored, s1.pointsScored);
                    if (pointsScoredCompare != 0) return pointsScoredCompare;

                    return s1.team.getName().compareTo(s2.team.getName());
                })
                .limit(count)
                .map(entry -> entry.getValue().team)
                .collect(Collectors.toList());
    }

    public List<TeamStandingDTO> getPoolStandingsFromKnockoutService(Long poolId) {
        List<Match> poolMatches = matchRepository.findPoolMatchesByPoolIdAndStatus(poolId, MatchStatus.COMPLETED);

        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));
        List<Team> allTeams = pool.getTeams();

        if (allTeams.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, BadmintonStanding> teamStandings = new HashMap<>();
        for (Team team : allTeams) {
            teamStandings.put(team.getId(), new BadmintonStanding(team));
        }

        for (Match match : poolMatches) {
            Team team1 = match.getTeam1();
            Team team2 = match.getTeam2();

            BadmintonStanding standing1 = teamStandings.get(team1.getId());
            BadmintonStanding standing2 = teamStandings.get(team2.getId());

            if (standing1 != null && standing2 != null) {
                standing1.matchesPlayed++;
                standing2.matchesPlayed++;

                int team1Score = match.getTeam1Score() != null ? match.getTeam1Score() : 0;
                int team2Score = match.getTeam2Score() != null ? match.getTeam2Score() : 0;

                standing1.pointsScored += team1Score;
                standing1.pointsConceded += team2Score;
                standing2.pointsScored += team2Score;
                standing2.pointsConceded += team1Score;

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

        List<TeamStandingDTO> standings = teamStandings.values().stream()
                .map(this::convertBadmintonStandingToDTO)
                .sorted((s1, s2) -> {
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

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
            standings.get(i).setForm(calculateTeamForm(standings.get(i).getTeamId(), poolMatches));
        }

        return standings;
    }

    // =====================================================================
    // HELPER METHODS & INNER CLASSES
    // =====================================================================

    private List<Match> getCompletedMatchesByType(Long tournamentId, MatchType matchType) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Long> poolIds = pools.stream().map(Pool::getId).collect(Collectors.toList());
        if (poolIds.isEmpty()) return new ArrayList<>();

        return matchRepository.findByPoolIdInAndMatchTypeAndStatusOrderByRoundNumberAscMatchOrderAsc(
            poolIds, matchType, MatchStatus.COMPLETED);
    }

    private Match createQualifierMatchFromQT(QualifiedTeam team1, QualifiedTeam team2, int matchOrder, Pool pool) {
        Match qualifierMatch = new Match();
        qualifierMatch.setTeam1(team1.team);
        qualifierMatch.setTeam2(team2.team);
        qualifierMatch.setPool(pool);
        qualifierMatch.setMatchType(MatchType.QUALIFIER);
        qualifierMatch.setMatchName("Qualifier " + matchOrder);
        qualifierMatch.setRoundNumber(1);
        qualifierMatch.setMatchOrder(matchOrder);
        qualifierMatch.setStatus(MatchStatus.SCHEDULED);
        return qualifierMatch;
    }

    private String calculateTeamForm(Long teamId, List<Match> poolMatches) {
        List<Match> teamMatches = poolMatches.stream()
                .filter(match -> match.getTeam1().getId().equals(teamId) ||
                        match.getTeam2().getId().equals(teamId))
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .sorted((m1, m2) -> {
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

        StringBuilder form = new StringBuilder();
        int matchesToShow = Math.min(5, teamMatches.size());

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
                form.append("D");
            }
        }

        return form.toString();
    }

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

    private MatchDTO convertToDTO(Match match) {
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

        if (team.getPool() != null) {
            dto.setPoolId(team.getPool().getId());
            dto.setPoolName(team.getPool().getName());

            if (team.getPool().getTournament() != null) {
                dto.setTournamentId(team.getPool().getTournament().getId());
                dto.setTournamentName(team.getPool().getTournament().getTitle());
            }
        }

        return dto;
    }

    // -- Inner Classes --

    /**
     * Represents a team that has qualified from a pool, carrying its ranking metadata.
     */
    private static class QualifiedTeam {
        Team team;
        Long poolId;
        int poolRank; // 1-based rank within their pool
        TeamStandingDTO standing; // standing data for global ranking

        QualifiedTeam(Team team, Long poolId, int poolRank, TeamStandingDTO standing) {
            this.team = team;
            this.poolId = poolId;
            this.poolRank = poolRank;
            this.standing = standing;
        }
    }

    private static class BadmintonStanding {
        Team team;
        int matchesPlayed = 0;
        int matchWins = 0;
        int matchLosses = 0;
        int pointsScored = 0;
        int pointsConceded = 0;
        int tournamentPoints = 0;

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
    }
}
