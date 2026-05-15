package com.realciv.realciv.data;

public record LeadershipContestDecision(boolean changed, String message) {
    public static LeadershipContestDecision changed(String message) {
        return new LeadershipContestDecision(true, message);
    }

    public static LeadershipContestDecision denied(String message) {
        return new LeadershipContestDecision(false, message);
    }
}
