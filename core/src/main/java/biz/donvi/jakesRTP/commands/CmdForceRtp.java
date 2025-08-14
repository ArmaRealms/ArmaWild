package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.GeneralUtil;
import biz.donvi.jakesRTP.JrtpBaseException;
import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleportAction;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class CmdForceRtp implements TabExecutor {

    private final RandomTeleporter randomTeleporter;

    public CmdForceRtp(final RandomTeleporter randomTeleporter) {
        this.randomTeleporter = randomTeleporter;
    }

    private static List<String> filterPrefix(final List<String> items, final String prefix) {
        final String p = prefix == null ? "" : prefix.toLowerCase();
        final ArrayList<String> out = new ArrayList<>();
        for (final String s : items)
            if (s.toLowerCase().startsWith(p)) out.add(s);
        return out;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
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
        } catch (final JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (final JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
        } catch (final Exception e) {
            sender.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }

    private void subForceRtpWithConfig(final CommandSender sender, final String playerName, final String configName) throws Exception {
        final Player playerToTp = sender.getServer().getPlayerExact(playerName);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(playerName));
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
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(playerName));
            return;
        }
        final World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), worldName);
        if ((destWorld) == null) {
            sender.sendMessage(Messages.WORLD_NOT_FOUND.format(worldName));
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

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        // Hide suggestions if sender lacks permission
        if (!sender.hasPermission("jakesrtp.others")) return List.of();

        if (args.length <= 1) {
            // Suggest player names (prefix-filtered)
            final String prefix = args.length == 0 ? "" : args[0];
            final List<String> players = new ArrayList<>();
            for (final Player player : getServer().getOnlinePlayers())
                players.add(player.getName());
            return filterPrefix(players, prefix);
        } else if (args.length == 2) {
            // Suggest flags -c or -w
            return filterPrefix(List.of("-c", "-w"), args[1]);
        } else if (args.length == 3) {
            if ("-c".equalsIgnoreCase(args[1])) {
                return filterPrefix(randomTeleporter.getRtpSettingsNames(), args[2]);
            } else if ("-w".equalsIgnoreCase(args[1])) {
                final List<String> worldNames = new ArrayList<>();
                for (final World world : getServer().getWorlds())
                    worldNames.add(world.getName());
                return filterPrefix(worldNames, args[2]);
            }
        }
        return List.of();
    }

}
