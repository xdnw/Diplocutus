package link.locutus.discord.event.nation;

import link.locutus.discord.db.entities.DBNation;

public class NationChangePositionEvent extends NationChangeEvent2 {
    public NationChangePositionEvent(DBNation original, DBNation changed) {
        super(original, changed);
    }
}
