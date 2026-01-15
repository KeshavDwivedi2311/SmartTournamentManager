
package com.sports.SmartSport.tournament.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateTournamentRequest {
    private String title;
    private LocalDate startDate;
    private String venue;
    private Integer maxTeams;
    private String tournamentType;
    private String status;
    private String description;
    private List<PoolRequest> pools;
    
    @Data
    public static class PoolRequest {
        private String name;
        private List<TeamRequest> teams;
    }
    
    @Data
    public static class TeamRequest {
        private String name;
        private Integer gamesPlayed = 0;
        private Boolean isActive = true;
    }
}
