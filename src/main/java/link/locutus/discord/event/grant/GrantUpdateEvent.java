package link.locutus.discord.event.grant;

import link.locutus.discord.api.types.tx.GrantTransfer;
import link.locutus.discord.event.grant.GrantChangeEvent;

public class GrantUpdateEvent extends GrantChangeEvent {
    public GrantUpdateEvent(GrantTransfer previous, GrantTransfer current) {
        super(previous, current);
    }
}
