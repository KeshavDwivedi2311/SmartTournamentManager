package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.tournament.DTO.TeamStandingDTO;
import com.sports.SmartSport.tournament.Repository.PoolRepository;
import com.sports.SmartSport.tournament.Repository.TournamentRepository;
import com.sports.SmartSport.tournament.entity.Pool;
import com.sports.SmartSport.tournament.entity.Tournament;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PoolService {
    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;


    @Autowired
    private KnockoutService knockoutService;

    public Map<String, List<TeamStandingDTO>> getPoolStandings(Long tournamentId) {
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        Map<String, List<TeamStandingDTO>> allStandings = new HashMap<>();

        for (Pool pool : pools) {
            List<TeamStandingDTO> poolStandings = knockoutService.getPoolStandingsFromKnockoutService(pool.getId());
            allStandings.put(pool.getName(), poolStandings);
        }

        return allStandings;
    }

    public List<TeamStandingDTO> getSinglePoolStandings(Long poolId) {
        return knockoutService.getPoolStandingsFromKnockoutService(poolId);
    }
    public Pool createPool(Pool pool, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found with id: " + tournamentId));

        // Check for duplicate pool name (case-insensitive)
        if (poolRepository.existsByNameIgnoreCaseAndTournamentId(pool.getName(), tournamentId)) {
            throw new RuntimeException("Pool with name '" + pool.getName() + "' already exists in this tournament");
        }

        // Validate pool name
        if (pool.getName() == null || pool.getName().trim().isEmpty()) {
            throw new RuntimeException("Pool name cannot be empty");
        }

        Pool poolCreated = new Pool(pool.getName().trim(), tournament);
        return poolRepository.save(pool);
    }

    public List<Pool> getPoolsByTournament(Long tournamentId) {
        return poolRepository.findByTournamentIdWithTeams(tournamentId);
    }

    public Optional<Pool> getPoolById(Long poolId) {
        return poolRepository.findById(poolId);
    }

    public Pool updatePool(Long poolId, Pool poolDetails) {
        Optional<Pool> existingPool = poolRepository.findById(poolId);
        if (existingPool.isEmpty()) {
            throw new RuntimeException("Pool not found with id: " + poolId);
        }

        Pool pool = existingPool.get();
        pool.setName(poolDetails.getName());
        pool.setDescription(poolDetails.getDescription());
        pool.setMaxTeams(poolDetails.getMaxTeams());

        return poolRepository.save(pool);
    }

    public boolean deletePool(Long poolId) {
        Optional<Pool> pool = poolRepository.findById(poolId);
        if (pool.isEmpty()) {
            return false;
        }

        // First, remove pool reference from all teams in this pool
        List<Team> teams = teamRepository.findByPoolId(poolId);
        for (Team team : teams) {
            team.setPool(null);
            teamRepository.save(team);
        }

        poolRepository.deleteById(poolId);
        return true;
    }

    public Pool assignTeamToPool(Long poolId, Long teamId) {
        Optional<Pool> poolOpt = poolRepository.findById(poolId);
        Optional<Team> teamOpt = teamRepository.findById(teamId);

        if (poolOpt.isEmpty()) {
            throw new RuntimeException("Pool not found with id: " + poolId);
        }
        if (teamOpt.isEmpty()) {
            throw new RuntimeException("Team not found with id: " + teamId);
        }

        Pool pool = poolOpt.get();
        Team team = teamOpt.get();

        // Check if pool has space
        Long currentTeamCount = poolRepository.countTeamsByPoolId(poolId);
        if (pool.getMaxTeams() != null && currentTeamCount >= pool.getMaxTeams()) {
            throw new RuntimeException("Pool is full. Maximum teams: " + pool.getMaxTeams());
        }

        team.setPool(pool);
        teamRepository.save(team);

        return poolRepository.findById(poolId).orElse(pool);
    }

    public Pool removeTeamFromPool(Long poolId, Long teamId) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            throw new RuntimeException("Team not found with id: " + teamId);
        }

        Team team = teamOpt.get();
        team.setPool(null);
        teamRepository.save(team);

        return poolRepository.findById(poolId).orElse(null);
    }

}
