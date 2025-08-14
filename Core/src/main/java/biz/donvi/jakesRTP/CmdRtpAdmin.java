package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.ArgsChecker;
import biz.donvi.argsChecker.ArgsTester;
import biz.donvi.argsChecker.DynamicArgsMap;
import biz.donvi.jakesRTP.GeneralUtil.Pair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CmdRtpAdmin extends DynamicArgsMap implements TabExecutor {

    Map<String, Object> cmdMap;
    /**
     * The result of {@code getConfigNames()}, stored with an expiration time. If the data has not expired, the method
     * should return the value of {@code getConfigNamesResults}. If it has expired, it should compute the new value,
     * save
     * it here, then return it.
     */
    private Pair<Long, List<String>> getConfigNamesResults;

    public CmdRtpAdmin(final Map<String, Object> commandMap) {
        cmdMap = commandMap;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {

        final ArgsChecker argsChecker = new ArgsChecker(args);

        if (argsChecker.matches(true, "reload"))
            subReload(sender);
        else if (argsChecker.matches(true, "status"))
            sender.sendMessage("Incorrect usage. Try:\n/rtp-admin status <#static|name-of-config>");
        else if (argsChecker.matches(true, "status", null))
            subStatus(sender, argsChecker.getRemainingArgs());
        else if (argsChecker.matches(true, "reload-messages"))
            subReloadMessages(sender);
        else return false;
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return ArgsTester.nextCompleteInTree(args, cmdMap, this);
    }

    @Override
    public void getPotential(final String[] path) throws ResultAlreadySetException {
    }

    @Override
    public void getPotential(final String path) throws ResultAlreadySetException {
        //noinspection SwitchStatementWithTooFewBranches
        switch (path) {
            case "status":
                setResult(getConfigNames());
        }
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
        } else try {
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
