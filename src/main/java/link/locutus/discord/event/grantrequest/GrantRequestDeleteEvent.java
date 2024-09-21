package link.locutus.discord.event.grantrequest;

import link.locutus.discord.api.types.tx.DBGrantRequest;

public class GrantRequestDeleteEvent extends GrantRequestChangeEvent {
    public GrantRequestDeleteEvent(DBGrantRequest previous) {
        super(previous, null);
    }
}
