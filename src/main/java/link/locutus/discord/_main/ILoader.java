package link.locutus.discord._main;

import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.commands.manager.CommandManager;
import link.locutus.discord.commands.manager.v2.impl.SlashCommandManager;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.*;
import net.dv8tion.jda.api.JDA;

public interface ILoader {
    ILoader resolveFully(long timeout);
    void initialize();

    default int getNationId() {
        return Settings.INSTANCE.NATION_ID;
    }

    default long getAdminUserId() {
        return Settings.INSTANCE.ADMIN_USER_ID;
    }
    JDA getJda();
    SlashCommandManager getSlashCommandManager();
    CommandManager getCommandManager();

    NationDB getNationDB();
    DiscordDB getDiscordDB();
    WarDB getWarDB();
    BankDB getBankDB();

    DnsApi getApiV3();

    default String printStacktrace() {
        return "";
    }
}
