
package com.sports.SmartSport.tournament.DTO;

public class QualifierSchedulingRules {
    private Boolean avoidSamePool = true;
    private MatchingStrategy matchingStrategy = MatchingStrategy.CROSS_POOL;
    private QualifierFormat qualifierFormat = QualifierFormat.SINGLE_ELIMINATION;

    public enum MatchingStrategy {
        CROSS_POOL,
        RANKING_BASED,
        RANDOM
    }

    public enum QualifierFormat {
        SINGLE_ELIMINATION,
        ROUND_ROBIN
    }

    // Constructors
    public QualifierSchedulingRules() {}

    public QualifierSchedulingRules(Boolean avoidSamePool, MatchingStrategy matchingStrategy, QualifierFormat qualifierFormat) {
        this.avoidSamePool = avoidSamePool;
        this.matchingStrategy = matchingStrategy;
        this.qualifierFormat = qualifierFormat;
    }

    // Getters and Setters
    public Boolean getAvoidSamePool() {
        return avoidSamePool;
    }

    public void setAvoidSamePool(Boolean avoidSamePool) {
        this.avoidSamePool = avoidSamePool;
    }

    public MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }

    public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }

    public QualifierFormat getQualifierFormat() {
        return qualifierFormat;
    }

    public void setQualifierFormat(QualifierFormat qualifierFormat) {
        this.qualifierFormat = qualifierFormat;
    }

    @Override
    public String toString() {
        return "QualifierSchedulingRules{" +
                "avoidSamePool=" + avoidSamePool +
                ", matchingStrategy=" + matchingStrategy +
                ", qualifierFormat=" + qualifierFormat +
                '}';
    }
}
