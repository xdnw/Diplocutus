package link.locutus.discord.db.entities.components;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.generated.AllianceBankValues;
import link.locutus.discord.api.generated.AllianceMemberFunds;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.*;
import link.locutus.discord.db.SQLUtil;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.event.Event;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.math.ArrayUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AlliancePrivate implements DBEntity<Void, AlliancePrivate> {
    private int parentId;

    private final Map<ResourceType, Double> stockpile = new ConcurrentHashMap<>();
    private final Map<ResourceType, Double> memberDeposited = new ConcurrentHashMap<>();

    private final AtomicLong outdatedStockpileAndDeposits = new AtomicLong(-1);

    public AlliancePrivate() {
        this(0);
    }

    public AlliancePrivate(int parentId) {
        this.parentId = parentId;
    }

    @Override
    public String getTableName() {
        return "alliance_private";
    }

    public DBAlliance getAlliance() {
        return DBAlliance.getOrCreate(parentId);
    }

    @Override
    public boolean update(Void entity, Consumer<Event> eventConsumer) {
        return false;
    }

    @Override
    public Object[] write() {
        return new Object[]{
                parentId,
                ArrayUtil.toByteArray(ResourceType.resourcesToArray(stockpile)),
                ArrayUtil.toByteArray(ResourceType.resourcesToArray(memberDeposited)),
                outdatedStockpileAndDeposits.get(),
        };
    }

    @Override
    public void load(Object[] raw) {
        stockpile.clear();
        memberDeposited.clear();

        this.parentId = (int) raw[0];
        if (raw[1] != null) stockpile.putAll(ResourceType.resourcesToMap(ArrayUtil.toDoubleArray((byte[]) raw[1])));
        if (raw[2] != null) memberDeposited.putAll(ResourceType.resourcesToMap(ArrayUtil.toDoubleArray((byte[]) raw[2])));
        outdatedStockpileAndDeposits.set(SQLUtil.castLong(raw[3]));
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        result.put("alliance_id", int.class);
        result.put("stockpile", byte[].class);
        result.put("memberDeposited", byte[].class);
        result.put("outdatedStockpileAndDeposits", long.class);
        return result;
    }

    @Override
    public AlliancePrivate emptyInstance() {
        return new AlliancePrivate();
    }

    public AlliancePrivate copy() {
        AlliancePrivate result = new AlliancePrivate(parentId);
        result.stockpile.putAll(stockpile);
        result.memberDeposited.putAll(memberDeposited);
        result.outdatedStockpileAndDeposits.set(outdatedStockpileAndDeposits.get());
        return result;
    }

    public int getAllianceId() {
        return parentId;
    }

    //    private final Map<ResourceType, Double> stockpile = new ConcurrentHashMap<>();
    public Map<ResourceType, Double> getStockpile(long timestamp) {
        return withApi(outdatedStockpileAndDeposits, timestamp, stockpile, () -> {
            DnsApi api = getAlliance().getApi(true);
            if (api != null) {
                List<AllianceBankValues> result = api.allianceBankValues().call();
                if (result != null && !result.isEmpty()) {
                    if (update(result.get(0))) {
                        Locutus.imp().getNationDB().saveAlliancePrivate(this);
                    }
                    return true;
                }
            }
            return false;
        });
    }

    // stockpile / member funds
    public boolean update(AllianceBankValues stockpile) {
        boolean dirty = false;
        for (ResourceType resource : ResourceType.values) {
            double existingStockpile = this.stockpile.getOrDefault(resource, 0.0);
            double existingMemberDeposited = this.memberDeposited.getOrDefault(resource, 0.0);

            double newStockpile = resource.getAlliance(stockpile);
            double newMemberDeposited = resource.getMember(stockpile);

            if (Math.round(existingStockpile * 100) != Math.round(newStockpile * 100)) {
                this.stockpile.put(resource, newStockpile);
                dirty = true;
            }
            if (Math.round(existingMemberDeposited * 100) != Math.round(newMemberDeposited * 100)) {
                this.memberDeposited.put(resource, newMemberDeposited);
                dirty = true;
            }
        }
        return dirty;
    }
}
