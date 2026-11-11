package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.commands.BrigadierCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrigadierCommandsTest {
    @Test
    void forceRtpUsesTargetWorldByDefaultAndAcceptsExplicitDestinations() throws Exception {
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        final RandomTeleporter teleporter = mock(RandomTeleporter.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(teleporter.getRtpSettings()).thenReturn(new ArrayList<>());
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        final Commands registrar = mock(Commands.class, invocation -> {
            if ("register".equals(invocation.getMethod().getName())) {
                dispatcher.getRoot().addChild(invocation.getArgument(0));
                return Set.of();
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        BrigadierCommands.register(plugin, registrar);

        final CommandSourceStack source = mock(CommandSourceStack.class);
        final CommandSender sender = mock(CommandSender.class);
        final Server server = mock(Server.class);
        final Player target = mock(Player.class);
        final World targetWorld = mock(World.class);
        final World otherWorld = mock(World.class);
        final Location origin = new Location(targetWorld, 0, 64, 0);
        final Location otherSpawn = new Location(otherWorld, 0, 64, 0);
        final Location landing = new Location(otherWorld, 100, 64, 100);
        when(source.getSender()).thenReturn(sender);
        when(sender.getServer()).thenReturn(server);
        when(server.getPlayerExact("Steve")).thenReturn(target);
        when(server.getWorlds()).thenReturn(List.of(otherWorld));
        when(otherWorld.getName()).thenReturn("other");
        when(otherWorld.getSpawnLocation()).thenReturn(otherSpawn);
        when(target.getLocation()).thenReturn(origin);
        when(teleporter.getRtpSettingsByWorld(targetWorld)).thenReturn(RtpProfile.DEFAULT_SETTINGS);
        when(teleporter.getRtpSettingsByName("mundo")).thenReturn(RtpProfile.DEFAULT_SETTINGS);
        when(teleporter.getRtpSettingsByWorld(otherWorld)).thenReturn(RtpProfile.DEFAULT_SETTINGS);
        when(teleporter.getRtpLocation(RtpProfile.DEFAULT_SETTINGS, origin, true)).thenReturn(landing);
        when(teleporter.getRtpLocation(RtpProfile.DEFAULT_SETTINGS, otherSpawn, true)).thenReturn(landing);
        when(target.teleportAsync(landing)).thenReturn(new CompletableFuture<>());

        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("forcertp Steve", source));
        when(sender.hasPermission("jakesrtp.others")).thenReturn(true);
        dispatcher.execute("forcertp Steve", source);
        dispatcher.execute("forcertp Steve -c mundo", source);
        dispatcher.execute("forcertp Steve -w other", source);

        verify(teleporter).getRtpSettingsByWorld(targetWorld);
        verify(teleporter).getRtpSettingsByName("mundo");
        verify(teleporter).getRtpSettingsByWorld(otherWorld);
        verify(teleporter).getRtpLocation(RtpProfile.DEFAULT_SETTINGS, otherSpawn, true);
        verify(target, times(3)).teleportAsync(landing);
    }

    @Test
    void profileShortcutOverridesExistingCommandWithoutRemovingItsNamespace() throws Exception {
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        final RandomTeleporter teleporter = mock(RandomTeleporter.class);
        final Server server = mock(Server.class);
        final CommandMap commandMap = mock(CommandMap.class);
        final YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(plugin.getServer()).thenReturn(server);
        when(server.getCommandMap()).thenReturn(commandMap);
        final Command existing = mock(Command.class);
        final Command unrelated = mock(Command.class);
        final Map<String, Command> knownCommands = new HashMap<>(Map.of(
                "mundo", existing, "external:mundo", existing, "other", unrelated));
        when(commandMap.getKnownCommands()).thenReturn(knownCommands);
        when(teleporter.getRtpSettings()).thenReturn(new ArrayList<>(List.of(
                profile("end", true), profile("nether", true), profile("recursos", true),
                profile("mundo", true), profile("legado", true), profile("disabled", false)
        )));
        final List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        final Commands registrar = mock(Commands.class, invocation -> {
            if ("register".equals(invocation.getMethod().getName())) {
                nodes.add(invocation.getArgument(0));
                return Set.of();
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        when(registrar.getDispatcher()).thenReturn(new CommandDispatcher<>());

        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        BrigadierCommands.register(plugin, registrar);

        assertEquals(List.of("rtp", "forcertp", "rtp-admin", "end", "nether", "recursos", "mundo", "legado"),
                nodes.stream().map(LiteralCommandNode::getLiteral).toList());
        assertNull(knownCommands.get("mundo"));
        assertSame(existing, knownCommands.get("external:mundo"));
        assertSame(unrelated, knownCommands.get("other"));
        assertNotNull(nodes.get(0).getChild("confirm"));
        assertNotNull(nodes.get(0).getChild("profile"));
        assertNotNull(nodes.get(3).getChild("confirm"));
        assertNull(nodes.get(0).getChild("reload"));
    }

    @Test
    void shortcutChecksBasePermissionAndAccessibleProfile() throws Exception {
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        final RandomTeleporter teleporter = mock(RandomTeleporter.class);
        final Server server = mock(Server.class);
        final CommandMap commandMap = mock(CommandMap.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(plugin.getServer()).thenReturn(server);
        when(server.getCommandMap()).thenReturn(commandMap);
        when(commandMap.getKnownCommands()).thenReturn(new HashMap<>());
        when(teleporter.getRtpSettings()).thenReturn(new ArrayList<>(List.of(profile("end", true))));
        final List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        final Commands registrar = mock(Commands.class, invocation -> {
            if ("register".equals(invocation.getMethod().getName())) {
                nodes.add(invocation.getArgument(0));
                return Set.of();
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        when(registrar.getDispatcher()).thenReturn(new CommandDispatcher<>());
        BrigadierCommands.register(plugin, registrar);

        final Player player = mock(Player.class);
        final CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(player);
        when(player.hasPermission("jakesrtp.use")).thenReturn(true);
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>(List.of("end")));

        final LiteralCommandNode<CommandSourceStack> end = nodes.get(3);
        assertTrue(end.getRequirement().test(source));
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>());
        assertFalse(end.getRequirement().test(source));
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>(List.of("end")));
        when(player.hasPermission("jakesrtp.use")).thenReturn(false);
        assertFalse(end.getRequirement().test(source));
    }

    @Test
    void configurableAliasesRegisterDirectAndNestedCommandsWithoutByNamePermission() throws Exception {
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        final RandomTeleporter teleporter = mock(RandomTeleporter.class);
        final Server server = mock(Server.class);
        final CommandMap commandMap = mock(CommandMap.class);
        final YamlConfiguration config = new YamlConfiguration();
        config.set("aliases.recurso", List.of("minerar", "explorar", "confirm", "rtp",
                "nether", "ocupado", "two words", "minerar"));
        config.set("aliases.disabled", List.of("invalido"));
        config.set("aliases.unknown", List.of("inexistente"));
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(plugin.getServer()).thenReturn(server);
        when(server.getCommandMap()).thenReturn(commandMap);
        final Command existing = mock(Command.class);
        final Command disabled = mock(Command.class);
        final Map<String, Command> knownCommands = new HashMap<>(Map.of(
                "ocupado", existing, "external:ocupado", existing, "invalido", disabled));
        when(commandMap.getKnownCommands()).thenReturn(knownCommands);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        when(teleporter.getRtpSettings()).thenReturn(new ArrayList<>(List.of(
                profile("recurso", true), profile("nether", true), profile("disabled", false)
        )));

        final List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        final Commands registrar = mock(Commands.class, invocation -> {
            if ("register".equals(invocation.getMethod().getName())) {
                nodes.add(invocation.getArgument(0));
                return Set.of();
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        when(registrar.getDispatcher()).thenReturn(new CommandDispatcher<>());

        BrigadierCommands.register(plugin, registrar);

        assertEquals(List.of("rtp", "forcertp", "rtp-admin", "recurso", "nether", "minerar", "explorar", "ocupado"),
                nodes.stream().map(LiteralCommandNode::getLiteral).toList());
        assertNull(knownCommands.get("ocupado"));
        assertSame(existing, knownCommands.get("external:ocupado"));
        assertSame(disabled, knownCommands.get("invalido"));
        final LiteralCommandNode<CommandSourceStack> rtp = nodes.get(0);
        assertNotNull(rtp.getChild("profile"));
        assertNull(rtp.getChild("recurso")); // Canonical /rtp <profile> still requires usebyname.
        assertNotNull(rtp.getChild("minerar"));
        assertNotNull(rtp.getChild("minerar").getChild("confirm"));
        assertNotNull(nodes.get(5).getChild("confirm"));
        assertNotNull(rtp.getChild("ocupado"));
        assertNull(rtp.getChild("disabled"));

        final Player player = mock(Player.class);
        final CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(player);
        when(player.hasPermission("jakesrtp.use")).thenReturn(true);
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>(List.of("recurso")));
        assertTrue(nodes.get(5).getRequirement().test(source));
        assertTrue(rtp.getChild("minerar").getRequirement().test(source));
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>());
        assertFalse(nodes.get(5).getRequirement().test(source));
        assertFalse(rtp.getChild("minerar").getRequirement().test(source));
        when(player.hasPermission("jakesrtp.use")).thenReturn(false);
        assertFalse(nodes.get(5).getRequirement().test(source));
    }

    private static RtpProfile profile(final String name, final boolean enabled) throws Exception {
        final RtpProfile profile = mock(RtpProfile.class);
        final Field nameField = RtpProfile.class.getField("name");
        final Field enabledField = RtpProfile.class.getField("commandEnabled");
        nameField.setAccessible(true);
        enabledField.setAccessible(true);
        nameField.set(profile, name);
        enabledField.set(profile, enabled);
        return profile;
    }
}
