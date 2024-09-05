package link.locutus.discord.event.grant;

import link.locutus.discord.api.types.tx.GrantTransfer;
import link.locutus.discord.event.grant.GrantChangeEvent;

public class GrantCreateEvent extends GrantChangeEvent {
    public GrantCreateEvent(GrantTransfer current) {
        super(null, current);
    }
}
