package link.locutus.discord.event.grantrequest;

import link.locutus.discord.api.types.tx.DBGrantRequest;

public class GrantRequestUpdateEvent extends GrantRequestChangeEvent {
    public GrantRequestUpdateEvent(DBGrantRequest previous, DBGrantRequest current) {
        super(previous, current);
    }
}
