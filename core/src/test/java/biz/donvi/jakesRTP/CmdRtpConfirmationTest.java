package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.commands.CmdRtp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CmdRtpConfirmationTest {
    private RandomTeleporter teleporter;
    private RtpProfile profile;
    private Player player;
    private Economy economy;
    private BukkitScheduler scheduler;
    private Command command;
    private YamlConfiguration config;

    private static boolean hasClick(final Component component, final String command) {
        return ClickEvent.runCommand(command).equals(component.clickEvent())
                || component.children().stream().anyMatch(child -> hasClick(child, command));
    }

    private static boolean hasHoverText(final Component component, final String expected) {
        return (component.hoverEvent() != null && component.hoverEvent().value() instanceof Component text
                && PlainTextComponentSerializer.plainText().serialize(text).equals(expected))
                || component.children().stream().anyMatch(child -> hasHoverText(child, expected));
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        JakesRtpPlugin.plugin = mock(JakesRtpPlugin.class);
        config = new YamlConfiguration();
        when(JakesRtpPlugin.plugin.getConfig()).thenReturn(config);
        when(JakesRtpPlugin.plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        economy = mock(Economy.class);
        when(JakesRtpPlugin.plugin.canUseEconomy()).thenReturn(true);
        when(JakesRtpPlugin.plugin.getEconomy()).thenReturn(economy);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Player");
        when(player.getLocation()).thenReturn(new Location(null, 0, 64, 0));
        when(economy.getBalance(player)).thenReturn(100.0);
        when(economy.format(25.0)).thenReturn("25 coins");
        scheduler = mock(BukkitScheduler.class);
        final Server server = mock(Server.class);
        when(player.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.scheduleSyncRepeatingTask(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(42);

        profile = mock(RtpProfile.class);
        setField(profile, "name", "survival");
        setField(profile, "cost", 25.0);
        setField(profile, "warmupEnabled", true);
        setField(profile, "coolDown", new CoolDownTracker(30));
        teleporter = mock(RandomTeleporter.class);
        setField(teleporter, "playersInWarmup", new HashMap<UUID, Integer>());
        when(teleporter.getRtpSettingsByWorldForPlayer(player)).thenReturn(profile);
        when(teleporter.getRtpSettingsByNameForPlayer(player, "survival")).thenReturn(profile);
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>(List.of("survival")));
        command = mock(Command.class);
        Messages.setMap(new Yaml().load(getClass().getResourceAsStream("/translations/lang_en.yml")));
    }

    @AfterEach
    void tearDown() {
        JakesRtpPlugin.plugin = null;
    }

    @Test
    void paidRtpOnlyPromptsWithoutStartingWarmupOrCharging() {
        invoke("rtp");
        assertPrompt("/rtp confirm");
        assertTrue(teleporter.playersInWarmup.isEmpty());
        assertEquals(-1, profile.coolDown.timeDifference(player.getName()));
        verifyNoInteractions(scheduler);
        verify(economy, never()).withdrawPlayer(eq(player), anyDouble());
    }

    @Test
    void paidRtpWithoutWarmupStillRequiresConfirmation() throws Exception {
        setField(profile, "warmupEnabled", false);
        invoke("rtp");
        assertPrompt("/rtp confirm");
        verify(economy, never()).withdrawPlayer(eq(player), anyDouble());
    }

    @Test
    void namedPromptPreservesAliasAndProfileWithCustomSubcommand() {
        config.set("rtp-confirmation.subcommand", "aceitar");
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(true);
        invoke("wild", "survival");
        assertPrompt("/wild survival aceitar");
    }

    @Test
    void confirmationDoesNotRequireNamedProfilePermission() {
        invoke("rtp", "CONFIRM");
        assertWarmupStarted();
        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void namedConfirmationStillRequiresPermission() throws Exception {
        invoke("rtp", "survival", "confirm");
        verify(player).sendMessage(Messages.NP_NO_PERMISSION.format(Placeholder.unparsed("permission", "jakesrtp.usebyname")));
        verify(teleporter, never()).getRtpSettingsByNameForPlayer(any(), anyString());
        verifyNoInteractions(scheduler);
    }

    @Test
    void namedConfirmationAcceptsConfiguredSubcommand() {
        config.set("rtp-confirmation.subcommand", "aceitar");
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(true);
        invoke("wild", "survival", "ACEITAR");
        assertWarmupStarted();
    }

    @Test
    void freeRtpStartsWithoutConfirmation() throws Exception {
        setField(profile, "cost", 0.0);
        invoke("rtp");
        assertWarmupStarted();
        verifyNoInteractions(economy);
    }

    @Test
    void insufficientFundsAreCheckedAgainOnConfirmation() {
        invoke("rtp");
        when(economy.getBalance(player)).thenReturn(0.0);
        when(economy.format(0.0)).thenReturn("0 coins");
        invoke("rtp", "confirm");
        verify(player).sendMessage(Messages.ECON_NOT_ENOUGH_MONEY.format(Placeholder.unparsed("cost", "25 coins"), Placeholder.unparsed("balance", "0 coins")));
        verifyNoInteractions(scheduler);
    }

    @Test
    void fundsAreCheckedAgainAfterWarmup() {
        invoke("rtp", "confirm");
        final ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleSyncRepeatingTask(eq(JakesRtpPlugin.plugin), task.capture(), eq(2L), eq(20L));
        when(economy.getBalance(player)).thenReturn(0.0);
        task.getValue().run();
        verify(player).sendMessage(Messages.ECON_NO_LONGER_ENOUGH_MONEY.format());
        verify(economy, never()).withdrawPlayer(eq(player), anyDouble());
        assertTrue(teleporter.playersInWarmup.isEmpty());
    }

    @Test
    void confirmationStillRespectsCooldownAndExistingWarmup() {
        profile.coolDown.log(player.getName());
        invoke("rtp", "confirm");
        verifyNoInteractions(scheduler);
        when(player.hasPermission("jakesrtp.nocooldown")).thenReturn(true);
        teleporter.playersInWarmup.put(player.getUniqueId(), 99);
        invoke("rtp", "confirm");
        verify(player).sendMessage(Messages.WARMUP_RTP_ALREADY_CALLED.format());
        verifyNoInteractions(scheduler);
    }

    @Test
    void completionIncludesConfirmationWithoutNamedProfilePermission() {
        final CmdRtp executor = new CmdRtp(teleporter);
        assertEquals(List.of("confirm"), executor.onTabComplete(player, command, "rtp", new String[]{"co"}));
        assertTrue(executor.onTabComplete(player, command, "rtp", new String[]{"survival", ""}).isEmpty());
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(true);
        assertEquals(List.of("confirm"), executor.onTabComplete(player, command, "rtp", new String[]{"SURVIVAL", "co"}));
    }

    @Test
    void invalidSubcommandFallsBackToConfirm() {
        config.set("rtp-confirmation.subcommand", "two words");
        invoke("rtp");
        assertPrompt("/rtp confirm");
    }

    @Test
    void malformedArgumentsCannotConfirmOrStartRtp() {
        invoke("rtp", "survival", "anything");
        invoke("rtp", "survival", "anything", "confirm");
        verifyNoInteractions(teleporter, scheduler, economy);
    }

    @Test
    void miniMessagePreservesQuotedTagsAndDoesNotParsePlaceholderContent() {
        Messages.addMap(new HashMap<>(Map.of("confirm-paid-rtp",
                "<click:run_command:'/wild aceitar'><hover:show_text:'Confirmar'><cost></hover></click>")));
        final Component result = Messages.ECON_CONFIRM_RTP.format(
                Placeholder.unparsed("cost", "<red>25 coins</red>"));
        assertEquals("<red>25 coins</red>", PlainTextComponentSerializer.plainText().serialize(result));
        assertTrue(hasClick(result, "/wild aceitar"));
    }

    private void invoke(final String label, final String... args) {
        assertTrue(new CmdRtp(teleporter).onCommand(player, command, label, args));
    }

    private void assertWarmupStarted() {
        verify(scheduler).scheduleSyncRepeatingTask(eq(JakesRtpPlugin.plugin), any(Runnable.class), eq(2L), eq(20L));
        assertEquals(42, teleporter.playersInWarmup.get(player.getUniqueId()));
    }

    private void assertPrompt(final String expectedCommand) {
        final ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(message.capture());
        assertTrue(PlainTextComponentSerializer.plainText().serialize(message.getValue()).contains("25 coins"));
        assertTrue(hasClick(message.getValue(), expectedCommand));
        assertTrue(hasHoverText(message.getValue(), expectedCommand));
    }
}
