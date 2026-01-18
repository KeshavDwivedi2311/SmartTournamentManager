package com.sports.SmartSport.tournament.service;

import com.sports.SmartSport.tournament.Repository.MatchRepository;
import com.sports.SmartSport.tournament.Repository.PoolRepository;
import com.sports.SmartSport.tournament.DTO.TournamentScheduleStatusDTO;
import com.sports.SmartSport.tournament.entity.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to calculate tournament schedule status
 */
@Service
@Transactional
public class TournamentScheduleService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private TournamentConfigService configService;

    /**
     * Get tournament schedule status
     */
    public TournamentScheduleStatusDTO getScheduleStatus(Long tournamentId) {
        TournamentConfig config = configService.getConfig(tournamentId);
        
        // Get all matches (pool and knockout) for the tournament
        // This query gets matches from all pools in the tournament, including all match types
        List<Pool> pools = poolRepository.findByTournamentId(tournamentId);
        List<Long> poolIds = pools.stream().map(Pool::getId).collect(Collectors.toList());
        
        // Get ALL matches (league + knockout) by getting matches from all pools
        List<Match> allMatches = new ArrayList<>();
        for (Long poolId : poolIds) {
            List<Match> poolMatches = matchRepository.findByPoolId(poolId);
            allMatches.addAll(poolMatches);
        }
        
        // Count matches by status
        long totalMatches = allMatches.size();
        long completedMatches = allMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .count();
        long ongoingMatches = allMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.ONGOING)
                .count();
        long remainingMatches = totalMatches - completedMatches;
        
        // Calculate estimated vs actual time
        LocalDateTime tournamentStart = config.getTournamentStartTime();
        LocalDateTime now = LocalDateTime.now();
        
        // Check if tournament has started
        boolean tournamentStarted = tournamentStart != null && !now.isBefore(tournamentStart);
        
        long actualElapsedMinutes = 0;
        long timeDifferenceMinutes = 0;
        boolean isAhead = false;
        
        if (tournamentStarted) {
            Duration elapsed = Duration.between(tournamentStart, now);
            actualElapsedMinutes = elapsed.toMinutes();
            
            // Calculate how many matches should have been completed by now
            // Based on court schedule and elapsed time
            long expectedCompletedMatches = calculateExpectedCompletedMatches(config, allMatches, actualElapsedMinutes);
            
            // Matches difference: positive = behind (completed fewer than expected), negative = ahead (completed more than expected)
            long matchesDiff = expectedCompletedMatches - completedMatches;
            timeDifferenceMinutes = (int) Math.abs(matchesDiff);
            isAhead = matchesDiff < 0; // Negative means ahead (completed more than expected)
        }
        
        long timeDifferenceAbs = Math.abs(timeDifferenceMinutes);
        
        // Calculate estimated time remaining
        long estimatedRemainingMinutes = 0;
        if (remainingMatches > 0) {
            // Average time per match based on completed matches
            long avgMatchDuration = calculateAverageMatchDuration(completedMatches, allMatches);
            if (avgMatchDuration == 0) {
                avgMatchDuration = config.getEstimatedMatchDurationMinutes() != null 
                    ? config.getEstimatedMatchDurationMinutes() 
                    : 15;
            }
            estimatedRemainingMinutes = remainingMatches * avgMatchDuration;
        }
        
        // Calculate expected completed matches based on elapsed time
        int expectedCompletedMatches = 0;
        if (tournamentStarted && actualElapsedMinutes > 0) {
            long expectedMatches = calculateExpectedCompletedMatches(config, allMatches, actualElapsedMinutes);
            expectedCompletedMatches = (int) expectedMatches;
        }
        
        // Calculate court booking end time (from config or derive from schedule)
        LocalDateTime courtBookingEnd = config.getCourtBookingEndTime();
        if (courtBookingEnd == null && tournamentStart != null) {
            // Calculate from court schedule - last slot + 1 hour
            courtBookingEnd = calculateCourtBookingEndTime(config, tournamentStart);
        }
        
        // Calculate court time remaining
        long courtTimeRemainingMinutes = 0;
        if (courtBookingEnd != null && now.isBefore(courtBookingEnd)) {
            Duration remaining = Duration.between(now, courtBookingEnd);
            courtTimeRemainingMinutes = remaining.toMinutes();
        }
        
        TournamentScheduleStatusDTO status = new TournamentScheduleStatusDTO();
        status.setTotalMatches((int) totalMatches);
        status.setCompletedMatches((int) completedMatches);
        status.setOngoingMatches((int) ongoingMatches);
        status.setRemainingMatches((int) remainingMatches);
        status.setAhead(isAhead);
        status.setTimeDifferenceMinutes((int) timeDifferenceAbs);
        status.setEstimatedRemainingMinutes((int) estimatedRemainingMinutes);
        status.setCompletionPercentage(totalMatches > 0 ? (int)(completedMatches * 100 / totalMatches) : 0);
        status.setTournamentStarted(tournamentStarted);
        status.setTournamentStartTime(tournamentStart);
        status.setCourtBookingEndTime(courtBookingEnd);
        status.setCourtTimeRemainingMinutes(courtTimeRemainingMinutes);
        status.setExpectedCompletedMatches(expectedCompletedMatches);
        
        return status;
    }
    
    /**
     * Calculate expected number of completed matches based on court schedule and elapsed time
     * Properly parses the court schedule JSON and calculates based on actual court availability per time slot
     */
    private long calculateExpectedCompletedMatches(TournamentConfig config, List<Match> allMatches, long elapsedMinutes) {
        if (config.getTournamentStartTime() == null) {
            return 0;
        }
        
        int matchDuration = config.getEstimatedMatchDurationMinutes() != null 
            ? config.getEstimatedMatchDurationMinutes() 
            : 15;
        int breakTime = config.getBreakTimeMinutes() != null 
            ? config.getBreakTimeMinutes() 
            : 2;
        
        String scheduleJson = config.getCourtSchedule();
        if (scheduleJson == null || scheduleJson.isEmpty() || "[]".equals(scheduleJson)) {
            // Default: assume 4 courts available
            int avgCourts = 4;
            long matchesPerCourt = elapsedMinutes / (matchDuration + breakTime);
            return Math.min(matchesPerCourt * avgCourts, allMatches.size());
        }
        
        // Parse court schedule JSON
        // Format: [{"startTime":"09:00","courts":4},{"startTime":"10:00","courts":4},...]
        List<CourtScheduleSlot> slots = parseCourtSchedule(scheduleJson);
        if (slots.isEmpty()) {
            // Fallback to default
            int avgCourts = 4;
            long matchesPerCourt = elapsedMinutes / (matchDuration + breakTime);
            return Math.min(matchesPerCourt * avgCourts, allMatches.size());
        }
        
        LocalDateTime tournamentStart = config.getTournamentStartTime();
        LocalDateTime now = LocalDateTime.now();
        long totalExpectedMatches = 0;
        
        // Calculate matches for each time slot that has elapsed
        for (int i = 0; i < slots.size(); i++) {
            CourtScheduleSlot slot = slots.get(i);
            
            // Convert slot start time to LocalDateTime
            String[] timeParts = slot.getStartTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
            LocalDateTime slotStartTime = tournamentStart.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            
            // If this slot hasn't started yet, stop calculating
            if (slotStartTime.isAfter(now)) {
                break;
            }
            
            // Calculate slot duration
            long slotDurationMinutes = 60; // Default: 1 hour per slot
            if (i < slots.size() - 1) {
                // Calculate duration until next slot
                CourtScheduleSlot nextSlot = slots.get(i + 1);
                String[] nextTimeParts = nextSlot.getStartTime().split(":");
                int nextHour = Integer.parseInt(nextTimeParts[0]);
                int nextMinute = nextTimeParts.length > 1 ? Integer.parseInt(nextTimeParts[1]) : 0;
                LocalDateTime nextSlotStartTime = tournamentStart.withHour(nextHour).withMinute(nextMinute).withSecond(0).withNano(0);
                slotDurationMinutes = Duration.between(slotStartTime, nextSlotStartTime).toMinutes();
            }
            
            // Calculate how much of this slot has elapsed
            LocalDateTime slotEndTime = slotStartTime.plusMinutes(slotDurationMinutes);
            LocalDateTime effectiveEndTime = now.isBefore(slotEndTime) ? now : slotEndTime;
            long elapsedInSlot = Duration.between(slotStartTime, effectiveEndTime).toMinutes();
            if (elapsedInSlot <= 0) continue;
            
            // Calculate matches in this slot: (elapsed time / (match duration + break)) * number of courts
            long matchesInSlot = (elapsedInSlot / (matchDuration + breakTime)) * slot.getCourts();
            totalExpectedMatches += matchesInSlot;
        }
        
        return Math.min(totalExpectedMatches, allMatches.size());
    }
    
    /**
     * Parse court schedule JSON into list of slots
     */
    private List<CourtScheduleSlot> parseCourtSchedule(String scheduleJson) {
        List<CourtScheduleSlot> slots = new ArrayList<>();
        if (scheduleJson == null || scheduleJson.isEmpty() || "[]".equals(scheduleJson)) {
            return slots;
        }
        
        try {
            // Simple JSON parsing
            // Format: [{"startTime":"09:00","courts":4},...]
            String cleaned = scheduleJson.trim();
            if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) {
                return slots;
            }
            
            // Remove brackets
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            if (cleaned.isEmpty()) {
                return slots;
            }
            
            // Split by },{ to get individual slots
            String[] slotStrings = cleaned.split("\\},\\s*\\{");
            
            for (String slotStr : slotStrings) {
                // Clean up braces
                slotStr = slotStr.replace("{", "").replace("}", "").trim();
                
                String startTime = null;
                int courts = 0;
                
                // Parse key-value pairs
                String[] pairs = slotStr.split(",");
                for (String pair : pairs) {
                    pair = pair.trim();
                    if (pair.contains("startTime")) {
                        int colonIndex = pair.indexOf(":");
                        if (colonIndex > 0) {
                            String value = pair.substring(colonIndex + 1).trim();
                            value = value.replace("\"", "").replace("'", "");
                            startTime = value;
                        }
                    } else if (pair.contains("courts")) {
                        int colonIndex = pair.indexOf(":");
                        if (colonIndex > 0) {
                            String value = pair.substring(colonIndex + 1).trim();
                            courts = Integer.parseInt(value);
                        }
                    }
                }
                
                if (startTime != null && courts > 0) {
                    slots.add(new CourtScheduleSlot(startTime, courts));
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
            System.err.println("Error parsing court schedule: " + e.getMessage());
        }
        
        return slots;
    }
    
    /**
     * Helper class for court schedule slots
     */
    private static class CourtScheduleSlot {
        private String startTime;
        private int courts;
        
        public CourtScheduleSlot(String startTime, int courts) {
            this.startTime = startTime;
            this.courts = courts;
        }
        
        public String getStartTime() { return startTime; }
        public int getCourts() { return courts; }
    }
    
    /**
     * Calculate average match duration from completed matches
     */
    private long calculateAverageMatchDuration(long completedCount, List<Match> allMatches) {
        if (completedCount == 0) return 0;
        
        List<Match> completed = allMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .filter(m -> m.getStartTime() != null && m.getEndTime() != null)
                .collect(Collectors.toList());
        
        if (completed.isEmpty()) return 0;
        
        long totalSeconds = completed.stream()
                .mapToLong(m -> {
                    Duration d = Duration.between(m.getStartTime(), m.getEndTime());
                    return d.getSeconds();
                })
                .sum();
        
        return totalSeconds / completed.size() / 60; // Convert to minutes
    }
    
    /**
     * Calculate court booking end time from court schedule
     * Uses the last slot's start time + 1 hour as the end time
     */
    private LocalDateTime calculateCourtBookingEndTime(TournamentConfig config, LocalDateTime tournamentStart) {
        String scheduleJson = config.getCourtSchedule();
        if (scheduleJson == null || scheduleJson.isEmpty() || "[]".equals(scheduleJson)) {
            // Default: 8 hours from start
            return tournamentStart.plusHours(8);
        }
        
        try {
            // Simple JSON parsing for court schedule
            // Format: [{"startTime":"09:00","courts":4},...]
            String cleaned = scheduleJson.trim();
            if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) {
                return tournamentStart.plusHours(8);
            }
            
            // Find the last startTime in the schedule
            String lastTime = null;
            int lastTimeIndex = cleaned.lastIndexOf("\"startTime\"");
            if (lastTimeIndex > 0) {
                int colonIndex = cleaned.indexOf(":", lastTimeIndex);
                int quoteIndex = cleaned.indexOf("\"", colonIndex + 1);
                int endQuoteIndex = cleaned.indexOf("\"", quoteIndex + 1);
                if (quoteIndex > 0 && endQuoteIndex > quoteIndex) {
                    lastTime = cleaned.substring(quoteIndex + 1, endQuoteIndex);
                }
            }
            
            if (lastTime != null) {
                // Parse time (HH:mm format)
                String[] parts = lastTime.split(":");
                if (parts.length == 2) {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    
                    // Set end time to last slot + 1 hour
                    LocalDateTime lastSlotTime = tournamentStart.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                    return lastSlotTime.plusHours(1);
                }
            }
        } catch (Exception e) {
            // If parsing fails, default to 8 hours
        }
        
        // Default: 8 hours from start
        return tournamentStart.plusHours(8);
    }
}
