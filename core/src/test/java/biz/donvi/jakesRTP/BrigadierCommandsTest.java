package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.commands.BrigadierCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrigadierCommandsTest {
    @Test
    void registersConfiguredProfilesButNotDisabledOrCollidingShortcuts() throws Exception {
        final JakesRtpPlugin plugin = mock(JakesRtpPlugin.class);
        final RandomTeleporter teleporter = mock(RandomTeleporter.class);
        final Server server = mock(Server.class);
        final CommandMap commandMap = mock(CommandMap.class);
        final YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(plugin.getServer()).thenReturn(server);
        when(server.getCommandMap()).thenReturn(commandMap);
        when(commandMap.getCommand("mundo")).thenReturn(mock(Command.class));
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

        // Collisions log a warning, just as they do in a live server.
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        BrigadierCommands.register(plugin, registrar);

        assertEquals(List.of("rtp", "forcertp", "rtp-admin", "end", "nether", "recursos", "legado"),
                nodes.stream().map(LiteralCommandNode::getLiteral).toList());
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
        when(commandMap.getCommand("ocupado")).thenReturn(mock(Command.class));
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

        assertEquals(List.of("rtp", "forcertp", "rtp-admin", "recurso", "nether", "minerar", "explorar"),
                nodes.stream().map(LiteralCommandNode::getLiteral).toList());
        final LiteralCommandNode<CommandSourceStack> rtp = nodes.get(0);
        assertNotNull(rtp.getChild("profile"));
        assertNull(rtp.getChild("recurso")); // Canonical /rtp <profile> still requires usebyname.
        assertNotNull(rtp.getChild("minerar"));
        assertNotNull(rtp.getChild("minerar").getChild("confirm"));
        assertNotNull(nodes.get(5).getChild("confirm"));
        assertNull(rtp.getChild("ocupado"));
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
