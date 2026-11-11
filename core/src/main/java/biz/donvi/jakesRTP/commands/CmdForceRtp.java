package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.GeneralUtil;
import biz.donvi.jakesRTP.exception.JrtpBaseException;
import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleportAction;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import biz.donvi.jakesRTP.exception.NotPermittedException;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdForceRtp {

    private final RandomTeleporter randomTeleporter;

    public CmdForceRtp(final RandomTeleporter randomTeleporter) {
        this.randomTeleporter = randomTeleporter;
    }

    public boolean execute(final CommandSender sender, final String[] args) {
        if (args.length == 0 || args.length == 2) return false;
        final String flag = args.length == 1 ? null : args[1];
        final boolean byProfile = "-c".equalsIgnoreCase(flag);
        final boolean byWorld = "-w".equalsIgnoreCase(flag);
        if (args.length > 1 && !byProfile && !byWorld) return false;

        try {
            final Server server = sender.getServer();
            final Player player = server.getPlayerExact(args[0]);
            if (player == null) {
                sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(Placeholder.unparsed("player", args[0])));
                return true;
            }

            final Location playerLocation = player.getLocation();
            final World currentWorld = playerLocation.getWorld();
            World world = currentWorld;
            if (byWorld) {
                world = GeneralUtil.getWorldIgnoreCase(server, args[2]);
                if (world == null) {
                    sender.sendMessage(Messages.WORLD_NOT_FOUND.format(Placeholder.unparsed("world", args[2])));
                    return true;
                }
            }

            final RtpProfile profile = byProfile ? randomTeleporter.getRtpSettingsByName(args[2])
                    : randomTeleporter.getRtpSettingsByWorld(world);
            final Location origin = world == currentWorld ? playerLocation : world.getSpawnLocation();
            new RandomTeleportAction(randomTeleporter, profile, origin, true, true,
                    randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!").teleportAsync(player);
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

}
