package link.locutus.discord._main;

import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.commands.manager.CommandManager;
import link.locutus.discord.commands.manager.v2.impl.SlashCommandManager;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.*;
import net.dv8tion.jda.api.JDA;

import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class FinalizedLoader implements ILoader {

    private final SlashCommandManager slashCommandManager;
    private final JDA jda;
    private final DiscordDB discordDB;
    private final NationDB nationDB;
    private final WarDB warDb;
    private final BankDB bankDb;
    private final CommandManager commandManager;
    private final DnsApi apiV3;

    public FinalizedLoader(PreLoader loader) {
        this.slashCommandManager = loader.getSlashCommandManager();
        this.jda = loader.getJda();
        this.discordDB = loader.getDiscordDB();
        this.nationDB = loader.getNationDB();
        this.warDb = loader.getWarDB();
        this.bankDb = loader.getBankDB();
        this.commandManager = loader.getCommandManager();
        this.apiV3 = loader.getApiV3();
    }

    @Override
    public SlashCommandManager getSlashCommandManager() {
        return slashCommandManager;
    }

    @Override
    public ILoader resolveFully(long timeout) {
        return this;
    }

    @Override
    public void initialize() {
        // Do nothing
    }

    @Override
    public JDA getJda() {
        return jda;
    }

    @Override
    public DiscordDB getDiscordDB() {
        return discordDB;
    }

    @Override
    public NationDB getNationDB() {
        return nationDB;
    }

    @Override
    public WarDB getWarDB() {
        return warDb;
    }

    @Override
    public BankDB getBankDB() {
        return bankDb;
    }

    @Override
    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public DnsApi getApiV3() {
        return apiV3;
    }


}
