package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import biz.donvi.jakesRTP.exception.JrtpBaseException;
import biz.donvi.jakesRTP.exception.NotPermittedException;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;

public class CmdRtp {
    private final RandomTeleporter randomTeleporter;
    private final RtpExecutionService executionService;
    private final String confirmationSubcommand;

    public CmdRtp(final RandomTeleporter randomTeleporter) {
        this.randomTeleporter = randomTeleporter;
        this.executionService = new RtpExecutionService(randomTeleporter, this::showConfirmationDialog);
        final String configured = plugin.getConfig().getString("rtp-confirmation.subcommand", "confirm").trim();
        if (configured.matches("[a-zA-Z0-9_-]+")) {
            confirmationSubcommand = configured;
        } else {
            plugin.getLogger().warning("Invalid rtp-confirmation.subcommand; using 'confirm'. Use letters, numbers, '_' or '-'.");
            confirmationSubcommand = "confirm";
        }
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label,
                           final String[] args, final String shortcutProfile) {
        try {
            if (!(sender instanceof final Player player)) return true;
            if (args.length > (shortcutProfile == null ? 2 : 1)) return true;
            final boolean confirmed = args.length > 0
                    && args[args.length - 1].equalsIgnoreCase(confirmationSubcommand);
            final int profileArgs = args.length - (confirmed ? 1 : 0);
            if (profileArgs > (shortcutProfile == null ? 1 : 0)) return true;

            final boolean requiresNamedPermission = shortcutProfile == null && profileArgs == 1;
            if (requiresNamedPermission && !player.hasPermission("jakesrtp.usebyname")) {
                player.sendMessage(Messages.NP_NO_PERMISSION.format(
                        Placeholder.unparsed("permission", "jakesrtp.usebyname")));
                return true;
            }

            final RtpProfile profile = shortcutProfile != null
                    ? randomTeleporter.getRtpSettingsByNameForPlayer(player, shortcutProfile)
                    : profileArgs == 0
                    ? randomTeleporter.getRtpSettingsByWorldForPlayer(player)
                    : randomTeleporter.getRtpSettingsByNameForPlayer(player, args[0]);

            executionService.request(player, profile, confirmed, requiresNamedPermission);
        } catch (final NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(
                    Placeholder.component("reason", JrtpBaseException.userMessage(npe))));
        } catch (final JrtpBaseException e) {
            sender.sendMessage(JrtpBaseException.userMessage(e));
            e.printStackTrace();
        }
        return true;
    }

    protected void showConfirmationDialog(final Player player, final String profile,
                                          final String formattedCost, final Consumer<Player> onConfirm) {
        RtpConfirmationDialog.show(player, profile, formattedCost, onConfirm);
    }
}
