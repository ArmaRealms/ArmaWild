package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.Messages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

import java.util.List;

final class RtpConfirmationDialog {
    private RtpConfirmationDialog() {
    }

    static void show(final Player player, final String profile, final String cost, final Consumer<Player> onConfirm) {
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Messages.ECON_CONFIRM_DIALOG_TITLE.format())
                        .canCloseWithEscape(true)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(
                                Messages.ECON_CONFIRM_DIALOG_BODY.format(
                                        Placeholder.unparsed("profile", profile),
                                        Placeholder.unparsed("value", cost),
                                        Placeholder.unparsed("cost", cost)))))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(Messages.ECON_CONFIRM_DIALOG_ACCEPT.format(), null, 150,
                                DialogAction.staticAction(ClickEvent.callback(audience -> {
                                    if (audience instanceof final Player teleporter
                                            && teleporter.getUniqueId().equals(player.getUniqueId())) {
                                        onConfirm.accept(teleporter);
                                    }
                                }))),
                        ActionButton.create(Messages.ECON_CONFIRM_DIALOG_CANCEL.format(), null, 150, null)))));
    }
}
