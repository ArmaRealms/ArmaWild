package biz.donvi.jakesRTP;

import java.util.HashMap;

public class CoolDownTracker {

    private static final long MS_TO_TICKS = 50L;
    private final HashMap<String, Long> tracker = new HashMap<>();
    private final long coolDownTime;

    /**
     * Creates a CoolDownTracker object, and sets the coolDownTime.
     *
     * @param coolDownTimeInSeconds The amount of time that must be waited to get True from a check.
     */
    CoolDownTracker(final double coolDownTimeInSeconds) {
        coolDownTime = (long) (coolDownTimeInSeconds * 1000);
    }

    /**
     * Returns the cooldown time in milliseconds.
     *
     * @return The cooldown time in milliseconds.
     */
    public long getCoolDownTime() {
        return coolDownTime;
    }

    /**
     * Returns the cooldown time in ticks.
     * This is used for Bukkit/Spigot/Paper plugins, where 1 tick is 50 milliseconds.
     *
     * @return The cooldown time in ticks.
     */
    public long getCoolDownTimeInTicks() {
        return coolDownTime / MS_TO_TICKS;
    }

    /**
     * Checks if between now and the last logged time is greater or less than the last logged time.
     * No changes will be made to the data regardless of the result.
     *
     * @param playerName Name of the player/user to check.
     * @return If the difference in time between now and the last logged time is greater than the cool down.
     */
    public boolean check(final String playerName) {
        final Long time = tracker.get(playerName);
        final long currentTime = System.currentTimeMillis();
        return time == null || currentTime - time > coolDownTime;
    }

    /**
     * Checks if the time difference between now and the last logged time is greater or less than
     * the last logged time. If true (or this is the first time) the current time will be logged.
     *
     * @param playerName Name of the player/user to check.
     * @return If the difference in time between now and the last logged time is greater than the cool down.
     */
    boolean checkAndLog(final String playerName) {
        final Long time = tracker.get(playerName);
        final long currentTime = System.currentTimeMillis();
        if (time == null || currentTime - time > coolDownTime) {
            tracker.put(playerName, currentTime);
            return true;
        }
        return false;
    }

    /**
     * Sets the logged time for the given player to the current time.
     *
     * @param playerName Player to log time for.
     */
    public void log(final String playerName) {
        log(playerName, System.currentTimeMillis());
    }

    /**
     * Sets the logged time for the given player to the given time.
     *
     * @param playerName Player to log time for.
     * @param time       The time to log.
     */
    public void log(final String playerName, final long time) {
        tracker.put(playerName, time);
    }

    /**
     * Returns the amount of time between now and the last successful log, or -1 if no time was ever logged.
     *
     * @param playerName The player to check.
     * @return The amount of time between now and the last logged time, or -1 if no time was ever logged.
     */
    long timeDifference(final String playerName) {
        return timeDifference(playerName, System.currentTimeMillis());
    }

    /**
     * Returns the amount of time between the given time and the last successful log, or -1 if no time was ever logged.
     *
     * @param playerName The player to check.
     * @return The amount of time between given time and the last logged time, or -1 if no time was ever logged.
     */
    long timeDifference(final String playerName, final long time) {
        final Long oldTime = tracker.get(playerName);
        if (oldTime == null) return -1;
        return time - oldTime;
    }

    /**
     * Returns the minimum amount of time to wait (in milliseconds) before being able to call a successful (true) check.
     * Negative values mean a check would return true.
     *
     * @param playerName The player to check.
     * @return Returns the minimum amount of time to wait before being able to call a successful check.
     */
    public long timeLeft(final String playerName) {
        return coolDownTime - timeDifference(playerName);
    }

    /**
     * Returns the amount of time left to wait before calling rtp again, formatted in a reasonable manner.
     *
     * @param playerName The player to check.
     * @return A string describing the time left until the cooldown is over. Human-readable.
     */
    public String timeLeftWords(final String playerName) {
        return GeneralUtil.readableTime(timeLeft(playerName));
    }

    /**
     * Returns the amount of time that a player must wait between rtp calls, formatted in a reasonable manner.
     *
     * @return A human-readable string of the cooldown time.
     */
    String coolDownTime() {
        return coolDownTime == 0 ? "None." : GeneralUtil.readableTime(coolDownTime);
    }
}
