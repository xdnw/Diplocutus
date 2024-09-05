package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.AllianceGrantHistory;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.grant.GrantUpdateEvent;
import link.locutus.discord.util.TimeUtil;

import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

public class GrantTransfer extends Transaction2 implements DBEntity<AllianceGrantHistory, GrantTransfer> {
    public long RequestedDate;
    public String requestText;

    public GrantTransfer() {
        super();
        this.requestText = "";
    }

    @Override
    public String getTableName() {
        return "alliance_grant_history";
    }

    @Override
    public boolean update(AllianceGrantHistory entity, Consumer<Event> eventConsumer) {
        long tx_id = (entity.RequestedDate.getTime() - TimeUtil.getOrigin()) << 16 ^ entity.MemberId;
        double[] newResources = ResourceType.getBuffer();
        newResources[ResourceType.CASH.ordinal()] = entity.CashAmount;
        newResources[ResourceType.TECHNOLOGY.ordinal()] = entity.TechAmount;
        newResources[ResourceType.MINERALS.ordinal()] = entity.MineralsAmount;
        newResources[ResourceType.URANIUM.ordinal()] = entity.UraniumAmount;
        newResources[ResourceType.RARE_METALS.ordinal()] = entity.RareMetalsAmount;
        newResources[ResourceType.FUEL.ordinal()] = entity.FuelAmount;
        GrantTransfer copy = super.update(
                tx_id,
                entity.ApprovedDate.getTime(),
                entity.AllianceId,
                1,
                entity.MemberId,
                0,
                entity.EditorId,
                entity.approvalText,
                newResources,
                this::copy
        );
//        this.RequestedDate = entity.RequestedDate.getTime();
        if (this.RequestedDate != entity.RequestedDate.getTime()) {
            if (copy == null) copy = this.copy();
            this.RequestedDate = entity.RequestedDate.getTime();
        }
//        this.requestText = entity.requestText;
        if (!this.requestText.equals(entity.requestText)) {
            if (copy == null) copy = this.copy();
            this.requestText = entity.requestText;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new GrantUpdateEvent(copy, this));
        }
        return copy != null;
    }

    @Override
    public Object[] write() {
        Object[] arr = new Object[11];
        super.write(arr);
        arr[9] = this.RequestedDate;
        arr[10] = this.requestText;
        return arr;
    }

    @Override
    public void load(Object[] raw) {
        super.load(raw);
        this.RequestedDate = (long) raw[9];
        this.requestText = (String) raw[10];

    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = super.getTypes();
        types.put("RequestedDate", long.class);
        types.put("requestText", String.class);
        return types;
    }

    @Override
    public GrantTransfer emptyInstance() {
        return new GrantTransfer();
    }
}
