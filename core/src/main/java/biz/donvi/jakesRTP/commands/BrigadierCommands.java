package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import biz.donvi.jakesRTP.RandomTeleporter;
import biz.donvi.jakesRTP.RtpProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BrigadierCommands {
    private BrigadierCommands() {
    }

    public static void register(final JakesRtpPlugin plugin, final Commands registrar) {
        final String confirm = confirmationLiteral(plugin);

        final Map<String, String> configuredAliases = new LinkedHashMap<>();
        final Map<String, String> shortcuts = loadShortcuts(plugin, registrar, confirm, configuredAliases);

        final var rtpCommand = Commands.literal("rtp")
                .requires(source -> isPlayerWithPermission(source, "jakesrtp.use"))
                .executes(ctx -> rtp(plugin, ctx, new String[0], null))
                .then(Commands.literal(confirm)
                        .executes(ctx -> rtp(plugin, ctx, new String[]{confirm}, null)))
                .then(Commands.argument("profile", StringArgumentType.word())
                        .requires(source -> isPlayerWithPermission(source, "jakesrtp.usebyname"))
                        .suggests((ctx, builder) -> suggestProfiles(plugin, ctx, builder, true, confirm))
                        .executes(ctx -> rtp(plugin, ctx,
                                new String[]{StringArgumentType.getString(ctx, "profile")}, null))
                        .then(Commands.literal(confirm)
                                .executes(ctx -> rtp(plugin, ctx,
                                        new String[]{StringArgumentType.getString(ctx, "profile"), confirm}, null))));

        for (final Map.Entry<String, String> alias : configuredAliases.entrySet()) {
            final String profileName = alias.getValue();
            final String aliasName = alias.getKey();
            rtpCommand.then(Commands.literal(aliasName)
                    .requires(source -> canUseShortcut(plugin, source, profileName))
                    .executes(ctx -> rtp(plugin, ctx, new String[0], profileName, aliasName))
                    .then(Commands.literal(confirm)
                            .executes(ctx -> rtp(plugin, ctx, new String[]{confirm}, profileName, aliasName))));
        }

        registrar.register(rtpCommand.build(), "Teleports you to a random location", List.of("wild"));

        registrar.register(Commands.literal("forcertp")
                .requires(source -> source.getSender().hasPermission("jakesrtp.others"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (final Player player : plugin.getServer().getOnlinePlayers())
                                suggest(builder, player.getName());
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("-c")
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestProfiles(plugin, ctx, builder, false, confirm))
                                        .executes(ctx -> force(plugin, ctx, "-c",
                                                StringArgumentType.getString(ctx, "profile")))))
                        .then(Commands.literal("-w")
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            plugin.getServer().getWorlds().forEach(world -> suggest(builder, world.getName()));
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> force(plugin, ctx, "-w",
                                                StringArgumentType.getString(ctx, "world"))))))
                .build(), "Force a random teleport for an online player");

        registrar.register(Commands.literal("rtp-admin")
                .requires(source -> source.getSender().hasPermission("jakesrtp.admin"))
                .then(Commands.literal("reload")
                        .executes(ctx -> admin(ctx, new String[]{"reload"})))
                .then(Commands.literal("reload-messages")
                        .executes(ctx -> admin(ctx, new String[]{"reload-messages"})))
                .then(Commands.literal("status")
                        .executes(ctx -> admin(ctx, new String[]{"status"}))
                        .then(Commands.argument("profile", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    suggest(builder, "#static");
                                    return suggestProfiles(plugin, ctx, builder, false, confirm);
                                })
                                .executes(ctx -> admin(ctx, new String[]{
                                        "status", StringArgumentType.getString(ctx, "profile")}))))
                .build(), "Administration commands for random teleport");

        for (final Map.Entry<String, String> shortcut : shortcuts.entrySet()) {
            final String profileName = shortcut.getValue();
            registrar.register(Commands.literal(shortcut.getKey())
                    .requires(source -> canUseShortcut(plugin, source, profileName))
                    .executes(ctx -> rtp(plugin, ctx, new String[0], profileName))
                    .then(Commands.literal(confirm)
                            .executes(ctx -> rtp(plugin, ctx, new String[]{confirm}, profileName)))
                    .build(), "Random teleport using profile " + profileName);
        }
    }

    private static Map<String, String> loadShortcuts(final JakesRtpPlugin plugin, final Commands registrar,
                                                     final String confirm, final Map<String, String> configuredAliases) {
        final Map<String, String> shortcuts = new LinkedHashMap<>();
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter == null) return shortcuts;

        final Set<String> reserved = new HashSet<>(List.of("rtp", "wild", "forcertp", "rtp-admin",
                "jrtpdebug", confirm.toLowerCase(Locale.ROOT)));
        final List<RtpProfile> profiles = teleporter.getRtpSettings();

        for (final RtpProfile profile : profiles) {
            if (profile.commandEnabled)
                addShortcut(plugin, registrar, reserved, shortcuts, profile.name, profile.name);
        }
        // Profile names remain reserved even when they have no command shortcut.
        for (final RtpProfile profile : profiles) reserved.add(profile.name.toLowerCase(Locale.ROOT));

        final ConfigurationSection aliases = plugin.getConfig().getConfigurationSection("aliases");
        if (aliases == null) return shortcuts;

        for (final String profileName : aliases.getKeys(false)) {
            final RtpProfile profile = profiles.stream()
                    .filter(candidate -> candidate.name.equalsIgnoreCase(profileName))
                    .findFirst().orElse(null);
            if (profile == null || !profile.commandEnabled) {
                plugin.getLogger().warning("Skipping aliases for missing or disabled RTP profile: " + profileName);
                continue;
            }
            if (!aliases.isList(profileName)) {
                plugin.getLogger().warning("RTP aliases must be a list for profile: " + profileName);
                continue;
            }
            for (final String name : aliases.getStringList(profileName)) {
                if (addShortcut(plugin, registrar, reserved, shortcuts, name, profile.name))
                    configuredAliases.put(name.toLowerCase(Locale.ROOT), profile.name);
            }
        }
        return shortcuts;
    }

    private static boolean addShortcut(final JakesRtpPlugin plugin, final Commands registrar,
                                       final Set<String> reserved, final Map<String, String> shortcuts,
                                       final String name, final String profileName) {
        if (name == null || !name.matches("[a-zA-Z0-9_-]+")) {
            plugin.getLogger().warning("Skipping invalid RTP shortcut: " + name);
            return false;
        }
        final String label = name.toLowerCase(Locale.ROOT);
        if (!reserved.add(label)) {
            plugin.getLogger().warning("Skipping reserved or duplicate RTP shortcut: " + name);
            return false;
        }
        if (registrar.getDispatcher().getRoot().getChild(label) != null
                || plugin.getServer().getCommandMap().getCommand(label) != null) {
            plugin.getLogger().warning("Skipping RTP shortcut already owned by another command: " + label);
            return false;
        }
        shortcuts.put(label, profileName);
        return true;
    }

    private static boolean canUseShortcut(final JakesRtpPlugin plugin, final CommandSourceStack source,
                                          final String profileName) {
        if (!(source.getSender() instanceof Player player) || !player.hasPermission("jakesrtp.use")) return false;
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        return teleporter != null && teleporter.getRtpSettingsNamesForPlayer(player).stream()
                .anyMatch(name -> name.equalsIgnoreCase(profileName));
    }

    private static String confirmationLiteral(final JakesRtpPlugin plugin) {
        final String configured = plugin.getConfig().getString("rtp-confirmation.subcommand", "confirm").trim();
        return configured.matches("[a-zA-Z0-9_-]+") ? configured.toLowerCase(Locale.ROOT) : "confirm";
    }

    private static boolean isPlayerWithPermission(final CommandSourceStack source, final String permission) {
        return source.getSender() instanceof Player && source.getSender().hasPermission(permission);
    }

    private static String invokedLabel(final CommandContext<CommandSourceStack> ctx) {
        final String first = ctx.getInput().split(" ", 2)[0];
        return first.startsWith("/") ? first.substring(1) : first;
    }

    private static int rtp(final JakesRtpPlugin plugin, final CommandContext<CommandSourceStack> ctx,
                           final String[] args, final String shortcutProfile) {
        return rtp(plugin, ctx, args, shortcutProfile, null);
    }

    private static int rtp(final JakesRtpPlugin plugin, final CommandContext<CommandSourceStack> ctx,
                           final String[] args, final String shortcutProfile, final String nestedAlias) {
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter != null) {
            final String label = invokedLabel(ctx) + (nestedAlias == null ? "" : " " + nestedAlias);
            new CmdRtp(teleporter).execute(ctx.getSource().getSender(), label, args, shortcutProfile);
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int force(final JakesRtpPlugin plugin, final CommandContext<CommandSourceStack> ctx,
                             final String flag, final String destination) {
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter != null) new CmdForceRtp(teleporter).execute(ctx.getSource().getSender(), new String[]{
                StringArgumentType.getString(ctx, "player"), flag, destination});
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int admin(final CommandContext<CommandSourceStack> ctx, final String[] args) {
        new CmdRtpAdmin().execute(ctx.getSource().getSender(), args);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestProfiles(
            final JakesRtpPlugin plugin, final CommandContext<CommandSourceStack> ctx,
            final SuggestionsBuilder builder, final boolean forPlayer, final String confirm) {
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter != null) {
            final CommandSender sender = ctx.getSource().getSender();
            final List<String> names = forPlayer && sender instanceof Player player
                    ? teleporter.getRtpSettingsNamesForPlayer(player)
                    : teleporter.getRtpSettingsNames();
            for (final String name : names) {
                if (!forPlayer || !name.equalsIgnoreCase(confirm)) suggest(builder, name);
            }
        }
        return builder.buildFuture();
    }

    private static void suggest(final SuggestionsBuilder builder, final String value) {
        if (value.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) builder.suggest(value);
    }
}
