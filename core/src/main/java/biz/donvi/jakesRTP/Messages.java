package biz.donvi.jakesRTP;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

import static biz.donvi.jakesRTP.GeneralUtil.replaceLegacyColors;
import static biz.donvi.jakesRTP.GeneralUtil.replaceNewColors;
import static biz.donvi.jakesRTP.GeneralUtil.replaceWrittenLineBreaks;
import static java.util.logging.Level.WARNING;

public enum Messages {
    NP_GENERIC("not-permitted-generic"),
    NP_UNEXPECTED_EXCEPTION("not-permitted-major-error"),
    NP_R_NOT_ENABLED("not-enabled-in-this-world"),
    NP_R_NO_RTPSETTINGS_NAME("no-settings-found-with-name"),
    NP_R_NO_RTPSETTINGS_NAME_FOR_PLAYER("no-settings-found-with-name-for-player"),
    NP_R_TOO_MANY_FAILED_ATTEMPTS("too-many-failed-attempts"),
    NP_NO_PERMISSION("no-permission"),
    PLAYER_NOT_FOUND("player-not-found"),
    WORLD_NOT_FOUND("world-not-found"),
    RTPSETTINGS_NO_CONTAIN_WORLD("rtp-settings-no-contain-world"),
    RTPSETTINGS_MUST_USE_WORLD("rtp-settings-must-use-world"),
    NEED_WAIT_COOLDOWN("need-to-wait-for-cooldown"),
    WARMUP_TELEPORTING_IN_X("teleporting-in-x-seconds"),
    WARMUP_CANCEL_BECAUSE_MOVE("moved-during-warmup"),
    WARMUP_RTP_ALREADY_CALLED("rtp-called-while-in-warmup"),
    ECON_NOT_ENOUGH_MONEY("not-enough-money"),
    ECON_CONFIRM_RTP("confirm-paid-rtp"),
    ECON_NO_LONGER_ENOUGH_MONEY("no-longer-enough-money"),
    ECON_YOU_WERE_CHARGED_X("you-were-charged-x"),
    ECON_ERROR("economy-error"),
    COOLDOWN_OVER("cooldown-over"),
    READABLE_TIME("readable-time"),
    READABLE_TIME_WORD_DAYS("readable-time-word-days"),
    READABLE_TIME_WORD_HOURS("readable-time-word-hours"),
    READABLE_TIME_WORD_MINUTES("readable-time-word-minutes"),
    READABLE_TIME_WORD_SECONDS("readable-time-word-seconds");

    private static final String[] mappedValues = new String[Messages.values().length];
    final String key;

    Messages(final String key) {
        this.key = key;
    }

    static void setMap(final Map<String, String> newMap) {
        final ArrayList<String> emptyValues = new ArrayList<>();
        for (final Messages m : Messages.values()) {
            mappedValues[m.ordinal()] = newMap.remove(m.key);
            if (mappedValues[m.ordinal()] == null) emptyValues.add(m.name() + " ~ " + m.key);
        }
        if (!newMap.isEmpty()) {
            JakesRtpPlugin.log(WARNING, "More mappings were given than expected. Extra keys:");
            for (final String extraValue : newMap.keySet()) JakesRtpPlugin.log(WARNING, extraValue);
        }
        if (!emptyValues.isEmpty()) {
            JakesRtpPlugin.log(WARNING, "Some messages could not be assigned values. Missing keys:");
            for (final String missingValue : emptyValues) JakesRtpPlugin.log(WARNING, missingValue);
        }
    }

    static int addMap(final Map<String, String> newMap) {
        int numValuesAdded = 0;
        for (final Messages m : Messages.values()) {
            final String value = newMap.remove(m.key);
            if (value != null) {
                mappedValues[m.ordinal()] = value;
                numValuesAdded++;
            }
        }
        if (!newMap.isEmpty()) {
            JakesRtpPlugin.log(WARNING, "Some extra keys were found:");
            for (final String extraValue : newMap.keySet()) JakesRtpPlugin.log(WARNING, extraValue);
        }
        return numValuesAdded;
    }

    private static String reformat(final String s) {
        return s == null ? null : replaceNewColors(replaceLegacyColors(replaceWrittenLineBreaks(s)));
    }

    String raw() {
        return reformat(mappedValues[this.ordinal()]);
    }

    public @NotNull String format(final Object... args) {
        return MessageFormat.format(raw(), args);
    }

    /** Parse MiniMessage directly, preserving quotes in click/hover tags and literal placeholder values. */
    public @NotNull Component formatMiniMessage(final TagResolver... placeholders) {
        return MiniMessage.miniMessage().deserialize(
                replaceWrittenLineBreaks(mappedValues[this.ordinal()]), placeholders);
    }
}
