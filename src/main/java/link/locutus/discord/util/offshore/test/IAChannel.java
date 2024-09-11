package link.locutus.discord.util.offshore.test;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.RateLimitUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.task.ia.IACheckup;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IAChannel {
    private final DBNation nation;
    private final GuildDB db;
    private final Category category;
    private final TextChannel channel;

    public IAChannel(DBNation nation, GuildDB db, Category category, TextChannel channel) {
        this.nation = nation;
        this.db = db;
        this.category = category;
        this.channel = channel;
    }

    public DBNation getNation() {
        if (nation == null) {
            String[] split = channel.getName().split("-");
            if (MathMan.isInteger(split[split.length - 1])) {
                Locutus.imp().getNationDB().getNation(Integer.parseInt(split[split.length - 1]));
            }
        }
        return nation;
    }

    public void updatePerms() {
        User user = nation.getUser();
        if (user == null) return;
        Member member = db.getGuild().getMember(user);
        if (member == null) return;
        RateLimitUtil.complete(channel.upsertPermissionOverride(member).grant(Permission.VIEW_CHANNEL));

        String expected = DiscordUtil.toDiscordChannelString(nation.getNation()) + "-" + nation.getNation_id();
        String name = channel.getName();
        if (!name.equalsIgnoreCase(expected)) {
            RateLimitUtil.queue(channel.getManager().setName(expected));
        }
    }

    public TextChannel getChannel() {
        return channel;
    }

    public void update(Map<IACheckup.AuditType, Map.Entry<Object, String>> audits) {
        Set<String> emojis = new LinkedHashSet<>();
        if (audits == null) {
            if (nation.isVacation()) {
                emojis.add("\uD83C\uDFD6\ufe0f");
            }
            if (nation.active_m() > 2440) {
                emojis.add("\uD83E\uDEA6");
            }
            User user = nation.getUser();
            Guild guild = db.getGuild();
            if (user == null || guild.getMember(user) == null) {
                emojis.add("\uD83D\uDCDB");
            }
        } else {
            for (Map.Entry<IACheckup.AuditType, Map.Entry<Object, String>> entry : audits.entrySet()) {
                IACheckup.AuditType type = entry.getKey();
                Map.Entry<Object, String> result = entry.getValue();
                emojis.add(type.emoji);
            }
            String cmd = CM.audit.run.cmd.nationList(nation.getNation_id() + "").toCommandArgs();
            IACheckup.createEmbed(new DiscordChannelIO(channel), cmd, nation, audits, 0);
        }
    }
}
