package link.locutus.discord.api.types.tx;

import com.google.gson.JsonElement;
import link.locutus.discord.api.generated.AllianceGrantHistory;
import link.locutus.discord.api.generated.BankHistory;
import link.locutus.discord.api.generated.LoanHistory;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.event.Event;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import com.google.gson.JsonObject;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.util.math.ArrayUtil;
import org.jooq.Record;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Transaction2 {
    public long tx_id;
    public long tx_datetime;
    public long sender_id;
    public int sender_type;
    public long receiver_id;
    public int receiver_type;
    public int banker_nation;
    public String note;
    public double[] resources;

    public Transaction2() {
        this.resources = ResourceType.getBuffer();
        this.note = "";
    }

    public <T extends Transaction2> T update(long tx_id,
                                                long tx_datetime,
                                                long sender_id,
                                                int sender_type,
                                                long receiver_id,
                                                int receiver_type,
                                                int banker_nation,
                                                String note,
                                                double[] resources,
                                             Supplier<T> makeCopy) {
        T copy = null;
        if (this.tx_id != tx_id) {
            if (copy == null) copy = makeCopy.get();
            this.tx_id = tx_id;
        }
        if (this.tx_datetime != tx_datetime) {
            if (copy == null) copy = makeCopy.get();
            this.tx_datetime = tx_datetime;
        }
        if (this.sender_id != sender_id) {
            if (copy == null) copy = makeCopy.get();
            this.sender_id = sender_id;
        }
        if (this.sender_type != sender_type) {
            if (copy == null) copy = makeCopy.get();
            this.sender_type = sender_type;
        }
        if (this.receiver_id != receiver_id) {
            if (copy == null) copy = makeCopy.get();
            this.receiver_id = receiver_id;
        }
        if (this.receiver_type != receiver_type) {
            if (copy == null) copy = makeCopy.get();
            this.receiver_type = receiver_type;
        }
        if (this.banker_nation != banker_nation) {
            if (copy == null) copy = makeCopy.get();
            this.banker_nation = banker_nation;
        }
        if (!Objects.equals(this.note, note)) {
            if (copy == null) copy = makeCopy.get();
            this.note = note;
        }
        if (!ResourceType.equals(this.resources, resources)) {
            if (copy == null) copy = makeCopy.get();
            this.resources = resources;
        }
        return copy;
    }

    public Object[] write(Object[] arr) {
        arr[0] = this.tx_id;
        arr[1] = this.tx_datetime;
        arr[2] = this.sender_id;
        arr[3] = this.sender_type;
        arr[4] = this.receiver_id;
        arr[5] = this.receiver_type;
        arr[6] = this.banker_nation;
        arr[7] = this.note;
        arr[8] = ArrayUtil.toByteArray(this.resources);
        return arr;
    }

    public void load(Object[] raw) {
        this.tx_id = (long) raw[0];
        this.tx_datetime = (long) raw[1];
        this.sender_id = (long) raw[2];
        this.sender_type = (int) raw[3];
        this.receiver_id = (long) raw[4];
        this.receiver_type = (int) raw[5];
        this.banker_nation = (int) raw[6];
        this.note = (String) raw[7];
        this.resources = ArrayUtil.toDoubleArray((byte[]) raw[8]);
    }

    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("tx_id", long.class);
        types.put("tx_datetime", long.class);
        types.put("sender_id", long.class);
        types.put("sender_type", int.class);
        types.put("receiver_id", long.class);
        types.put("receiver_type", int.class);
        types.put("banker_nation", int.class);
        types.put("note", String.class);
        types.put("resources", byte[].class);
        return types;
    }

    public boolean isSelfWithdrawal(DBNation nation) {
        if (this.isSenderAA() && this.note != null) {
            Map<String, String> notes = DNS.parseTransferHashNotes(this.note);
            if (notes.containsKey("#deposit")) {
                String banker = notes.get("#banker");
                return MathMan.isInteger(banker) && Long.parseLong(banker) == nation.getNation_id();
            }
        }
        return false;
    }

    private static Map.Entry<Long, Integer> idIsAlliance(Element td) {
        try {
            long id;
            int type;

            String url = td.getElementsByTag("a").get(0).attr("href").toLowerCase();
            id = Integer.parseInt(url.split("=")[1]);
            if (url.contains("/alliance/")) {
                type = 2;
            } else if (url.contains("/nation/")) {
                type = 1;
            } else {
                type = 0;
            }
            return new AbstractMap.SimpleEntry<>(id, type);
        } catch (IndexOutOfBoundsException ignore) {
            return new AbstractMap.SimpleEntry<>(0L, 0);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Transaction2 tx) {
            return tx.tx_id == tx_id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(tx_id);
    }

    public long getDate() {
        return tx_datetime;
    }

    public long getReceiver() {
        return receiver_id;
    }

    public long getSender() {
        return sender_id;
    }

    public double convertedTotal() {
        return ResourceType.convertedTotal(resources);
    }

    public String toSimpleString() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(tx_datetime), ZoneOffset.UTC).toLocalDate() +
                " | " + note +
                " | sender: " + DNS.getName(sender_id, sender_type == 2) +
                " | receiver: " + DNS.getName(receiver_id, receiver_type == 2) +
                " | banker: " + DNS.getName(banker_nation, false) +
                " | " + ResourceType.resourcesToString(resources);
    }

    @Override
    public String toString() {
        return toSimpleString();
    }

    public boolean isSenderGuild() {
        return sender_type == 3;
    }

    public boolean isReceiverGuild() {
        return receiver_type == 3;
    }

    public boolean isSenderAA() {
        return sender_type == 2;
    }

    public boolean isReceiverAA() {
        return receiver_type == 2;
    }

    public boolean isSenderNation() {
        return sender_type == 1;
    }

    public boolean isReceiverNation() {
        return receiver_type == 1;
    }

    public NationOrAllianceOrGuild getSenderObj() {
        return NationOrAllianceOrGuild.create(sender_id, sender_type);
    }

    public NationOrAllianceOrGuild getReceiverObj() {
        return NationOrAllianceOrGuild.create(receiver_id, receiver_type);
    }
}
