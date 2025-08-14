package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.JrtpBaseException;
import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleportAction;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.List;

import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;

public class CmdRtp implements TabExecutor {

    private final RandomTeleporter randomTeleporter;
    // Track scheduled cooldown notifications per player UUID -> record, inside record map profileName->taskId
    private final Map<UUID, CooldownNotifyRecord> cooldownNotifies = new ConcurrentHashMap<>();

    private static record CooldownNotifyRecord(Map<String, Integer> perProfileTaskIds) {}

    public CmdRtp(final RandomTeleporter randomTeleporter) {
        this.randomTeleporter = randomTeleporter;
    }

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        try {
            if ((args.length == 0 || args.length == 1) && sender instanceof Player) {
                final Player player = (Player) sender;
                if (args.length == 1 && !sender.hasPermission("jakesrtp.usebyname"))
                    return false;
                final RtpProfile relSettings = args.length == 0
                        ? randomTeleporter.getRtpSettingsByWorldForPlayer(player)
                        : randomTeleporter.getRtpSettingsByNameForPlayer(player, args[0]);
                if (player.hasPermission("jakesrtp.nocooldown") // If the player has permission to skip cooldown
                        || player.hasPermission("jakesrtp.nocooldown." + relSettings.name.toLowerCase())
                        || relSettings.coolDown.check(player.getName())) { // Or they are not on cooldown
                    if (!randomTeleporter.playersInWarmup.containsKey(player.getUniqueId())) {
                        final boolean warmup =
                                relSettings.warmupEnabled && // Obvious...
                                        !player.hasPermission("jakesrtp.nowarmup") && // Or if they have the perm to avoid it
                                        !player.hasPermission("jakesrtp.nowarmup." + relSettings.name.toLowerCase());
                        if (!plugin.canUseEconomy() || relSettings.cost <= 0 ||
                                plugin.getEconomy().getBalance(player) >= relSettings.cost) {
                            // ==== By this point, all checks are done and the player WILL be teleported. ====
                            final Runnable execRtp = makeRunnable(player, relSettings, warmup);
                            if (warmup) { // If there is a warmup, schedule the runnable
                                final int taskID = sender
                                        .getServer().getScheduler() // Get the task ID so that we can cancel it later.
                                        .scheduleSyncRepeatingTask(plugin, execRtp, 2, 20);
                                if (taskID == -1) // This should only really happen during shutdown.
                                    throw new JrtpBaseException("Could not schedule rtp-after-warmup.");
                                randomTeleporter.playersInWarmup.put(player.getUniqueId(),
                                        taskID); // Needed for canceling.
                            } else execRtp.run(); // No warmup, just run the teleport.
                        } else player.sendMessage(Messages.ECON_NOT_ENOUGH_MONEY.format(
                                relSettings.cost, plugin.getEconomy().getBalance(player)));
                    } else player.sendMessage(Messages.WARMUP_RTP_ALREADY_CALLED.format());
                } else {
                    player.sendMessage(Messages.NEED_WAIT_COOLDOWN.format(
                            relSettings.coolDown.timeLeftWords(player.getName())));
                    scheduleCooldownEndNotice(player, relSettings);
                }
            }
        } catch (final JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (final JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private void scheduleCooldownEndNotice(final Player player, final RtpProfile profile) {
        final long msLeft = profile.coolDown.timeLeft(player.getName());
        if (msLeft <= 0) return;
        final int ticks = (int) Math.max(1, (msLeft + 49) / 50); // ceil to ticks
        final UUID uuid = player.getUniqueId();
        final BukkitScheduler scheduler = player.getServer().getScheduler();

        // Ensure record exists
        final CooldownNotifyRecord record = cooldownNotifies.computeIfAbsent(
                uuid, u -> new CooldownNotifyRecord(new ConcurrentHashMap<>())
        );
        final Map<String, Integer> perProfile = record.perProfileTaskIds();
        final String profileKey = profile.name.toLowerCase();
        // Cancel prior task for this profile if any
        final Integer prev = perProfile.remove(profileKey);
        if (prev != null) scheduler.cancelTask(prev);

        final int taskId = scheduler.scheduleSyncDelayedTask(plugin, () -> {
            try {
                final Player p = plugin.getServer().getPlayer(uuid);
                if (p == null || !p.isOnline()) return;
                // Only notify if cooldown actually ended
                if (profile.coolDown.timeLeft(p.getName()) <= 0) {
                    p.sendMessage(Messages.COOLDOWN_OVER.format(profile.name));
                }
            } finally {
                final CooldownNotifyRecord rec = cooldownNotifies.get(uuid);
                if (rec != null) {
                    final Map<String, Integer> m = rec.perProfileTaskIds();
                    m.remove(profileKey);
                    if (m.isEmpty()) cooldownNotifies.remove(uuid);
                }
            }
        }, ticks);
        if (taskId != -1) perProfile.put(profileKey, taskId);
    }

    private Runnable makeRunnable(final Player player, final RtpProfile rtpProfile, final boolean calculatedWarmup) {
        return new Runnable() {
            private final BukkitScheduler scheduler = player.getServer().getScheduler();
            private final Location startLoc = player.getLocation().clone();
            private final boolean warmup = calculatedWarmup;
            private final long startTime = System.currentTimeMillis();
            private int done = 0;

            @Override
            public void run() {
                // The annoying error message, lets hope we never need use this...
                if (done > 1) taskError();
                    // If There should be no warmup, we teleport the user immediately.
                else if (!warmup) teleport();
                    // If we want the user to stand still AND they move, we cancel this runnable / future rtp.
                else if (rtpProfile.warmupCancelOnMove &&
                        (startLoc.getWorld() != player.getWorld() || startLoc.distance(player.getLocation()) > 1))
                    cancel();
                    // If we have waited enough time, we teleport the user.
                else if (timeDifInSeconds() >= rtpProfile.warmup) teleport();
                    // If we got to this point, the user still has to wait, and if wanted, we let them know how long.
                else if (rtpProfile.warmupCountDown) countDown();
                // If none of these were called, we just silently wait until the next time run() is called.
            }

            private int timeDifInSeconds() {
                return (int) ((System.currentTimeMillis() - startTime) / 1000);
            }

            private void countDown() {
                player.sendMessage(Messages.
                        WARMUP_TELEPORTING_IN_X.format(
                                rtpProfile.warmup - timeDifInSeconds()
                        ));
            }

            private void teleport() {
                try {
                    if (rtpProfile.cost > 0 && plugin.getEconomy().getBalance(player) < rtpProfile.cost) {
                        player.sendMessage(Messages.ECON_NO_LONGER_ENOUGH_MONEY.format());
                        return;
                    }
                    // Do the teleport action
                    final var rtpAction = new RandomTeleportAction(
                            randomTeleporter,
                            rtpProfile,
                            player.getLocation(),
                            true,
                            true,
                            randomTeleporter.logRtpOnCommand, "Rtp-from-command triggered!"
                    );
                    if (rtpProfile.preferSyncTpOnCommand)
                        rtpAction.teleportSync(player);
                    else rtpAction.teleportAsync(player);
                    // Log in the cooldown list
                    rtpProfile.coolDown.log(player.getName(), System.currentTimeMillis());
                    // Charge the player
                    if (rtpProfile.cost > 0) {
                        final EconomyResponse er = plugin.getEconomy().withdrawPlayer(player, rtpProfile.cost);
                        if (er.transactionSuccess()) player.sendMessage(Messages.ECON_YOU_WERE_CHARGED_X.format(
                                plugin.getEconomy().format(er.amount),
                                plugin.getEconomy().format(er.balance)));
                        else player.sendMessage(Messages.ECON_ERROR.format(
                                "An economy error occurred: {0}", er.errorMessage));
                    }
                } catch (final Exception e) {
                    player.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
                    e.printStackTrace();
                } finally {
                    cancelTask();
                }
            }

            private void cancel() {
                player.sendMessage(Messages.WARMUP_CANCEL_BECAUSE_MOVE.format());
                cancelTask();
            }

            private void cancelTask() {
                done++;
                final Integer taskID = randomTeleporter.playersInWarmup.remove(player.getUniqueId());
                if (taskID != null)
                    scheduler.cancelTask(taskID); // Only cancel if task existed.
            }

            private void taskError() {
                if (done > 1000) scheduler.cancelTasks(plugin); // Emergency cleanup.
                if (done < 10 || done % 100 == 0) throw new RuntimeException("RTP task run twice?? Please report.");
                done++; // This is meant to be annoying, but not *too* annoying.
            }
        };
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        // When sender can't use by name or isn't a player, no suggestions.
        if (!(sender instanceof Player) || !sender.hasPermission("jakesrtp.usebyname")) return List.of();

        // Provide suggestions even when args.length == 0 (alias like /wild often sends empty args during completion)
        final String prefix = args.length == 0 ? "" : args[0];
        final ArrayList<String> out = new ArrayList<>();
        for (final String name : randomTeleporter.getRtpSettingsNamesForPlayer((Player) sender))
            if (name.toLowerCase().startsWith(prefix.toLowerCase())) out.add(name);
        return out;
    }
}
