package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.AllianceGrantHistory;
import link.locutus.discord.api.generated.AllianceGrantRequest;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DBGrantRequest extends Transaction2 implements DBEntity<AllianceGrantRequest, DBGrantRequest> {


    public DBGrantRequest() {
    }

    @Override
    public String getTableName() {
        return "alliance_grant_request";
    }

    public boolean update(AllianceGrantRequest entity, int allianceId, Consumer<Event> eventConsumer) {
        if (entity == null) return false;
        if (allianceId == 0) throw new IllegalArgumentException("Alliance ID cannot be 0");
        int tx_id = entity.grantId;
        int receiverId = entity.MemberId;
        int receiverType = 0;
        int senderType = 1;
        double[] amount = ResourceType.getBuffer();
        amount[ResourceType.CASH.ordinal()] = entity.CashAmount;
        amount[ResourceType.TECHNOLOGY.ordinal()] = entity.TechAmount;
        amount[ResourceType.MINERALS.ordinal()] = entity.MineralsAmount;
        amount[ResourceType.URANIUM.ordinal()] = entity.UraniumAmount;
        amount[ResourceType.RARE_METALS.ordinal()] = entity.RareMetalsAmount;
        amount[ResourceType.FUEL.ordinal()] = entity.FuelAmount;

        DBGrantRequest copy = super.update(
                tx_id,
                entity.RequestedDate.getTime(),
                allianceId,
                senderType,
                receiverId,
                receiverType,
                allianceId,
                entity.requestText,
                amount,
                this::copy
        );
        if (copy != null) {

        }
        return true;
    }

    @Override
    public boolean update(AllianceGrantRequest entity, Consumer<Event> eventConsumer) {
        return update(entity, (int) sender_id, eventConsumer);
    }

    @Override
    public Object[] write() {
        return super.write(new Object[9]);
    }

    @Override
    public void load(Object[] raw) {
        super.load(raw);
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        return super.getTypes();
    }

    @Override
    public DBGrantRequest emptyInstance() {
        return new DBGrantRequest();
    }
}
