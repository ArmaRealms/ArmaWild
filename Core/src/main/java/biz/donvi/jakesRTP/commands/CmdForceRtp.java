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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class CmdForceRtp implements TabExecutor {

    private final RandomTeleporter randomTeleporter;
    Map<String, Object> cmdMap;

    public CmdForceRtp(final RandomTeleporter randomTeleporter, final Map<String, Object> commandMap) {
        this.randomTeleporter = randomTeleporter;
        this.cmdMap = commandMap;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final List<String> argsList = new ArrayList<>(Arrays.asList(args));
        try {
            if (argsList.contains("-c"))
                subForceRtpWithConfig(sender, args);
            else if (argsList.contains("-w"))
                subForceRtpWithWorld(sender, args);
            else return false;
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

    private void subForceRtpWithConfig(final CommandSender sender, final String[] args) throws Exception {
        final Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(args[0]));
            return;
        }
        final RtpProfile rtpProfile = randomTeleporter.getRtpSettingsByName(args[1]);

        // ↑ Check step | Teleport step ↓

        new RandomTeleportAction(
                randomTeleporter, rtpProfile, playerToTp.getLocation(), true, true,
                randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        ).teleportAsync(playerToTp);
    }

    private void subForceRtpWithWorld(final CommandSender sender, final String[] args) throws Exception {
        final Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(args[0]));
            return;
        }
        final World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), args[1]);
        if ((destWorld) == null) {
            sender.sendMessage(Messages.WORLD_NOT_FOUND.format(args[1]));
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
        if (args.length == 0) {
            final List<String> players = new ArrayList<>();
            for (final Player player : getServer().getOnlinePlayers())
                players.add(player.getName());
            return players;
        } else if (args.length == 2) {
            switch (args[1]) {
                case "-c":
                    return randomTeleporter.getRtpSettingsNames();
                case "-w":
                    final List<String> worldNames = new ArrayList<>();
                    for (final World world : getServer().getWorlds())
                        worldNames.add(world.getName());
                    return worldNames;
            }
        }
        return List.of();
    }

}
