package com.fiesta.chatarchive;

import net.ess3.api.events.PrivateMessageSentEvent;
import net.essentialsx.api.v2.events.HelpopMessageSendEvent;
import net.essentialsx.api.v2.events.UserMailEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class EssentialsArchiveListener implements Listener {
    private final ChatArchivePlugin archive;

    public EssentialsArchiveListener(ChatArchivePlugin archive) {
        this.archive = archive;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrivate(PrivateMessageSentEvent event) {
        if (event.getResponse().isSuccess()) {
            archive.record("essentials_private", event.getSender().getName(),
                    event.getRecipient().getName(), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMail(UserMailEvent event) {
        archive.record("essentials_mail", event.getMessage().getSenderUsername(),
                event.getRecipient().getName(), event.getMessage().getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHelpop(HelpopMessageSendEvent event) {
        archive.record("essentials_helpop", event.getSender().getName(), null, event.getMessage());
    }
}
