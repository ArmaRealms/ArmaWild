package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleportAction;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import biz.donvi.jakesRTP.exception.JrtpBaseException;
import biz.donvi.jakesRTP.exception.NotPermittedException;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;

/** Validates and executes RTP requests independently of command dispatch. */
final class RtpExecutionService {
    @FunctionalInterface
    interface ConfirmationPresenter {
        void show(Player player, String profile, String cost, Consumer<Player> onConfirm);
    }

    private final RandomTeleporter randomTeleporter;
    private final ConfirmationPresenter confirmationPresenter;

    RtpExecutionService(final RandomTeleporter randomTeleporter,
                        final ConfirmationPresenter confirmationPresenter) {
        this.randomTeleporter = randomTeleporter;
        this.confirmationPresenter = confirmationPresenter;
    }

    void request(final Player player, final RtpProfile relSettings, final boolean confirmed,
             final boolean requiresNamedPermission, final boolean resolveByWorld) throws JrtpBaseException {
        // Cooldown check with bypass permission
        final boolean bypassCooldown = player.hasPermission("jakesrtp.nocooldown")
                || player.hasPermission("jakesrtp.nocooldown." + relSettings.name.toLowerCase());
        final boolean cooldownOk = relSettings.coolDown.check(player.getName());
        if (!(bypassCooldown || cooldownOk)) {
            player.sendMessage(Messages.NEED_WAIT_COOLDOWN.format(
                    Placeholder.component("time", relSettings.coolDown.timeLeftComponent(player.getName()))));
            return;
        }

        // Already in warmup
        if (randomTeleporter.playersInWarmup.containsKey(player.getUniqueId())) {
            player.sendMessage(Messages.WARMUP_RTP_ALREADY_CALLED.format());
            return;
        }

        // Determine warmup necessity
        final boolean warmup = relSettings.warmupEnabled
                && !player.hasPermission("jakesrtp.nowarmup")
                && !player.hasPermission("jakesrtp.nowarmup." + relSettings.name.toLowerCase());

        // Economy check: require funds if economy enabled and cost > 0
        final boolean needsPayment = plugin.canUseEconomy() && relSettings.cost > 0;
        if (needsPayment && plugin.getEconomy().getBalance(player) < relSettings.cost) {
            player.sendMessage(Messages.ECON_NOT_ENOUGH_MONEY.format(
                    Placeholder.unparsed("cost", plugin.getEconomy().format(relSettings.cost)),
                    Placeholder.unparsed("balance", plugin.getEconomy().format(plugin.getEconomy().getBalance(player)))));
            return;
        }

        if (needsPayment && !confirmed) {
            confirmationPresenter.show(player, relSettings.name,
                    plugin.getEconomy().format(relSettings.cost),
                    confirmedPlayer -> confirm(confirmedPlayer, player.getUniqueId(),
                            relSettings.name, requiresNamedPermission, resolveByWorld, relSettings.cost));
            return;
        }

        // All checks passed: execute teleport (immediately or schedule warmup)
        final Runnable execRtp = makeRunnable(player, relSettings, warmup);
        if (warmup) {
            final int taskID = player.getServer().getScheduler()
                    .scheduleSyncRepeatingTask(plugin, execRtp, 2, 20);
            if (taskID == -1) throw new JrtpBaseException("Could not schedule rtp-after-warmup.");
            randomTeleporter.playersInWarmup.put(player.getUniqueId(), taskID);
        } else {
            execRtp.run();
        }
    }

    private void confirm(final Player player, final UUID requesterId, final String profileName,
                         final boolean requiresNamedPermission, final boolean resolveByWorld, final double presentedCost) {
        if (!player.getUniqueId().equals(requesterId) || !player.isOnline() || !plugin.isEnabled()) return;
        if (!player.hasPermission("jakesrtp.use")) {
            player.sendMessage(Messages.NP_NO_PERMISSION.format(
                    Placeholder.unparsed("permission", "jakesrtp.use")));
            return;
        }
        if (requiresNamedPermission && !player.hasPermission("jakesrtp.usebyname")) {
            player.sendMessage(Messages.NP_NO_PERMISSION.format(
                    Placeholder.unparsed("permission", "jakesrtp.usebyname")));
            return;
        }
        try {
            // A callback must resolve the profile again: permissions, cost and other settings can change.
            final RandomTeleporter currentTeleporter = plugin.getRandomTeleporter();
            if (currentTeleporter == null) {
                player.sendMessage(Messages.NP_R_NOT_ENABLED.format(Placeholder.unparsed("code", "~ECP")));
                return;
            }
            final RtpProfile currentProfile = resolveByWorld
                    ? currentTeleporter.getRtpSettingsByWorldForPlayer(player)
                    : currentTeleporter.getRtpSettingsByNameForPlayer(player, profileName);
            final boolean priceIncreased = plugin.canUseEconomy() && currentProfile.cost > presentedCost;
            new RtpExecutionService(currentTeleporter, confirmationPresenter)
                    .request(player, currentProfile, !priceIncreased, requiresNamedPermission, resolveByWorld);
        } catch (final NotPermittedException npe) {
            player.sendMessage(Messages.NP_GENERIC.format(
                    Placeholder.component("reason", JrtpBaseException.userMessage(npe))));
        } catch (final JrtpBaseException e) {
            player.sendMessage(JrtpBaseException.userMessage(e));
            e.printStackTrace();
        }
    }

    private void scheduleCooldownEndNotice(final Player player, final @NotNull RtpProfile profile) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(Messages.COOLDOWN_OVER.format(Placeholder.unparsed("profile", profile.name)));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }, profile.coolDown.getCoolDownTimeInTicks());
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    private @NotNull Runnable makeRunnable(final @NotNull Player player, final RtpProfile rtpProfile, final boolean calculatedWarmup) {
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
                                Placeholder.unparsed("seconds", Integer.toString(rtpProfile.warmup - timeDifInSeconds()))
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
                    rtpProfile.coolDown.log(player.getName());
                    scheduleCooldownEndNotice(player, rtpProfile);
                    // Charge the player
                    if (rtpProfile.cost > 0) {
                        final EconomyResponse er = plugin.getEconomy().withdrawPlayer(player, rtpProfile.cost);
                        if (er.transactionSuccess()) player.sendMessage(Messages.ECON_YOU_WERE_CHARGED_X.format(
                                Placeholder.unparsed("amount", plugin.getEconomy().format(er.amount)),
                                Placeholder.unparsed("balance", plugin.getEconomy().format(er.balance))));
                        else player.sendMessage(Messages.ECON_ERROR.format(
                                Placeholder.unparsed("error", String.valueOf(er.errorMessage))));
                    }
                } catch (final Exception e) {
                    player.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(Placeholder.component("reason", JrtpBaseException.userMessage(e))));
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

}
