
package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.team.entity.Team;
import com.sports.SmartSport.team.repository.TeamRepository;
import com.sports.SmartSport.tournament.DTO.MatchDTO;
import com.sports.SmartSport.tournament.DTO.MatchUpdateRequest;
import com.sports.SmartSport.tournament.DTO.BulkMatchUpdateRequest;
import com.sports.SmartSport.tournament.Repository.MatchRepository;
import com.sports.SmartSport.tournament.Repository.PoolRepository;
import com.sports.SmartSport.tournament.entity.Match;
import com.sports.SmartSport.tournament.entity.MatchStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchService {
    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private TeamRepository teamRepository;

    public List<MatchDTO> getMatchesByStatus(Long poolId, MatchStatus status) {
        List<Match> matches = matchRepository.findByPoolIdAndStatusOrderByMatchOrder(poolId, status);
        return matches.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MatchDTO> getUpcomingMatches(Long poolId) {
        List<MatchStatus> upcomingStatuses = List.of(MatchStatus.SCHEDULED, MatchStatus.READY);
        List<Match> matches = matchRepository.findByPoolIdAndStatusInOrderByMatchOrder(poolId, upcomingStatuses);
        return matches.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MatchDTO> getOngoingMatches(Long poolId) {
        return getMatchesByStatus(poolId, MatchStatus.ONGOING);
    }

    public List<MatchDTO> getCompletedMatches(Long poolId) {
        return getMatchesByStatus(poolId, MatchStatus.COMPLETED);
    }

    public List<MatchDTO> getNextMatchesToPlay(Long poolId, int limit) {
        // Only return matches that are explicitly marked as NEXT status
        List<Match> matches = matchRepository.findByPoolIdAndStatusOrderByMatchOrder(poolId, MatchStatus.NEXT);
        
        // Apply limit if needed (though we expect max 2 anyway)
        if (limit > 0 && matches.size() > limit) {
            matches = matches.subList(0, limit);
        }
        
        return matches.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public MatchDTO markMatchAsReady(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new RuntimeException("Only scheduled matches can be marked as ready");
        }

        match.markAsReady();
        match = matchRepository.save(match);
        return convertToDTO(match);
    }

    public MatchDTO startMatch(Long matchId, String courtNumber) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Allow starting READY, SCHEDULED, or NEXT matches
        if (match.getStatus() != MatchStatus.READY && 
            match.getStatus() != MatchStatus.SCHEDULED && 
            match.getStatus() != MatchStatus.NEXT) {
            throw new RuntimeException("Only ready, scheduled, or next matches can be started. Current status: " + match.getStatus());
        }

        match.startMatch();
        if (courtNumber != null && !courtNumber.trim().isEmpty()) {
            match.setCourtNumber(courtNumber.trim());
        }

        match = matchRepository.save(match);
        
        // Log for debugging
        System.out.println("Match " + matchId + " started. Status changed from " + 
            match.getStatus() + " to ONGOING on court " + courtNumber);
        
        return convertToDTO(match);
    }

    public MatchDTO completeMatch(Long matchId, MatchUpdateRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != MatchStatus.ONGOING) {
            throw new RuntimeException("Only ongoing matches can be completed");
        }

        // Validate scores
        if (request.getTeam1Score() == null || request.getTeam2Score() == null) {
            throw new RuntimeException("Both team scores are required");
        }

        if (request.getTeam1Score() < 0 || request.getTeam2Score() < 0) {
            throw new RuntimeException("Scores cannot be negative");
        }

        // Determine winner
        Team winner = null;
        if (!request.getTeam1Score().equals(request.getTeam2Score())) {
            if (request.getWinnerId() != null) {
                winner = teamRepository.findById(request.getWinnerId())
                        .orElseThrow(() -> new RuntimeException("Winner team not found"));

                // Validate winner is one of the playing teams
                if (!winner.getId().equals(match.getTeam1().getId()) &&
                        !winner.getId().equals(match.getTeam2().getId())) {
                    throw new RuntimeException("Winner must be one of the playing teams");
                }
            } else {
                // Auto-determine winner based on score
                winner = request.getTeam1Score() > request.getTeam2Score() ?
                        match.getTeam1() : match.getTeam2();
            }
        }

        match.completeMatch(request.getTeam1Score(), request.getTeam2Score(), winner);

        if (request.getNotes() != null) {
            match.setNotes(request.getNotes());
        }

        match = matchRepository.save(match);
        return convertToDTO(match);
    }

    public List<MatchDTO> completeMatchesBulk(List<BulkMatchUpdateRequest> requests) {
        List<MatchDTO> completedMatches = new ArrayList<>();
        
        for (BulkMatchUpdateRequest request : requests) {
            try {
                MatchUpdateRequest updateRequest = new MatchUpdateRequest();
                updateRequest.setTeam1Score(request.getTeam1Score());
                updateRequest.setTeam2Score(request.getTeam2Score());
                updateRequest.setNotes(request.getNotes());
                updateRequest.setCourtNumber(request.getCourtNumber());
                
                MatchDTO completedMatch = completeMatch(request.getMatchId(), updateRequest);
                completedMatches.add(completedMatch);
            } catch (Exception e) {
                // Log error but continue with other matches
                System.err.println("Failed to complete match " + request.getMatchId() + ": " + e.getMessage());
            }
        }
        
        return completedMatches;
    }

    public List<MatchDTO> completeAllOngoingMatches(Long poolId, List<MatchUpdateRequest> matchUpdates) {
        List<MatchDTO> ongoingMatches = getOngoingMatches(poolId);
        List<MatchDTO> completedMatches = new ArrayList<>();
        
        // Create a map for quick lookup of match updates by match order or ID
        Map<Integer, MatchUpdateRequest> updateMap = new HashMap<>();
        for (int i = 0; i < matchUpdates.size(); i++) {
            updateMap.put(i, matchUpdates.get(i));
        }
        
        for (int i = 0; i < ongoingMatches.size() && i < matchUpdates.size(); i++) {
            try {
                MatchDTO match = ongoingMatches.get(i);
                MatchUpdateRequest updateRequest = matchUpdates.get(i);
                
                MatchDTO completedMatch = completeMatch(match.getId(), updateRequest);
                completedMatches.add(completedMatch);
            } catch (Exception e) {
                System.err.println("Failed to complete match: " + e.getMessage());
            }
        }
        
        return completedMatches;
    }

    public MatchDTO updateMatchStatus(Long matchId, MatchStatus newStatus) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
    
        MatchStatus currentStatus = match.getStatus();
        
        // Add validation for status transitions
        validateStatusTransition(currentStatus, newStatus);
        
        match.setStatus(newStatus);
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO updateMatch(Long matchId, MatchUpdateRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (request.getStatus() != null) {
            validateStatusTransition(match.getStatus(), request.getStatus());
            match.setStatus(request.getStatus());
        }

        if (request.getCourtNumber() != null) {
            match.setCourtNumber(request.getCourtNumber());
        }

        if (request.getNotes() != null) {
            match.setNotes(request.getNotes());
        }

        // Handle score updates for ongoing/completed matches
        if (match.getStatus() == MatchStatus.ONGOING || match.getStatus() == MatchStatus.COMPLETED) {
            if (request.getTeam1Score() != null) {
                match.setTeam1Score(request.getTeam1Score());
            }
            if (request.getTeam2Score() != null) {
                match.setTeam2Score(request.getTeam2Score());
            }
        }

        match = matchRepository.save(match);
        return convertToDTO(match);
    }

    private void validateStatusTransition(MatchStatus currentStatus, MatchStatus newStatus) {
        // Define valid transitions
        switch (currentStatus) {
            case SCHEDULED:
                if (newStatus != MatchStatus.READY && newStatus != MatchStatus.NEXT) {
                    throw new RuntimeException("Scheduled matches can only be moved to READY or NEXT status");
                }
                break;
            case READY:
                if (newStatus != MatchStatus.ONGOING && newStatus != MatchStatus.SCHEDULED) {
                    throw new RuntimeException("Ready matches can only be moved to ONGOING or back to SCHEDULED");
                }
                break;
            case NEXT:
                if (newStatus != MatchStatus.ONGOING && newStatus != MatchStatus.SCHEDULED) {
                    throw new RuntimeException("Next matches can only be moved to ONGOING or back to SCHEDULED");
                }
                break;
            case ONGOING:
                if (newStatus != MatchStatus.COMPLETED && newStatus != MatchStatus.NEXT && newStatus != MatchStatus.SCHEDULED) {
                    throw new RuntimeException("Ongoing matches can only be moved to COMPLETED, NEXT, or SCHEDULED");
                }
                break;
            case COMPLETED:
                if (newStatus != MatchStatus.ONGOING && newStatus != MatchStatus.SCHEDULED) {
                    throw new RuntimeException("Completed matches can only be reverted to ONGOING or SCHEDULED");
                }
                break;
            default:
                throw new RuntimeException("Invalid current status: " + currentStatus);
        }
    }

    public MatchDTO unmarkAsNext(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStatus().equals(MatchStatus.NEXT)) {
            throw new RuntimeException("Only next matches can be unmarked");
        }

        match.setStatus(MatchStatus.SCHEDULED);
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO moveToScheduled(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus().equals(MatchStatus.ONGOING)) {
            // Clear court number when moving back to scheduled
            match.setCourtNumber(null);
        }

        match.setStatus(MatchStatus.SCHEDULED);
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO moveToNext(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStatus().equals(MatchStatus.ONGOING)) {
            throw new RuntimeException("Only ongoing matches can be moved to next");
        }

        // Check current NEXT matches count - SAME VALIDATION AS markAsNext
        long nextMatchesCount = matchRepository.countByPoolIdAndStatus(match.getPool().getId(), MatchStatus.NEXT);
        if (nextMatchesCount >= 2) {
            // Get the names of current NEXT matches for better error message
            List<Match> currentNextMatches = matchRepository.findByPoolIdAndStatusOrderByMatchOrder(
                match.getPool().getId(), MatchStatus.NEXT);
        
            String nextMatchesInfo = currentNextMatches.stream()
                .map(m -> m.getTeam1().getName() + " vs " + m.getTeam2().getName())
                .collect(Collectors.joining(", "));
            
            throw new RuntimeException("Cannot move to next: Maximum of 2 matches can be in 'next' status at once. " +
                "Current next matches: " + nextMatchesInfo + ". " +
                "Please start some 'next' matches first or move them back to scheduled.");
        }

        // Clear court number when moving to next
        match.setCourtNumber(null);
        match.setStatus(MatchStatus.NEXT);
        Match savedMatch = matchRepository.save(match);
        
        // Log for debugging
        System.out.println("Match " + matchId + " moved from ONGOING to NEXT. Total NEXT matches in pool " + 
            match.getPool().getId() + ": " + (nextMatchesCount + 1));
        
        return convertToDTO(savedMatch);
    }

    public MatchDTO revertToOngoing(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStatus().equals(MatchStatus.COMPLETED)) {
            throw new RuntimeException("Only completed matches can be reverted to ongoing");
        }

        match.setStatus(MatchStatus.ONGOING);
        // Note: You might want to clear scores or handle this differently
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO revertToScheduled(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStatus().equals(MatchStatus.COMPLETED)) {
            throw new RuntimeException("Only completed matches can be reverted to scheduled");
        }

        match.setStatus(MatchStatus.SCHEDULED);
        match.setCourtNumber(null);
        // Clear scores when reverting to scheduled
        match.setTeam1Score(0);
        match.setTeam2Score(0);
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO revertFromCompleted(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getStatus().equals(MatchStatus.COMPLETED)) {
            throw new RuntimeException("Only completed matches can be reverted");
        }

        // Default revert to ongoing (you can modify this logic as needed)
        match.setStatus(MatchStatus.ONGOING);
        Match savedMatch = matchRepository.save(match);
        return convertToDTO(savedMatch);
    }

    public MatchDTO markAsNext(Long matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found with ID: " + matchId));
        
        if (!match.getStatus().equals(MatchStatus.SCHEDULED)) {
            throw new RuntimeException("Only scheduled matches can be marked as next. Current status: " + match.getStatus());
        }
        
        // Check current NEXT matches count
        long nextMatchesCount = matchRepository.countByPoolIdAndStatus(match.getPool().getId(), MatchStatus.NEXT);
        if (nextMatchesCount >= 2) {
            // Get the names of current NEXT matches for better error message
            List<Match> currentNextMatches = matchRepository.findByPoolIdAndStatusOrderByMatchOrder(
                match.getPool().getId(), MatchStatus.NEXT);
            
            String nextMatchesInfo = currentNextMatches.stream()
                .map(m -> m.getTeam1().getName() + " vs " + m.getTeam2().getName())
                .collect(Collectors.joining(", "));
                
            throw new RuntimeException("Maximum of 2 matches can be marked as 'next' at once. " +
                "Current next matches: " + nextMatchesInfo + ". " +
                "Please start some matches first or unmark other 'next' matches.");
        }
        
        match.setStatus(MatchStatus.NEXT);
        Match savedMatch = matchRepository.save(match);
        
        // Log for debugging
        System.out.println("Match " + matchId + " marked as NEXT. Total NEXT matches in pool " + 
            match.getPool().getId() + ": " + (nextMatchesCount + 1));
        
        return convertToDTO(savedMatch);
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

        if (match.getWinner() != null) {
            dto.setWinnerId(match.getWinner().getId());
            dto.setWinnerName(match.getWinner().getName());
        }

        return dto;
    }
}
