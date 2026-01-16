package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.tournament.DTO.CreateTournamentRequest;
import com.sports.SmartSport.tournament.DTO.PoolDTO;
import com.sports.SmartSport.tournament.DTO.PoolStandingsDTO;
import com.sports.SmartSport.tournament.DTO.TeamDTO;
import com.sports.SmartSport.tournament.DTO.TeamStandingDTO;
import com.sports.SmartSport.tournament.Repository.MatchRepository;
import com.sports.SmartSport.tournament.Repository.PoolRepository;
import com.sports.SmartSport.tournament.Repository.TournamentRepository;
import com.sports.SmartSport.tournament.entity.Match;
import com.sports.SmartSport.tournament.entity.MatchStatus;
import com.sports.SmartSport.tournament.entity.Pool;
import com.sports.SmartSport.tournament.entity.Tournament;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TournamentService {
    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private KnockoutService knockoutService;

    public Pool createPool(String poolName, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found with id: " + tournamentId));

        // Check for duplicate pool name (case-insensitive)
        if (poolRepository.existsByNameIgnoreCaseAndTournamentId(poolName, tournamentId)) {
            throw new RuntimeException("Pool with name '" + poolName + "' already exists in this tournament");
        }

        // Validate pool name
        if (poolName == null || poolName.trim().isEmpty()) {
            throw new RuntimeException("Pool name cannot be empty");
        }

        Pool pool = new Pool(poolName.trim(), tournament);
        return poolRepository.save(pool);
    }

    public List<Pool> getPoolsByTournament(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new RuntimeException("Tournament not found with id: " + tournamentId);
        }
        return poolRepository.findByTournamentId(tournamentId);
    }

    public Team addTeamToPool(String teamName, Long poolId) {
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));

        Team team = new Team(teamName, pool);
        return teamRepository.save(team);
    }

    public List<Match> scheduleMatches(Long poolId) {
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));

        List<Team> teams = teamRepository.findByPoolIdAndIsActiveTrue(poolId);

        if (teams.size() < 2) {
            throw new RuntimeException("Need at least 2 teams to schedule matches");
        }

        return generateRoundRobinSchedule(pool, teams);
    }
    private List<Match> generateRoundRobinSchedule(Pool pool, List<Team> teams) {
        List<Match> matches = new ArrayList<>();
        int poolSize = teams.size();
        int matchOrder = 1;

        // Create a circular scheduling algorithm
        List<Team> schedule = new ArrayList<>(teams);

        // If odd number of teams, add a "bye" (null)
        if (poolSize % 2 == 1) {
            schedule.add(null);
        }

        int totalRounds = schedule.size() - 1;
        int matchesPerRound = schedule.size() / 2;

        for (int round = 0; round < totalRounds; round++) {
            for (int match = 0; match < matchesPerRound; match++) {
                Team team1 = schedule.get(match);
                Team team2 = schedule.get(schedule.size() - 1 - match);

                // Skip if either team is null (bye)
                if (team1 != null && team2 != null) {
                    // Check if these teams have already played
                    if (!matchRepository.existsMatchBetweenTeams(pool.getId(), team1.getId(), team2.getId())) {
                        Match newMatch = new Match(team1, team2, pool, matchOrder++);
                        matches.add(newMatch);

                        // Update games played count
                        team1.incrementGamesPlayed();
                        team2.incrementGamesPlayed();
                    }
                }
            }

            // Rotate teams for next round (keep first team fixed, rotate others)
            if (schedule.size() > 2) {
                Team temp = schedule.get(1);
                for (int i = 1; i < schedule.size() - 1; i++) {
                    schedule.set(i, schedule.get(i + 1));
                }
                schedule.set(schedule.size() - 1, temp);
            }
        }

        // Save all matches and update teams
        matchRepository.saveAll(matches);
        teamRepository.saveAll(teams);

        return matches;
    }

    public List<Match> getOptimizedSchedule(Long poolId) {
        List<Team> activeTeams = teamRepository.findByPoolIdAndIsActiveTrue(poolId);
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found"));

        return generateOptimizedSchedule(pool, activeTeams);
    }

    private List<Match> generateOptimizedSchedule(Pool pool, List<Team> teams) {
        List<Match> matches = new ArrayList<>();
        Deque<Team> teamQueue = new ArrayDeque<>(teams);
        Set<String> playedPairs = new HashSet<>();
        int matchOrder = 1;
        int poolSize = teams.size();

        while (!teamQueue.isEmpty() && teamQueue.size() >= 2) {
            Team team1 = teamQueue.pollFirst();
            Team team2 = findBestOpponent(team1, teamQueue, playedPairs, poolSize);

            if (team2 != null) {
                teamQueue.remove(team2);

                // Create match
                Match match = new Match(team1, team2, pool, matchOrder++);
                matches.add(match);

                // Mark this pair as played
                String pairKey = createPairKey(team1.getId(), team2.getId());
                playedPairs.add(pairKey);

                // Update games played
                team1.incrementGamesPlayed();
                team2.incrementGamesPlayed();

                // Add teams back to queue if they haven't completed all games
                if (!team1.hasCompletedAllGames(poolSize)) {
                    teamQueue.addLast(team1);
                }
                if (!team2.hasCompletedAllGames(poolSize)) {
                    teamQueue.addLast(team2);
                }
            } else {
                // No valid opponent found, add team back to end of queue
                teamQueue.addLast(team1);
            }
        }

        return matches;
    }

    private Team findBestOpponent(Team team1, Deque<Team> availableTeams, Set<String> playedPairs, int poolSize) {
        for (Team candidate : availableTeams) {
            String pairKey = createPairKey(team1.getId(), candidate.getId());

            // Check if they haven't played before and both haven't completed all games
            if (!playedPairs.contains(pairKey) &&
                    !team1.hasCompletedAllGames(poolSize) &&
                    !candidate.hasCompletedAllGames(poolSize)) {
                return candidate;
            }
        }
        return null;
    }

    private String createPairKey(Long team1Id, Long team2Id) {
        long min = Math.min(team1Id, team2Id);
        long max = Math.max(team1Id, team2Id);
        return min + "-" + max;
    }

    public Tournament createTournament(Tournament tournament){
        return tournamentRepository.save(tournament);
    }


    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return Optional.ofNullable(tournamentRepository.findById(id).orElse(null));
    }

    @Transactional
    public Tournament createCompleteTournament(CreateTournamentRequest request) {
        // Create and save tournament first
        Tournament tournament = new Tournament();
        tournament.setTitle(request.getTitle());
        tournament.setStartDate(request.getStartDate());
        tournament.setVenue(request.getVenue());
        tournament.setMaxTeams(request.getMaxTeams());
        tournament.setTournamentType(request.getTournamentType());
        tournament.setStatus(request.getStatus());
        tournament.setDescription(request.getDescription());

        Tournament savedTournament = tournamentRepository.save(tournament);

        // Process pools
        if (request.getPools() != null) {
            for (CreateTournamentRequest.PoolRequest poolRequest : request.getPools()) {
                Pool pool = new Pool();
                pool.setName(poolRequest.getName());
                pool.setTournament(savedTournament);

                Pool savedPool = poolRepository.save(pool);

                // Process teams
                if (poolRequest.getTeams() != null) {
                    for (CreateTournamentRequest.TeamRequest teamRequest : poolRequest.getTeams()) {
                        Team team = new Team();
                        team.setName(teamRequest.getName());
                        team.setGamesPlayed(teamRequest.getGamesPlayed());
                        team.setActive(teamRequest.getIsActive());
                        team.setTournament(savedTournament);
                        team.setPool(savedPool);

                        teamRepository.save(team);
                    }
                }
            }
        }

        // Return the complete tournament with all relationships loaded
        return tournamentRepository.findById(savedTournament.getId()).orElse(savedTournament);
    }

    public List<PoolStandingsDTO> getPoolStandings(Long tournamentId) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Get all pools for this tournament
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);

        List<PoolStandingsDTO> poolStandingsList = new ArrayList<>();

        for (Pool pool : pools) {
            // Use the existing method from KnockoutService to get standings
            List<TeamStandingDTO> standings = knockoutService.getPoolStandingsFromKnockoutService(pool.getId());

            PoolStandingsDTO poolStandings = new PoolStandingsDTO(
                    pool.getId(),
                    pool.getName(),
                    standings
            );

            poolStandingsList.add(poolStandings);
        }

        return poolStandingsList;
    }

    public List<PoolDTO> getTournamentPools(Long tournamentId) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Get all pools for this tournament
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);

        return pools.stream()
                .map(this::convertPoolToDTO)
                .collect(Collectors.toList());
    }

    private PoolDTO convertPoolToDTO(Pool pool) {
        PoolDTO dto = new PoolDTO(
                pool.getId(),
                pool.getName(),
                pool.getTournament().getId(),
                pool.getTournament().getTitle()
        );

        // Convert teams to DTOs
        List<TeamDTO> teamDTOs = pool.getTeams().stream()
                .map(this::convertTeamToDTO)
                .collect(Collectors.toList());

        dto.setTeams(teamDTOs);

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

    @Transactional
    public boolean deleteTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found with id: " + tournamentId));

        // Check if tournament has any matches that are completed
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        boolean hasCompletedMatches = pools.stream()
                .anyMatch(pool -> matchRepository.existsByPoolIdAndStatus(pool.getId(), MatchStatus.COMPLETED));

        if (hasCompletedMatches) {
            throw new RuntimeException("Cannot delete tournament with completed matches");
        }

        // Delete in order: matches -> teams -> pools -> tournament
        for (Pool pool : pools) {
            // Delete all matches in this pool
            matchRepository.deleteByPoolId(pool.getId());

            // Delete all teams in this pool
            teamRepository.deleteByPoolId(pool.getId());
        }

        // Delete all pools
        poolRepository.deleteByTournamentId(tournamentId);

        // Finally delete the tournament
        tournamentRepository.delete(tournament);

        return true;
    }

    @Transactional
    public boolean deletePool(Long poolId) {
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found with id: " + poolId));

        // Check if pool has any completed matches
        boolean hasCompletedMatches = matchRepository.existsByPoolIdAndStatus(poolId, MatchStatus.COMPLETED);

        if (hasCompletedMatches) {
            throw new RuntimeException("Cannot delete pool with completed matches");
        }

        // Delete matches first, then teams, then pool
        matchRepository.deleteByPoolId(poolId);
        teamRepository.deleteByPoolId(poolId);
        poolRepository.delete(pool);

        return true;
    }

    @Transactional
    public boolean deleteTeamFromPool(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        // Check if team has any completed matches
        boolean hasCompletedMatches = matchRepository.existsByTeamIdAndStatus(teamId, MatchStatus.COMPLETED);

        if (hasCompletedMatches) {
            throw new RuntimeException("Cannot delete team with completed matches");
        }

        // Delete all matches involving this team
        matchRepository.deleteByTeam1IdOrTeam2Id(teamId, teamId);

        // Delete the team
        teamRepository.delete(team);

        return true;
    }

    @Transactional
    public boolean deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        // Check if match is completed
        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new RuntimeException("Cannot delete completed match");
        }

        matchRepository.delete(match);
        return true;
    }

    @Transactional
    public int deleteAllPoolMatches(Long poolId) {
        Pool pool = poolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("Pool not found with id: " + poolId));

        // Only get LEAGUE matches (exclude knockout matches)
        List<Match> matches = matchRepository.findPoolMatchesByPoolId(poolId);

        // Check if any matches are completed
        boolean hasCompletedMatches = matches.stream()
                .anyMatch(match -> match.getStatus() == MatchStatus.COMPLETED);

        if (hasCompletedMatches) {
            throw new RuntimeException("Cannot delete pool matches: Some matches are already completed");
        }

        int deletedCount = matches.size();
        matchRepository.deleteByPoolId(poolId);

        return deletedCount;
    }
}
