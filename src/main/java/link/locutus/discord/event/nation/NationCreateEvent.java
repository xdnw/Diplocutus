package link.locutus.discord.event.nation;

import link.locutus.discord.Locutus;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;

public class NationCreateEvent extends NationChangeEvent2 {
    public NationCreateEvent(DBNation original, DBNation changed) {
        super(original, changed);
        setTime(changed.getDate());
    }
}
