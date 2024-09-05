package link.locutus.discord.pnw;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface NationOrAllianceOrGuild {

    static NationOrAllianceOrGuild create(long id, int type) {
        switch (type) {
            case 1:
                DBNation nation = DBNation.getById((int) id);
                if (nation == null) {
                    nation = new DBNation();
                    nation.setNation_id((int) id);
                }
                return nation;
            case 2:
                return DBAlliance.getOrCreate((int) id);
            case 3:
                return (NationOrAllianceOrGuild) Locutus.imp().getDiscordApi().getGuildById(id);
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    default int getId() {
        return (int) getIdLong();
    }

    default long getIdLong() {
        return getId();
    }

    default int getReceiverType() {
        if (isNation()) return 1;
        if (isAlliance()) return 2;
        if (isGuild()) return 3;
        throw new IllegalArgumentException("Invalid state: " + this);
    }

    default String getQualifiedId() {
        return getTypePrefix() + ":" + getIdLong();
    }

    default String getTypePrefix() {
        if (isNation()) return "nation";
        if (isAlliance()) return "aa";
        if (isGuild()) return "guild";
        throw new IllegalArgumentException("Invalid state: " + this);
    }

    int getAlliance_id();

    String getName();

    default Map.Entry<Long, Integer> getTransferIdAndType() {
        long sender_id;
        int sender_type;
        if (isGuild()) {
            GuildDB db = asGuild();
            if (db.hasAlliance()) {
                throw new IllegalStateException("Alliance Guilds cannot be used as sender. Specify the alliance instead: " + db.getGuild());
            }
            sender_id = db.getGuild().getIdLong();
            sender_type = 3;
        } else if (isAlliance()) {
            sender_id = getIdLong();
            sender_type = 2;
        } else if (isNation()) {
            sender_id = getId();
            sender_type = 1;
        } else throw new IllegalArgumentException("Invalid receiver: " + this);
        return new AbstractMap.SimpleEntry<>(sender_id, sender_type);
    }

    default Set<DBNation> getMemberDBNations() {
        Set<DBNation> nations = new HashSet<>();

        if (isNation()) nations.add(asNation());
        else if (isGuild()) {
            GuildDB db = asGuild();
            AllianceList aaList = db.getAllianceList();
            if (aaList != null && !aaList.isEmpty()) {
                nations.addAll(aaList.getNations(true, 0, true));
            } else {
                Guild guild = db.getGuild();
                Role role = Roles.MEMBER.toRole(guild);
                if (role != null) {
                    for (Member member : guild.getMembersWithRoles(role)) {
                        DBNation nation = DiscordUtil.getNation(member.getUser());
                        if (nation != null) {
                            nations.add(nation);
                        }
                    }
                }
            }
        }
        else if (isAlliance()) {
            nations.addAll(asAlliance().getNations(true, 0, true));
        } else {
            throw new IllegalArgumentException("Unknwon type " + getIdLong());
        }
        return nations;
    }

    default Set<DBNation> getDBNations() {
        Set<DBNation> nations = new HashSet<>();

        if (isNation()) nations.add(asNation());
        else if (isGuild()) {
            GuildDB db = asGuild();
            Set<Integer> ids = db.getAllianceIds();
            if (!ids.isEmpty()) {
                nations.addAll(Locutus.imp().getNationDB().getNations(ids));
            }
            Guild guild = db.getGuild();
            for (Member member : guild.getMembers()) {
                DBNation nation = DiscordUtil.getNation(member.getUser());
                if (nation != null) {
                    nations.add(nation);
                }
            }
        }
        else if (isAlliance()) {
            nations.addAll(asAlliance().getNations(false, 0, false));
        } else {
            throw new IllegalArgumentException("Unknwon type " + getIdLong());
        }
        return nations;
    }

    default String getQualifiedName() {
        String name = getName();
        return getTypePrefix() + ":" + (name == null || name.isEmpty() ? getIdLong() : name);
    }

    default boolean isAlliance() {
        return this instanceof DBAlliance;
    }

    default boolean isGuild() {
        return this instanceof GuildDB;
    }

    default DBAlliance asAlliance() {
        return (DBAlliance) this;
    }

    default boolean isNation() {
        return this instanceof DBNation;
    }

    default DBNation asNation() {
        return (DBNation) this;
    }

    default GuildDB asGuild() {
        return (GuildDB) this;
    }

    String getUrl();

    @Command(desc = "Get the sheet url for this")
    default String getSheetUrl() {
        if (isGuild()) return asGuild().getGuild().toString();
        return MarkupUtil.sheetUrl(getName(), getUrl());
    }

    @Command(desc = "Get the markdown url for this")
    default String getMarkdownUrl() {
        if (isGuild()) return asGuild().getGuild().toString();
        return MarkupUtil.markdownUrl(getName(), getUrl());
    }
}
