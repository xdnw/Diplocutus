package link.locutus.discord.db.entities;

import link.locutus.discord.api.generated.AllianceTreaties;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.NoFormat;
import link.locutus.discord.event.Event;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.TimeUtil;
import org.jooq.meta.derby.sys.Sys;

import java.util.*;
import java.util.function.Consumer;

public class Treaty implements DBEntity<AllianceTreaties, Treaty> {
    private long id;
    private long date;
    private long endTime;
    private TreatyType type;
    private int fromId;
    private int toId;

    public Treaty() {

    }

    @Override
    public String getTableName() {
        return "treaties";
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean update(AllianceTreaties entity, Consumer<Event> eventConsumer) {
        if (eventConsumer != null) throw new UnsupportedOperationException("Treaty does not support events");
        int minId = Math.min(entity.AllianceId, entity.AllianceId2);
        int maxId = Math.max(entity.AllianceId, entity.AllianceId2);
        this.id = MathMan.pairInt(minId, maxId);

        boolean dirty = false;
        if (entity.StartDate != null) {
            long time = entity.StartDate.getTime();
            if (time != date) {
                date = time;
                dirty = true;
            }
        }
        if (entity.EndDate != null) {
            long time = entity.EndDate.getTime();
            if (time != endTime) {
                endTime = time;
                dirty = true;
            }
        }
        if (entity.TreatyType != null && entity.TreatyType != type) {
            type = entity.TreatyType;
            dirty = true;
        }
        if (entity.AllianceId != null && entity.AllianceId2 != null) {
            int thisMin = Math.min(fromId, toId);
            int thisMax = Math.max(fromId, toId);
            if (minId != thisMin || maxId != thisMax) {
                fromId = minId;
                toId = maxId;
                dirty = true;
            }
        }
        return dirty;
    }

    @Override
    public Object[] write() {
        return new Object[]{id, date, endTime, type, fromId, toId};
    }

    @Override
    public void load(Object[] raw) {
        id = (long) raw[0];
        date = (long) raw[1];
        endTime = (long) raw[2];
        type = TreatyType.values[(int) raw[3]];
        fromId = (int) raw[4];
        toId = (int) raw[5];
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("id", long.class);
        types.put("date", long.class);
        types.put("endTime", long.class);
        types.put("type", int.class);
        types.put("fromId", int.class);
        types.put("toId", int.class);
        return types;
    }

    @Override
    public Treaty emptyInstance() {
        return new Treaty();
    }

    @Command(desc = "Absolute turns (since unix epoch) this treaty ends")
    public long getEndTime() {
        return endTime;
    }

    @Command(desc = "Number of turns until this treaty ends")
    public long getTimeRemaining() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    @Command(desc = "Date this treaty was signed")
    public long getDate() {
        return date;
    }

    @Command(desc = "Type of this treaty")
    public TreatyType getType() {
        return type;
    }

    public int getMinFromToId() {
        return Math.min(getFromId(), getToId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Treaty treaty = (Treaty) o;
        return (treaty.fromId == fromId && treaty.toId == toId && treaty.type == type) ||
                (treaty.fromId == toId && treaty.toId == fromId && treaty.type == type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, type);
    }

    @Command(desc = "id of the alliance that sent this treaty")
    public int getFromId() {
        return fromId;
    }

    @Command(desc = "id of the alliance that received this treaty")
    public int getToId() {
        return toId;
    }

    @Command(desc = "Get the alliance that sent this treaty")
    public DBAlliance getFrom() {
        return DBAlliance.getOrCreate(getFromId());
    }

    @Command(desc = "Get the alliance that received this treaty")
    public DBAlliance getTo() {
        return DBAlliance.getOrCreate(getToId());
    }

    @Command(desc = "If this treaty is between the given alliances")
    public boolean isAlliance(@NoFormat Set<DBAlliance> fromOrTo) {
        return fromOrTo.contains(getFrom()) || fromOrTo.contains(getTo());
    }

    @Override
    public String toString() {
        return "Treaty{" +
                ", date=" + date +
                ", type=" + type +
                ", from=" + fromId +
                ", to=" + toId +
                ", endTime=" + endTime +
                '}';
    }

    @Command
    public String toLineString() {
        return type + ":" + DNS.getName(fromId, true) + "/" + DNS.getName(toId, true);
    }
}
