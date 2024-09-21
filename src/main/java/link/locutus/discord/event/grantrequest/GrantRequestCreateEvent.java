package link.locutus.discord.event.grantrequest;

import link.locutus.discord.api.types.tx.DBGrantRequest;

public class GrantRequestCreateEvent extends GrantRequestChangeEvent {
    public GrantRequestCreateEvent(DBGrantRequest current) {
        super(null, current);
    }
}
