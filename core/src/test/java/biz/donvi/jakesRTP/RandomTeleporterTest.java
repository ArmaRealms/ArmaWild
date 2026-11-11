package biz.donvi.jakesRTP;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RandomTeleporterTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void eventProfilesAreResolvedOnlyWhenEnabled(final boolean enabled) throws Exception {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("rtp-on-first-join.enabled", enabled);
        config.set("rtp-on-death.enabled", enabled);
        config.set("rtp-on-first-join.settings", enabled ? "mundo" : "default-settings");
        config.set("rtp-on-death.settings", enabled ? "mundo" : "default-settings");

        final YamlConfiguration profileConfig = new YamlConfiguration();
        profileConfig.set("enabled", true);
        profileConfig.set("landing-world", "world");
        profileConfig.set("distribution", "world-border");
        final World world = mock(World.class);
        final Server server = mock(Server.class);
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        when(world.getName()).thenReturn("world");
        when(server.getWorld("world")).thenReturn(world);
        when(plugin.getServer()).thenReturn(server);
        final WorldBorderPluginHook hook = mock(WorldBorderPluginHook.class);
        when(hook.generateDistributions()).thenReturn(Map.of("world-border_world",
                new DistributionSettings(new DistributionShape.Rectangle(100, 100), 0, 0)));

        final JakesRtpPlugin oldPlugin = JakesRtpPlugin.plugin;
        final WorldBorderPluginHook oldHook = JakesRtpPlugin.worldBorderPluginHook;
        try (var logging = mockStatic(JakesRtpPlugin.class)) {
            JakesRtpPlugin.plugin = plugin;
            JakesRtpPlugin.worldBorderPluginHook = hook;
            Messages.setMap(new Yaml().load(getClass().getResourceAsStream("/translations/lang_en.yml")));
            final RandomTeleporter teleporter = new RandomTeleporter(
                    config, List.of(new GeneralUtil.Pair<>("mundo", profileConfig)), List.of());

            assertEquals(enabled, teleporter.firstJoinRtp);
            assertEquals(enabled, teleporter.onDeathRtp);
            final RtpProfile profile = teleporter.getRtpSettingsByName("mundo");
            if (enabled) {
                assertSame(profile, teleporter.firstJoinSettings);
                assertSame(profile, teleporter.onDeathSettings);
            } else {
                assertNull(teleporter.firstJoinSettings);
                assertNull(teleporter.onDeathSettings);
            }
        } finally {
            JakesRtpPlugin.plugin = oldPlugin;
            JakesRtpPlugin.worldBorderPluginHook = oldHook;
        }
    }
}
