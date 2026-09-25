package biz.donvi.jakesRTP;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {
    private static void override(final Messages message, final String value) {
        Messages.addMap(new HashMap<>(Map.of(message.key, value)));
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean hasClick(final Component component, final String command) {
        return ClickEvent.runCommand(command).equals(component.clickEvent())
                || component.children().stream().anyMatch(child -> hasClick(child, command));
    }

    private static boolean hasColor(final Component component, final NamedTextColor color) {
        return color.equals(component.color()) || component.children().stream().anyMatch(child -> hasColor(child, color));
    }

    private static boolean hasHover(final Component component) {
        return component.hoverEvent() != null || component.children().stream().anyMatch(MessagesTest::hasHover);
    }

    @BeforeEach
    void loadDefaults() {
        Messages.setMap(new Yaml().load(getClass().getResourceAsStream("/translations/lang_en.yml")));
    }

    @ParameterizedTest
    @EnumSource(Messages.class)
    void everyMessageSupportsMiniMessageAndNamedPlaceholders(final Messages message) {
        override(message, "<red><click:run_command:'/rtp'><hover:show_text:'Try again'><value></hover></click></red>");
        final Component rendered = message.format(Placeholder.unparsed("value", "Don't parse <blue>this</blue>"));
        assertEquals("Don't parse <blue>this</blue>", plain(rendered));
        assertTrue(hasClick(rendered, "/rtp"));
        assertTrue(hasColor(rendered, NamedTextColor.RED));
        assertTrue(hasHover(rendered));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "es", "hu", "is", "it", "nl", "pl", "ru"})
    void bundledTranslationsHaveValidYamlAndResolveNamedPlaceholders(final String language) {
        final Map<String, String> translations = new Yaml().load(
                getClass().getResourceAsStream("/translations/lang_" + language + ".yml"));
        final TagResolver.Builder placeholders = TagResolver.builder();
        for (final String name : new String[]{"reason", "code", "profile", "profiles", "permission", "player", "world",
                "time", "seconds", "cost", "balance", "amount", "error", "days", "hours", "minutes", "command"})
            placeholders.resolver(Placeholder.unparsed(name, "VALUE"));
        placeholders.resolver(Placeholder.styling("confirm", ClickEvent.runCommand("/rtp confirm")));
        for (final Map.Entry<String, String> entry : translations.entrySet()) {
            assertFalse(entry.getValue().matches("(?s).*\\{\\d+}.*"), entry.getKey());
            final Component rendered = MiniMessage.miniMessage().deserialize(entry.getValue(), placeholders.build());
            assertFalse(plain(rendered).contains("<"), language + ": " + entry.getKey());
        }
    }

    @Test
    void defaultLanguageSettingsUseNamedCooldownPlaceholder() {
        final Map<String, String> settings = new Yaml().load(getClass().getResourceAsStream("/language-settings.yml"));
        settings.remove("language");
        Messages.addMap(settings);
        assertEquals("Whoops! You still have to wait 10 seconds", plain(Messages.NEED_WAIT_COOLDOWN.format(
                Placeholder.unparsed("time", "10 seconds"))));
    }

    @Test
    void nestedCooldownDurationKeepsColorsAndClickEvents() {
        override(Messages.READABLE_TIME_WORD_SECONDS,
                "<aqua><click:run_command:'/rtp'><amount> seconds</click></aqua>");
        final Component duration = GeneralUtil.readableTimeComponent(5000);
        final Component message = Messages.NEED_WAIT_COOLDOWN.format(Placeholder.component("time", duration));
        assertEquals("Need to wait for cooldown: 5 seconds", plain(message));
        assertTrue(hasColor(message, NamedTextColor.AQUA));
        assertTrue(hasClick(message, "/rtp"));
        assertEquals("5 seconds", GeneralUtil.readableTime(5000));
    }

    @Test
    void readableTimeResolvesAllUnitsAndOmitsZeroUnits() {
        assertEquals("1 days, 2 hours, 3 minutes, 4 seconds.", GeneralUtil.readableTime(93_784_000));
        assertEquals("2 hours, 24 minutes, 35 seconds.", GeneralUtil.readableTime(8_675_000));
    }

    @Test
    void exceptionReasonsKeepFormattingButLogsRemainPlainText() {
        override(Messages.NP_R_NO_RTPSETTINGS_NAME_FOR_PLAYER,
                "<red><click:run_command:'/rtp'>Missing <profile></click></red>");
        final JrtpBaseException error = new JrtpBaseException.NotPermittedException(
                Messages.NP_R_NO_RTPSETTINGS_NAME_FOR_PLAYER.format(Placeholder.unparsed("profile", "survival")));
        final Component wrapped = Messages.NP_GENERIC.format(Placeholder.component("reason", JrtpBaseException.userMessage(error)));
        assertEquals("Missing survival", error.getMessage());
        assertEquals("Could not RTP for reason: Missing survival", plain(wrapped));
        assertTrue(hasClick(wrapped, "/rtp"));
        assertTrue(hasColor(wrapped, NamedTextColor.RED));
    }

    @Test
    void externalErrorTextIsLiteralAndNullMessagesAreHandled() {
        assertEquals("<red>external</red>", plain(JrtpBaseException.userMessage(new Exception("<red>external</red>"))));
        assertEquals("Exception", plain(JrtpBaseException.userMessage(new Exception())));
    }

    @Test
    void namedPlaceholdersSupportReorderingAndLineBreaks() {
        override(Messages.ECON_NOT_ENOUGH_MONEY, "Balance: <balance><newline>Cost: <cost>\\n<balance>");
        assertEquals("Balance: 100 coins\nCost: 25 coins\n100 coins", plain(Messages.ECON_NOT_ENOUGH_MONEY.format(
                Placeholder.unparsed("cost", "25 coins"), Placeholder.unparsed("balance", "100 coins"))));
    }
}
