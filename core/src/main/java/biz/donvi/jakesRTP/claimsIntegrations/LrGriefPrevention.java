package biz.donvi.jakesRTP.claimsIntegrations;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class LrGriefPrevention implements LocationRestrictor {
    protected GriefPrevention cmPlugin;
    private Claim lastClaim = null;

    public LrGriefPrevention(final GriefPrevention cmPlugin) {
        this.cmPlugin = cmPlugin;
    }

    @Override
    public Plugin supporterPlugin() {
        return cmPlugin;
    }

    @Override
    public boolean denyLandingAtLocation(final Location location) {
        final Claim currentClaim = cmPlugin.dataStore.getClaimAt(location, true, lastClaim);
        if (currentClaim == null) {
            return false;
        } else {
            lastClaim = currentClaim;
            return true;
        }
    }
}
