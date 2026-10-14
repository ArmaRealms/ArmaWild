package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.GeneralUtil;
import biz.donvi.jakesRTP.exception.JrtpBaseException;
import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleportAction;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import biz.donvi.jakesRTP.exception.NotPermittedException;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



public class CmdForceRtp {

    private final RandomTeleporter randomTeleporter;

    public CmdForceRtp(final RandomTeleporter randomTeleporter) {
        this.randomTeleporter = randomTeleporter;
    }

    public boolean execute(final CommandSender sender, final String[] args) {
        try {
            // Expected: /forcertp <playerName> -c <configName> | -w <worldName>
            if (args.length < 3) return false;

            final String targetName = args[0];
            final String flag = args[1];

            if ("-c".equalsIgnoreCase(flag)) {
                subForceRtpWithConfig(sender, targetName, args[2]);
            } else if ("-w".equalsIgnoreCase(flag)) {
                subForceRtpWithWorld(sender, targetName, args[2]);
            } else {
                return false;
            }
        } catch (final NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(Placeholder.component("reason", JrtpBaseException.userMessage(npe))));
        } catch (final JrtpBaseException e) {
            sender.sendMessage(JrtpBaseException.userMessage(e));
        } catch (final Exception e) {
            sender.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(Placeholder.component("reason", JrtpBaseException.userMessage(e))));
            e.printStackTrace();
        }
        return true;
    }

    private void subForceRtpWithConfig(final CommandSender sender, final String playerName, final String configName) throws Exception {
        final Player playerToTp = sender.getServer().getPlayerExact(playerName);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(Placeholder.unparsed("player", playerName)));
            return;
        }
        final RtpProfile rtpProfile = randomTeleporter.getRtpSettingsByName(configName);

        // ↑ Check step | Teleport step ↓

        new RandomTeleportAction(
                randomTeleporter, rtpProfile, playerToTp.getLocation(), true, true,
                randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        ).teleportAsync(playerToTp);
    }

    private void subForceRtpWithWorld(final CommandSender sender, final String playerName, final String worldName) throws Exception {
        final Player playerToTp = sender.getServer().getPlayerExact(playerName);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(Placeholder.unparsed("player", playerName)));
            return;
        }
        final World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), worldName);
        if ((destWorld) == null) {
            sender.sendMessage(Messages.WORLD_NOT_FOUND.format(Placeholder.unparsed("world", worldName)));
            return;
        }

        // ↑ Check step | Teleport step ↓

        new RandomTeleportAction(
                randomTeleporter,
                randomTeleporter.getRtpSettingsByWorld(destWorld),
                playerToTp.getLocation().getWorld() == destWorld
                        ? playerToTp.getLocation()
                        : destWorld.getSpawnLocation(),
                true,
                true,
                randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        ).teleportAsync(playerToTp);
    }

}
