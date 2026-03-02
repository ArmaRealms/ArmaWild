package biz.donvi.jakesRTP.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired after a player is successfully teleported to a random location via JakesRTP.
 * <p>
 * This event is always called on the main server thread.
 */
public class PlayerRtpEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final double cost;
    private final String profileName;

    /**
     * @param player      The player who was teleported.
     * @param location    The location the player was teleported to.
     * @param cost        The amount charged to the player for the teleport (0 if free).
     * @param profileName The name of the RTP profile used.
     */
    public PlayerRtpEvent(final Player player, final Location location, final double cost, final String profileName) {
        super(player);
        this.location = location;
        this.cost = cost;
        this.profileName = profileName;
    }

    /**
     * @return A copy of the location the player was teleported to.
     */
    public Location getLocation() {
        return location.clone();
    }

    /**
     * @return The amount charged to the player for this teleport (0 if free or economy disabled).
     */
    public double getCost() {
        return cost;
    }

    /**
     * @return The name of the RTP profile that was used.
     */
    public String getProfileName() {
        return profileName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
