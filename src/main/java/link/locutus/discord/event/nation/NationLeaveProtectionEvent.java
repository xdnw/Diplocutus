package link.locutus.discord.event.nation;

import link.locutus.discord.db.entities.DBNation;

public class NationLeaveProtectionEvent extends NationChangeEvent2{
    public NationLeaveProtectionEvent(DBNation original, DBNation changed) {
        super(original, changed);
    }
}
