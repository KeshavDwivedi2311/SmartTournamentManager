
package com.sports.SmartSport.tournament.DTO;

import lombok.Data;

@Data
public class BulkMatchUpdateRequest {
    private Long matchId;
    private Integer team1Score;
    private Integer team2Score;
    private String notes;
    private String courtNumber;
}
