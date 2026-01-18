package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.tournament.Repository.TournamentConfigRepository;
import com.sports.SmartSport.tournament.Repository.TournamentRepository;
import com.sports.SmartSport.tournament.entity.Tournament;
import com.sports.SmartSport.tournament.entity.TournamentConfig;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class TournamentConfigService {
    
    @Autowired
    private TournamentConfigRepository configRepository;
    
    @Autowired
    private TournamentRepository tournamentRepository;
    
    /**
     * Get or create default configuration for a tournament
     */
    public TournamentConfig getOrCreateConfig(Long tournamentId) {
        Optional<TournamentConfig> existing = configRepository.findByTournamentId(tournamentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create default configuration
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        
        TournamentConfig config = new TournamentConfig();
        config.setTournament(tournament);
        config.setPoolMatchPoints(15);
        config.setKnockoutMatchPoints(21);
        config.setBreakTimeMinutes(2);
        config.setEstimatedMatchDurationMinutes(15);
        config.setQualifiersPerPool(4); // Default: 4 teams qualify from each pool
        
        // Default court schedule: 4, 4, 3, 2 courts per hour
        String defaultSchedule = "[{\"startTime\":\"09:00\",\"courts\":4},{\"startTime\":\"10:00\",\"courts\":4},{\"startTime\":\"11:00\",\"courts\":3},{\"startTime\":\"12:00\",\"courts\":2}]";
        config.setCourtSchedule(defaultSchedule);
        
        return configRepository.save(config);
    }
    
    /**
     * Update tournament configuration
     */
    public TournamentConfig updateConfig(Long tournamentId, TournamentConfig updatedConfig) {
        TournamentConfig config = getOrCreateConfig(tournamentId);
        
        if (updatedConfig.getPoolMatchPoints() != null) {
            config.setPoolMatchPoints(updatedConfig.getPoolMatchPoints());
        }
        if (updatedConfig.getKnockoutMatchPoints() != null) {
            config.setKnockoutMatchPoints(updatedConfig.getKnockoutMatchPoints());
        }
        if (updatedConfig.getBreakTimeMinutes() != null) {
            config.setBreakTimeMinutes(updatedConfig.getBreakTimeMinutes());
        }
        if (updatedConfig.getEstimatedMatchDurationMinutes() != null) {
            config.setEstimatedMatchDurationMinutes(updatedConfig.getEstimatedMatchDurationMinutes());
        }
        if (updatedConfig.getCourtSchedule() != null) {
            config.setCourtSchedule(updatedConfig.getCourtSchedule());
        }
        if (updatedConfig.getTournamentStartTime() != null) {
            config.setTournamentStartTime(updatedConfig.getTournamentStartTime());
        }
        if (updatedConfig.getQualifiersPerPool() != null) {
            config.setQualifiersPerPool(updatedConfig.getQualifiersPerPool());
        }
        
        return configRepository.save(config);
    }
    
    /**
     * Get configuration for a tournament
     */
    public TournamentConfig getConfig(Long tournamentId) {
        return getOrCreateConfig(tournamentId);
    }
}
