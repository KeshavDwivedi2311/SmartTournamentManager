
package com.sports.SmartSport.tournament.DTO;

public class TeamStandingDTO {
    private Long teamId;
    private String teamName;
    private Long poolId;
    private String poolName;
    private int position;
    private int matchesPlayed;
    private int wins;
    private int losses;
    private int pointsScored;
    private int pointsConceded;
    private int pointDifference;
    private double netRatio; // Points scored / Points conceded
    private int tournamentPoints; // 3 for win, 0 for loss
    private String form; // Last 5 matches: "WWLWL"
    
    // Constructors
    public TeamStandingDTO() {}
    
    public TeamStandingDTO(Long teamId, String teamName, Long poolId, String poolName) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.poolId = poolId;
        this.poolName = poolName;
    }
    
    // Getters and Setters
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    
    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    
    public String getPoolName() { return poolName; }
    public void setPoolName(String poolName) { this.poolName = poolName; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    
    public int getPointsScored() { return pointsScored; }
    public void setPointsScored(int pointsScored) { this.pointsScored = pointsScored; }
    
    public int getPointsConceded() { return pointsConceded; }
    public void setPointsConceded(int pointsConceded) { this.pointsConceded = pointsConceded; }
    
    public int getPointDifference() { return pointDifference; }
    public void setPointDifference(int pointDifference) { this.pointDifference = pointDifference; }
    
    public double getNetRatio() { return netRatio; }
    public void setNetRatio(double netRatio) { this.netRatio = netRatio; }
    
    public int getTournamentPoints() { return tournamentPoints; }
    public void setTournamentPoints(int tournamentPoints) { this.tournamentPoints = tournamentPoints; }
    
    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }
    
    // Helper methods
    public void calculateDifferencesAndRatio() {
        this.pointDifference = this.pointsScored - this.pointsConceded;
        this.netRatio = this.pointsConceded > 0 ? 
                       (double) this.pointsScored / this.pointsConceded : 
                       this.pointsScored > 0 ? Double.MAX_VALUE : 0.0;
    }
    
    public double getWinPercentage() {
        if (matchesPlayed == 0) return 0.0;
        return (double) wins / matchesPlayed * 100;
    }
    
    public double getAveragePointsScored() {
        if (matchesPlayed == 0) return 0.0;
        return (double) pointsScored / matchesPlayed;
    }
    
    public double getAveragePointsConceded() {
        if (matchesPlayed == 0) return 0.0;
        return (double) pointsConceded / matchesPlayed;
    }
}
