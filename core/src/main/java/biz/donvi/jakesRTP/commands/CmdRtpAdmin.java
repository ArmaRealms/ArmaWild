package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.GeneralUtil;
import biz.donvi.jakesRTP.GeneralUtil.Pair;
import biz.donvi.jakesRTP.JakesRtpPlugin;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdRtpAdmin implements TabExecutor {

    /**
     * The result of {@code getConfigNames()}, stored with an expiration time. If the data has not expired, the method
     * should return the value of {@code getConfigNamesResults}. If it has expired, it should compute the new value,
     * save
     * it here, then return it.
     */
    private Pair<Long, List<String>> getConfigNamesResults;

    private static List<String> filterPrefix(final List<String> items, final String prefix) {
        final String p = prefix == null ? "" : prefix.toLowerCase();
        final ArrayList<String> out = new ArrayList<>();
        for (final String s : items)
            if (s.toLowerCase().startsWith(p)) out.add(s);
        return out;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) return false;
        final String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                subReload(sender);
                return true;
            case "status":
                subStatus(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            case "reload-messages":
                subReloadMessages(sender);
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        // Only ops/admins should see suggestions
        if (!sender.hasPermission("jakesrtp.admin")) return List.of();
        if (args.length <= 1) {
            final String prefix = args.length == 0 ? "" : args[0];
            return filterPrefix(List.of("reload", "status", "reload-messages"), prefix);
        } else if (args.length == 2 && "status".equalsIgnoreCase(args[0])) {
            // Suggest config names and special token
            final ArrayList<String> items = new ArrayList<>(getConfigNames());
            items.add("#static");
            return filterPrefix(items, args[1]);
        }
        return List.of();
    }

    private void subReload(final CommandSender sender) {
        JakesRtpPlugin.plugin.reloadCommands();
        JakesRtpPlugin.plugin.loadRandomTeleporter();
        JakesRtpPlugin.plugin.loadLocationCacheFiller();
        sender.sendMessage("Reloaded.");
    }

    private void subReloadMessages(final CommandSender sender) {
        JakesRtpPlugin.plugin.loadMessageMap();
    }

    /**
     * Gets a list of the RTP config names. Because this method is expected to be called multiple times per second,
     * yet returns data that changed infrequently, it temporarily stores the resulting list and only rechecks the
     * after 1000 milliseconds.
     *
     * @return A list of the names of the RTP configs.
     */
    private List<String> getConfigNames() {
        if (getConfigNamesResults == null || getConfigNamesResults.key < System.currentTimeMillis()) {
            final ArrayList<String> settingsNames = new ArrayList<>();
            for (final RtpProfile settings : JakesRtpPlugin.plugin.getRandomTeleporter().getRtpSettings()) {
                settingsNames.add(settings.name);
            }
            getConfigNamesResults = new Pair<>(System.currentTimeMillis() + 1000, settingsNames);
        }
        return getConfigNamesResults.value;
    }

    private void subStatus(final CommandSender sender, final String[] args) {
        final RandomTeleporter theRandomTeleporter = JakesRtpPlugin.plugin.getRandomTeleporter();
        if (args.length == 1 && args[0].equalsIgnoreCase("#static")) {
            final StringBuilder msg = new StringBuilder();
            for (final String line : theRandomTeleporter.infoStringAll(true))
                msg.append(line).append('\n');
            sender.sendMessage(msg.toString());
        } else if (args.length >= 1) try {
            final RtpProfile settings = theRandomTeleporter.getRtpSettingsByName(args[0]);
            for (final String message : settings.infoStringAll(true, true))
                sender.sendMessage(message);
        } catch (final Exception e) {
            sender.sendMessage(
                    "Could not find any settings with the name " + args[0] + ", " +
                            GeneralUtil.listText(theRandomTeleporter.getRtpSettingsNames())
            );
        }
    }
}
