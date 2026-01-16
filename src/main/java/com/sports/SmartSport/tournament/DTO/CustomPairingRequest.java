package com.sports.SmartSport.tournament.DTO;

import java.util.List;

public class CustomPairingRequest {
    private List<Pairing> pairings;
    private String matchType; // QUALIFIER or SEMIFINAL

    public static class Pairing {
        private Long team1Id;
        private Long team2Id;
        private String matchName;
        private Integer matchOrder;

        // Getters and setters
        public Long getTeam1Id() {
            return team1Id;
        }

        public void setTeam1Id(Long team1Id) {
            this.team1Id = team1Id;
        }

        public Long getTeam2Id() {
            return team2Id;
        }

        public void setTeam2Id(Long team2Id) {
            this.team2Id = team2Id;
        }

        public String getMatchName() {
            return matchName;
        }

        public void setMatchName(String matchName) {
            this.matchName = matchName;
        }

        public Integer getMatchOrder() {
            return matchOrder;
        }

        public void setMatchOrder(Integer matchOrder) {
            this.matchOrder = matchOrder;
        }
    }

    // Getters and setters
    public List<Pairing> getPairings() {
        return pairings;
    }

    public void setPairings(List<Pairing> pairings) {
        this.pairings = pairings;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
}
