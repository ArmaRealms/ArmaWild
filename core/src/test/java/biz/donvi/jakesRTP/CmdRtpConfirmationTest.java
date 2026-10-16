package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.commands.CmdRtp;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Server;
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
import java.util.function.Consumer;
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
    private YamlConfiguration config;
    private final List<DialogRequest> dialogRequests = new ArrayList<>();

    private record DialogRequest(Player player, String profile, String formattedCost, Consumer<Player> onConfirm) {
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
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("jakesrtp.use")).thenReturn(true);
        when(JakesRtpPlugin.plugin.isEnabled()).thenReturn(true);
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
        when(JakesRtpPlugin.plugin.getRandomTeleporter()).thenReturn(teleporter);
        when(teleporter.getRtpSettingsByNameForPlayer(player, "survival")).thenReturn(profile);
        when(teleporter.getRtpSettingsNamesForPlayer(player)).thenReturn(new ArrayList<>(List.of("survival")));
        Messages.setMap(new Yaml().load(getClass().getResourceAsStream("/translations/lang_en.yml")));
    }

    @AfterEach
    void tearDown() {
        JakesRtpPlugin.plugin = null;
    }

    @Test
    void paidRtpOnlyPromptsWithoutStartingWarmupOrCharging() {
        invoke("rtp");
        assertDialog();
        assertTrue(teleporter.playersInWarmup.isEmpty());
        assertEquals(-1, profile.coolDown.timeDifference(player.getName()));
        verifyNoInteractions(scheduler);
        verify(economy, never()).withdrawPlayer(eq(player), anyDouble());
    }

    @Test
    void paidRtpWithoutWarmupStillRequiresConfirmation() throws Exception {
        setField(profile, "warmupEnabled", false);
        invoke("rtp");
        assertDialog();
        verify(economy, never()).withdrawPlayer(eq(player), anyDouble());
    }

    @Test
    void namedPaidRtpWithCustomSubcommandOpensDialog() {
        config.set("rtp-confirmation.subcommand", "aceitar");
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(true);
        invoke("wild", "survival");
        assertDialog();
    }

    @Test
    void confirmationDoesNotRequireNamedProfilePermission() {
        invoke("rtp", "CONFIRM");
        assertWarmupStarted();
        assertTrue(dialogRequests.isEmpty());
        verify(player, never()).showDialog(any(Dialog.class));
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
        assertTrue(dialogRequests.isEmpty());
        verify(player, never()).showDialog(any(Dialog.class));
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
    void shortcutOpensDialogWithoutNamedProfilePermission() {
        invokeShortcut("survival", "survival");
        assertDialog();
        verifyNoInteractions(scheduler);
    }

    @Test
    void nestedShortcutPreservesAliasWhenConfirmingPaidTeleport() {
        invokeShortcut("rtp minerar", "survival");
        assertDialog();
    }

    @Test
    void shortcutConfirmationBypassesNamedPermissionAndStartsWarmup() {
        invokeShortcut("survival", "survival", "CONFIRM");
        assertWarmupStarted();
        assertTrue(dialogRequests.isEmpty());
        verify(player, never()).showDialog(any(Dialog.class));
        verify(player, never()).sendMessage(Messages.NP_NO_PERMISSION.format(
                Placeholder.unparsed("permission", "jakesrtp.usebyname")));
    }

    @Test
    void invalidSubcommandFallsBackToConfirm() {
        config.set("rtp-confirmation.subcommand", "two words");
        invoke("rtp");
        assertDialog();
    }

    @Test
    void malformedArgumentsCannotConfirmOrStartRtp() {
        invoke("rtp", "survival", "anything");
        invoke("rtp", "survival", "anything", "confirm");
        verifyNoInteractions(teleporter, scheduler, economy);
    }

    @Test
    void miniMessageDoesNotParseInjectedEconomyPlaceholderContent() {
        Messages.addMap(new HashMap<>(Map.of("confirm-rtp-dialog-body", "<cost>")));
        final Component result = Messages.ECON_CONFIRM_DIALOG_BODY.format(
                Placeholder.unparsed("cost", "<red>25 coins</red>"));
        assertEquals("<red>25 coins</red>", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void insufficientFundsDoNotOpenDialog() {
        when(economy.getBalance(player)).thenReturn(0.0);
        when(economy.format(0.0)).thenReturn("0 coins");
        invoke("rtp");
        assertTrue(dialogRequests.isEmpty());
        verify(player, never()).showDialog(any(Dialog.class));
        verifyNoInteractions(scheduler);
    }

    private CmdRtp command() {
        return new CmdRtp(teleporter) {
            @Override
            protected void showConfirmationDialog(final Player player, final String profile,
                                                  final String formattedCost, final Consumer<Player> onConfirm) {
                dialogRequests.add(new DialogRequest(player, profile, formattedCost, onConfirm));
            }
        };
    }

    private void invoke(final String label, final String... args) {
        assertTrue(command().execute(player, label, args, null));
    }

    private void invokeShortcut(final String label, final String profileName, final String... args) {
        assertTrue(command().execute(player, label, args, profileName));
    }

    private void assertWarmupStarted() {
        verify(scheduler).scheduleSyncRepeatingTask(eq(JakesRtpPlugin.plugin), any(Runnable.class), eq(2L), eq(20L));
        assertEquals(42, teleporter.playersInWarmup.get(player.getUniqueId()));
    }

    private void assertDialog() {
        assertEquals(1, dialogRequests.size());
        assertEquals(player, dialogRequests.getFirst().player());
        assertEquals("survival", dialogRequests.getFirst().profile());
        assertEquals("25 coins", dialogRequests.getFirst().formattedCost());
        verify(player, never()).showDialog(any(Dialog.class));
        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void dialogCallbackStartsRtpWithoutDispatchingACommand() {
        invoke("rtp");
        assertDialog();
        dialogRequests.getFirst().onConfirm().accept(player);
        assertWarmupStarted();
    }

    @Test
    void dialogCallbackRechecksBalanceBeforeStartingWarmup() {
        invoke("rtp");
        when(economy.getBalance(player)).thenReturn(0.0);
        when(economy.format(0.0)).thenReturn("0 coins");
        dialogRequests.getFirst().onConfirm().accept(player);
        verify(player).sendMessage(Messages.ECON_NOT_ENOUGH_MONEY.format(
                Placeholder.unparsed("cost", "25 coins"),
                Placeholder.unparsed("balance", "0 coins")));
        verifyNoInteractions(scheduler);
    }

    @Test
    void dialogCallbackRechecksNamedProfilePermission() {
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(true);
        invoke("rtp", "survival");
        assertDialog();
        when(player.hasPermission("jakesrtp.usebyname")).thenReturn(false);
        dialogRequests.getFirst().onConfirm().accept(player);
        verify(player).sendMessage(Messages.NP_NO_PERMISSION.format(
                Placeholder.unparsed("permission", "jakesrtp.usebyname")));
        verifyNoInteractions(scheduler);
    }

    @Test
    void dialogCallbackRechecksCooldown() {
        invoke("rtp");
        profile.coolDown.log(player.getName());
        dialogRequests.getFirst().onConfirm().accept(player);
        verifyNoInteractions(scheduler);
    }

    @Test
    void dialogCallbackIsBoundToOriginalPlayer() {
        invoke("rtp");
        final Player otherPlayer = mock(Player.class);
        when(otherPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(otherPlayer.isOnline()).thenReturn(true);
        dialogRequests.getFirst().onConfirm().accept(otherPlayer);
        verifyNoInteractions(scheduler);
    }

    @Test
    void dialogCallbackUsesCurrentTeleporterAfterReload() throws Exception {
        invoke("rtp");
        final RandomTeleporter replacement = mock(RandomTeleporter.class);
        setField(replacement, "playersInWarmup", new HashMap<UUID, Integer>());
        when(replacement.getRtpSettingsByWorldForPlayer(player)).thenReturn(profile);
        when(JakesRtpPlugin.plugin.getRandomTeleporter()).thenReturn(replacement);
        dialogRequests.getFirst().onConfirm().accept(player);
        assertEquals(42, replacement.playersInWarmup.get(player.getUniqueId()));
        assertTrue(teleporter.playersInWarmup.isEmpty());
    }

    @Test
    void dialogCallbackRequestsNewConfirmationIfPriceIncreases() throws Exception {
        invoke("rtp");
        setField(profile, "cost", 50.0);
        when(economy.format(50.0)).thenReturn("50 coins");
        dialogRequests.getFirst().onConfirm().accept(player);
        assertEquals(2, dialogRequests.size());
        assertEquals("50 coins", dialogRequests.get(1).formattedCost());
        verifyNoInteractions(scheduler);
    }
}

