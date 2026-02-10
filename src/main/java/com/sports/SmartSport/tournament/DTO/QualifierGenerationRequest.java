package com.sports.SmartSport.tournament.DTO;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for flexible qualifier match generation.
 * Supports per-pool qualifier counts, pool selection, and multiple pairing strategies.
 */
public class QualifierGenerationRequest {

    /**
     * Per-pool qualifier count: poolId -> number of teams to qualify from that pool.
     * Example: {1: 4, 2: 4} means 4 teams from pool 1 and 4 from pool 2.
     * If null, falls back to teamsPerPool (uniform for all pools).
     */
    private Map<Long, Integer> teamsPerPoolMap;

    /**
     * Uniform fallback: same number of teams from every pool.
     * Used when teamsPerPoolMap is null. Default: 2
     */
    private Integer teamsPerPool;

    /**
     * Which pools to include. If null/empty, all tournament pools are included.
     */
    private List<Long> selectedPoolIds;

    /**
     * Pairing strategy: CROSS_POOL, RANKING_BASED, RANDOM
     * Default: CROSS_POOL
     */
    private String pairingStrategy;

    /**
     * Whether to avoid same-pool matchups in qualifiers.
     * Default: true
     */
    private Boolean avoidSamePool;

    // Constructors
    public QualifierGenerationRequest() {}

    public QualifierGenerationRequest(Integer teamsPerPool) {
        this.teamsPerPool = teamsPerPool;
    }

    // Getters and Setters
    public Map<Long, Integer> getTeamsPerPoolMap() {
        return teamsPerPoolMap;
    }

    public void setTeamsPerPoolMap(Map<Long, Integer> teamsPerPoolMap) {
        this.teamsPerPoolMap = teamsPerPoolMap;
    }

    public Integer getTeamsPerPool() {
        return teamsPerPool != null ? teamsPerPool : 2;
    }

    public void setTeamsPerPool(Integer teamsPerPool) {
        this.teamsPerPool = teamsPerPool;
    }

    public List<Long> getSelectedPoolIds() {
        return selectedPoolIds;
    }

    public void setSelectedPoolIds(List<Long> selectedPoolIds) {
        this.selectedPoolIds = selectedPoolIds;
    }

    public String getPairingStrategy() {
        return pairingStrategy != null ? pairingStrategy : "CROSS_POOL";
    }

    public void setPairingStrategy(String pairingStrategy) {
        this.pairingStrategy = pairingStrategy;
    }

    public Boolean getAvoidSamePool() {
        return avoidSamePool != null ? avoidSamePool : true;
    }

    public void setAvoidSamePool(Boolean avoidSamePool) {
        this.avoidSamePool = avoidSamePool;
    }

    /**
     * Get the number of qualifying teams for a specific pool.
     * Checks teamsPerPoolMap first, falls back to teamsPerPool.
     */
    public int getTeamsForPool(Long poolId) {
        if (teamsPerPoolMap != null && teamsPerPoolMap.containsKey(poolId)) {
            return teamsPerPoolMap.get(poolId);
        }
        return getTeamsPerPool();
    }

    @Override
    public String toString() {
        return "QualifierGenerationRequest{" +
                "teamsPerPoolMap=" + teamsPerPoolMap +
                ", teamsPerPool=" + teamsPerPool +
                ", selectedPoolIds=" + selectedPoolIds +
                ", pairingStrategy='" + pairingStrategy + '\'' +
                ", avoidSamePool=" + avoidSamePool +
                '}';
    }
}
