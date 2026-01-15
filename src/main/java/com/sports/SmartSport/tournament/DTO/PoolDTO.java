
package com.sports.SmartSport.tournament.DTO;

import java.util.List;

public class PoolDTO {
    private Long id;
    private String name;
    private Long tournamentId;
    private String tournamentName;
    private List<TeamDTO> teams;
    private int teamCount;

    public PoolDTO() {}

    public PoolDTO(Long id, String name, Long tournamentId, String tournamentName) {
        this.id = id;
        this.name = name;
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public List<TeamDTO> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamDTO> teams) {
        this.teams = teams;
        this.teamCount = teams != null ? teams.size() : 0;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        this.teamCount = teamCount;
    }
}
