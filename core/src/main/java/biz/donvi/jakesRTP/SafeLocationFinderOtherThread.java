package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.exception.JrtpBaseException;
import biz.donvi.jakesRTP.exception.PluginDisabledException;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;
import static biz.donvi.jakesRTP.SafeLocationUtils.chunkXZ;


public class SafeLocationFinderOtherThread extends SafeLocationFinder {

    private final Map<String, ChunkSnapshot> chunkSnapshotMap = new HashMap<>();
    private final int timeout;

    /**
     * Just constructs the {@code SafeLocationFinder}, use {@code checkSafety} to check if
     * the current location is safe, and use {@code nextInSpiral} to move on to the next location.
     *
     * @param loc The location that will be checked for safety, and potentially modified.
     */
    public SafeLocationFinderOtherThread(final Location loc) {
        super(loc);
        timeout = 5;
    }

    /**
     * This constructs a fully managed {@code SafeLocationFinder}. Because the bounds for the check operations are
     * supplied, this object can check the safety of the location itself by calling instance method
     * {@code tryAndMakeSafe()}. The given location <b>will</b> be modified.
     *
     * @param loc             The location to try and make safe. This <b>will</b> be modified.
     * @param checkRadiusXZ   The distance out from the center that the location cam move.
     * @param checkRadiusVert The distance up and down that the location can move.
     * @param lowBound        The lowest Y value the location can have.
     * @param timeout         The max number of seconds to wait for data from another thread
     */
    public SafeLocationFinderOtherThread(
            final Location loc, final int checkRadiusXZ, final int checkRadiusVert,
            final int lowBound, final int highBound, final int timeout
    ) {
        super(loc, checkRadiusXZ, checkRadiusVert, lowBound, highBound);
        this.timeout = timeout;
    }

    /**
     * Gets the material of the location as if by {@code loc.getBlock().getType()}.<p>
     * Since this is the overridden version, we can not get the material the easy way.
     * Instead, we need to get and use a {@code ChunkSnapshot}.
     *
     * @param loc The location to get the material for.
     */
    @Override
    protected Material getLocMaterial(final Location loc) throws PluginDisabledException, TimeoutException {
        return SafeLocationUtils.util.locMatFromSnapshot(loc, getChunkForLocation(loc));
    }

    @Override
    protected void dropToGround() throws PluginDisabledException, TimeoutException {
        SafeLocationUtils.util.dropToGround(loc, lowBound, highBound, getChunkForLocation(loc));
    }

    @Override
    protected void dropToMiddle() throws PluginDisabledException, TimeoutException {
        SafeLocationUtils.util.dropToMiddle(loc, lowBound, highBound, getChunkForLocation(loc));
    }

    private ChunkSnapshot getChunkForLocation(final Location loc)
            throws PluginDisabledException, TimeoutException {

        final String chunkKey = chunkXZ(loc.getX()) + " " + chunkXZ(loc.getZ());

        final ChunkSnapshot cached = chunkSnapshotMap.get(chunkKey);
        if (cached != null) {
            return cached;
        }

        if (!plugin.locCache()) {
            throw new PluginDisabledException();
        }

        final Location chunkAt = loc.clone();

        try {
            final ChunkSnapshot snapshot = chunkAt.getWorld()
                    .getChunkAtAsync(chunkAt)
                    .thenApply(Chunk::getChunkSnapshot)
                    .get(timeout, TimeUnit.SECONDS);

            if (!plugin.locCache()) {
                throw new PluginDisabledException();
            }

            chunkSnapshotMap.put(chunkKey, snapshot);

            return snapshot;

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PluginDisabledException();

        } catch (final ExecutionException e) {
            throw new IllegalStateException("Failed to load chunk snapshot.", e);

        } catch (final CancellationException e) {
            throw new PluginDisabledException();
        }
    }

}
