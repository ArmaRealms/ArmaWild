package biz.donvi.jakesRTP.claimsIntegrations;

import me.angeschossen.lands.api.integration.LandsIntegration;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class LrLands implements LocationRestrictor {
    protected final LandsIntegration landsIntegration;
    protected Plugin lands;

    public LrLands(final Plugin plugin, final Plugin rtpPlugin) {
        this.lands = plugin;
        landsIntegration = new LandsIntegration(rtpPlugin);
    }

    @Override
    public Plugin supporterPlugin() {
        return lands;
    }

    @Override
    public boolean denyLandingAtLocation(final Location location) {
        return landsIntegration.isClaimed(location);
    }
}
