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
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BrigadierCommands {
    private BrigadierCommands() {
    }

    public static void register(final JakesRtpPlugin plugin, final Commands registrar) {
        final String confirm = confirmationLiteral(plugin);

        registrar.register(Commands.literal("rtp")
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
                                        new String[]{StringArgumentType.getString(ctx, "profile"), confirm}, null))))
                .build(), "Teleports you to a random location", List.of("wild"));

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
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    suggest(builder, "#static");
                                    return suggestProfiles(plugin, ctx, builder, false, confirm);
                                })
                                .executes(ctx -> admin(ctx, new String[]{
                                        "status", StringArgumentType.getString(ctx, "profile")}))))
                .build(), "Administration commands for random teleport");

        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter == null) return;

        final Set<String> reserved = new HashSet<>(List.of("rtp", "wild", "forcertp", "rtp-admin",
                "jrtpdebug", confirm.toLowerCase(Locale.ROOT)));
        for (final RtpProfile profile : teleporter.getRtpSettings()) {
            if (!profile.commandEnabled) continue;
            final String profileName = profile.name;
            final String alias = profileName.toLowerCase(Locale.ROOT);
            if (!alias.matches("[a-z0-9_-]+") || !reserved.add(alias)) {
                plugin.getLogger().warning("Skipping invalid or reserved RTP shortcut: " + profileName);
                continue;
            }
            registrar.register(Commands.literal(alias)
                    .requires(source -> source.getSender() instanceof Player player
                            && player.hasPermission("jakesrtp.use")
                            && plugin.getRandomTeleporter() != null
                            && plugin.getRandomTeleporter().getRtpSettingsNamesForPlayer(player).stream()
                            .anyMatch(name -> name.equalsIgnoreCase(profileName)))
                    .executes(ctx -> rtp(plugin, ctx, new String[0], profileName))
                    .then(Commands.literal(confirm)
                            .executes(ctx -> rtp(plugin, ctx, new String[]{confirm}, profileName)))
                    .build(), "Random teleport using profile " + profileName);
        }
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
        final RandomTeleporter teleporter = plugin.getRandomTeleporter();
        if (teleporter != null)
            new CmdRtp(teleporter).execute(ctx.getSource().getSender(), invokedLabel(ctx), args, shortcutProfile);
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
