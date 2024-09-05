package link.locutus.discord.event.grant;

import link.locutus.discord.api.types.tx.GrantTransfer;
import link.locutus.discord.event.grant.GrantChangeEvent;

public class GrantDeleteEvent extends GrantChangeEvent {
    public GrantDeleteEvent(GrantTransfer previous) {
        super(previous, null);
    }
}
