package link.locutus.discord.event.alliance;

import link.locutus.discord.Locutus;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class AllianceUpdateEvent extends AllianceChangeEvent {
    public AllianceUpdateEvent(DBAlliance previous, DBAlliance current) {
        super(previous, current);
    }
}