package link.locutus.discord.db.guild;

import link.locutus.discord.commands.manager.v2.binding.Key;

public abstract class GuildIntegerSetting extends GuildSetting<Integer> {
    public GuildIntegerSetting(GuildSettingCategory category) {
        super(category, Key.of(Integer.class));
    }

    @Override
    public final String toString(Integer value) {
        return value.toString();
    }
}
