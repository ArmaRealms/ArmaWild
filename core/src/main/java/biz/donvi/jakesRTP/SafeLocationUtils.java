package biz.donvi.jakesRTP;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;

public class SafeLocationUtils {

    public static final SafeLocationUtils util = new SafeLocationUtils();

    private SafeLocationUtils() {
    }

    static int chunkXZ(final double blockXZ) {
        return (int) Math.floor((double) blockXZ / 16);
    }

    /**
     * Checks if the current thread is the primary Bukkit thread.
     * If it is, nothing happens, if not, it throws an unchecked exception.
     */
    static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new AccessFromNonMainThreadError();
    }

    /**
     * Checks the given material against a <u>whitelist</u> of materials deemed to be "safe to be in"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    boolean isSafeToBeIn(final Material mat) {
        switch (mat) {
            case AIR:
            case SNOW:
            case FERN:
            case LARGE_FERN:
            case VINE:
            case GRASS_BLOCK:
            case TALL_GRASS:
            case GLOW_LICHEN:
            case MOSS_CARPET:
            case GLOW_BERRIES:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks the given material against a <u>blacklist</u> of materials deemed to be "safe to be on"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    boolean isSafeToBeOn(final Material mat) {
        switch (mat) {
            case LAVA:
            case MAGMA_BLOCK:
            case WATER:
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case CACTUS:
            case SEAGRASS:
            case KELP:
            case TALL_SEAGRASS:
            case LILY_PAD:
            case BAMBOO:
            case BAMBOO_SAPLING:
            case SMALL_DRIPLEAF:
            case BIG_DRIPLEAF:
            case BIG_DRIPLEAF_STEM:
            case POINTED_DRIPSTONE:
                return false;
            case GRASS_BLOCK:
            case STONE:
            case DIRT:
            default:
                return true;
        }
    }

    /**
     * Checks if the given material is any type of tree leaf.
     *
     * @param mat The material to check
     * @return Whether it is a type of leaf
     */
    boolean isTreeLeaves(final Material mat) {
        return Tag.LEAVES.isTagged(mat);
    }

    /**
     * Checks the given material against a <u>whitelist</u> of materials deemed to be "safe to go through".
     * Materials that are "safe to go through" are generally where that you can stand safely on, or be safely in,
     * though in general if you are finding a safe location, you would prefer not not end up on or in them. <p>
     * Note: Safe to go through is in the context of looking for a safe spot. These materials may not necessarily
     * allow players to walk through them.
     *
     * @param mat The material to check
     * @return Whether it is safe to go through
     */
    boolean isSafeToGoThrough(final Material mat) {
        //At the time of writing this, I can not think of any materials other than leaves that fit this category.
        //I am leaving the method in place though so if I decide to add more materials later, it will be easy.
        return isTreeLeaves(mat);
    }

    /**
     * Checks if the location is in a tree. To be in a tree, you must both be on a log, and in leaves.<p>
     * Note: This should only be run from the main thread.
     *
     * @param loc The location to check.
     * @return True if the location is in a tree.
     */
    boolean isInATree(final Location loc) {
        requireMainThread();
        for (final Material material : new Material[]{
                loc.clone().add(0, 1, 0).getBlock().getType(),
                loc.clone().add(0, 2, 0).getBlock().getType()})
            if (isTreeLeaves(material)) return true;
        return false;
    }

    /**
     * Checks if the location is in a tree. To be in a tree, you must both be on a log, and in leaves.<p>
     * Note: This can be run from any thread.
     *
     * @param loc   The location to check.
     * @param chunk The chunk snapshot that contains the {@code Location}'s data.
     * @return True if the location is in a tree.
     */
    boolean isInTree(final Location loc, final ChunkSnapshot chunk) {
        for (final Material material : new Material[]{
                locMatFromSnapshot(loc.clone().add(0, 1, 0), chunk),
                locMatFromSnapshot(loc.clone().add(0, 2, 0), chunk)})
            if (isTreeLeaves(material)) return true;
        return false;
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}<p>
     * Note: This should only be run from the main thread.
     *
     * @param loc The location to modify
     */
    void dropToGround(final Location loc) {
        requireMainThread();
        while (isSafeToBeIn(loc.getBlock().getType()) || isSafeToGoThrough(loc.getBlock().getType()))
            loc.add(0, -1, 0);
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}<p>
     * Note: This can be run from any thread.
     *
     * @param loc   The location to modify
     * @param chunk The chunk snapshot that contains the {@code Location}'s data.
     */
    void dropToGround(final Location loc, final ChunkSnapshot chunk) {
        while (isSafeToBeIn(locMatFromSnapshot(loc, chunk)) || isSafeToGoThrough(locMatFromSnapshot(loc, chunk)))
            loc.add(0, -1, 0);
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}<p>
     * Note: This should only be run from the main thread.
     *
     * @param loc      The location to modify
     * @param lowBound The lowest the location can go
     */
    void dropToGround(final Location loc, final int lowBound, final int highBound) {
        requireMainThread();
        // If our location was above the max height, drop us to it.
        if (loc.getY() > highBound) loc.setY(highBound);
        // If we start in a solid block, we need to wait until we get out of it
        while (loc.getBlockY() > lowBound && !(
                isSafeToBeIn(loc.getBlock().getType())
                        || isSafeToGoThrough(loc.getBlock().getType()))
        ) loc.add(0, -1, 0);
        // Now we are in something non-solid; we can start looking for the ground
        while (loc.getBlockY() > lowBound && (
                isSafeToBeIn(loc.getBlock().getType())
                        || isSafeToGoThrough(loc.getBlock().getType()))
        ) loc.add(0, -1, 0);
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}<p>
     * Note: This can be run from any thread.
     *
     * @param loc      The location to modify
     * @param lowBound The lowest the location can go
     * @param chunk    The chunk snapshot that contains the {@code Location}'s data.
     */
    void dropToGround(final Location loc, final int lowBound, final int highBound, final ChunkSnapshot chunk) {
        // If our location was above the max height, drop us to it.
        if (loc.getY() > highBound) loc.setY(highBound);
        // If we start in a solid block, we need to wait until we get out of it
        while (loc.getBlockY() > lowBound && !(
                isSafeToBeIn(locMatFromSnapshot(loc, chunk))
                        || isSafeToGoThrough(locMatFromSnapshot(loc, chunk)))
        ) loc.add(0, -1, 0);
        // Now we are in something non-solid; we can start looking for the ground
        while (loc.getBlockY() > lowBound && (
                isSafeToBeIn(locMatFromSnapshot(loc, chunk))
                        || isSafeToGoThrough(locMatFromSnapshot(loc, chunk)))
        ) loc.add(0, -1, 0);
    }

    /* ================================================== *\
                    Chunk cache utils
    \* ================================================== */

    void dropToMiddle(final Location loc, final int lowBound, final int highBound) {
        dropToMiddle(loc, lowBound, highBound, null);
    }

    void dropToMiddle(final Location loc, final int lowBound, final int highBound, final ChunkSnapshot chunk) {
        loc.setY((highBound + lowBound) / 2d);  //Set starting point
        Material mat = chunk == null            //Set starting material
                ? loc.getBlock().getType()          //  If we are on the bukkit thread, we should be called without a
                : locMatFromSnapshot(loc, chunk);   //  snapshot. If we are off it, we should have a snapshot.

        int change = 0;     //For movement control
        int direction = 1;  //For movement control
        boolean upWasSolid = false, //For escaping while
                downWasAir = false; //For escaping while

        //While [we are unsafe] check the next spot for partial safety
        while (
                (direction == -1
                        ? !(upWasSolid && isSafeToBeIn(mat))
                        : !(downWasAir && isSafeToBeOn(mat)))
                        && loc.getY() > 0
                        && loc.getY() < 128
        ) {
            mat = (chunk == null)
                    ? loc.getBlock().getType()
                    : locMatFromSnapshot(loc, chunk);
            //Preparing testing variables
            if (direction == 1)
                upWasSolid = isSafeToBeOn(mat);
            else
                downWasAir = isSafeToBeIn(mat) | isSafeToGoThrough(mat);
            //Moving location
            loc.add(0, change * direction, 0);
            if (direction == -1)
                change++;
            direction *= -1;
            loc.add(0, -change * direction, 0);
            mat = (chunk == null)
                    ? loc.getBlock().getType()
                    : locMatFromSnapshot(loc, chunk);
        }
    }

    Material locMatFromSnapshot(final Location loc, final ChunkSnapshot chunk) {
        if (!isLocationInsideChunk(loc, chunk))
            throw new RuntimeException("The given location is not within given chunk!");
        int x = loc.getBlockX() % 16;
        int z = loc.getBlockZ() % 16;
        if (x < 0) x += 16;
        if (z < 0) z += 16;
        return chunkLocMatFromSnapshot(x, loc.getBlockY(), z, chunk);
    }

    Material chunkLocMatFromSnapshot(final int inX, final int y, final int inZ, final ChunkSnapshot chunk) {
        return chunk.getBlockData(inX, y, inZ).getMaterial();
    }

    /* ================================================== *\
                Misc (but still related) utils
    \* ================================================== */

    boolean isLocationInsideChunk(final Location loc, final ChunkSnapshot chunk) {
        return (int) Math.floor((double) loc.getBlockX() / 16) == chunk.getX() &&
                (int) Math.floor((double) loc.getBlockZ() / 16) == chunk.getZ();
    }

    /**
     * Exists purely to throw an exception before an attempt is made
     * to access the Bukkit API from a thread other than the main
     */
    private static class AccessFromNonMainThreadError extends RuntimeException {
    }
}
