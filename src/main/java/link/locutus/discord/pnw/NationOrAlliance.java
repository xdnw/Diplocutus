package link.locutus.discord.pnw;

import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.MarkupUtil;

import java.util.Map;

public interface NationOrAlliance extends NationOrAllianceOrGuild {
    int getId();

    int getAlliance_id();

    String getMarkdownUrl();

    Map<ResourceType, Double> getStockpile(long updateThreshold);

    @Command(desc = "If this exists in game")
    boolean isValid();
}
