
package com.sports.SmartSport.tournament.DTO;

import java.util.List;

public class PoolStandingsDTO {
    private Long poolId;
    private String poolName;
    private List<TeamStandingDTO> standings;

    public PoolStandingsDTO() {}

    public PoolStandingsDTO(Long poolId, String poolName, List<TeamStandingDTO> standings) {
        this.poolId = poolId;
        this.poolName = poolName;
        this.standings = standings;
    }

    // Getters and Setters
    public Long getPoolId() {
        return poolId;
    }

    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public List<TeamStandingDTO> getStandings() {
        return standings;
    }

    public void setStandings(List<TeamStandingDTO> standings) {
        this.standings = standings;
    }
}
