package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.GeneralUtil;
import biz.donvi.jakesRTP.JakesRtpPlugin;
import biz.donvi.jakesRTP.Messages;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;


public class CmdRtpAdmin {

    public boolean execute(final CommandSender sender, final String[] args) {
        if (args.length == 0) return false;
        final String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                subReload(sender);
                return true;
            case "status":
                subStatus(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
                return true;
            case "reload-messages":
                subReloadMessages(sender);
                return true;
            default:
                return false;
        }
    }

    private void subReload(final CommandSender sender) {
        JakesRtpPlugin.plugin.reloadCommands();
        JakesRtpPlugin.plugin.loadRandomTeleporter();
        JakesRtpPlugin.plugin.loadLocationCacheFiller();
        sender.sendMessage(Messages.RELOADED.format());
    }

    private void subReloadMessages(final CommandSender sender) {
        JakesRtpPlugin.plugin.loadMessageMap();
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
                    Messages.SETTINGS_NOT_FOUND.format(
                            Placeholder.unparsed("profile", args[0]),
                            Placeholder.unparsed("profiles", GeneralUtil.listText(theRandomTeleporter.getRtpSettingsNames())))
            );
        }
    }
}
