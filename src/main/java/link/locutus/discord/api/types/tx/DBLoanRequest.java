package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.*;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.loanrequest.LoanRequestUpdateEvent;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DBLoanRequest extends Transaction2 implements DBEntity<AllianceLoanRequest, DBLoanRequest> {
    public double upfront;
    public InterestType interestType;
    public double interestRate;
    public double forcedInterest;
    public double minimumPayment;

    public int duration;
    public PaymentDurationType durationType;


    public DBLoanRequest() {
        interestType = InterestType.NONE;
        durationType = PaymentDurationType.WEEKS;
    }

    @Override
    public String getTableName() {
        return "alliance_loan_request";
    }

    @Override
    public boolean update(AllianceLoanRequest entity, Consumer<Event> eventConsumer) {
        int tx_id = entity.LoanId;
        double[] newResources = ResourceType.getBuffer();
        newResources[entity.LoanResourceType.ordinal()] = entity.InitialAmount;
        DBLoanRequest copy = super.update(
                tx_id,
                entity.RequestedDate.getTime(),
                entity.GiverId,
                entity.GiverType,
                entity.ReciverId,
                entity.ReciverType,
                entity.GiverId,
                entity.RequestText,
                newResources,
                this::copy
        );

        if (Math.round(this.upfront * 100) != Math.round(entity.UpfrontFee * 100)) {
            if (copy == null) copy = this.copy();
            this.upfront = entity.UpfrontFee;
        }
        if (this.interestType != entity.InterestType) {
            if (copy == null) copy = this.copy();
            this.interestType = entity.InterestType;
        }
        if (Math.round(this.interestRate * 100) != Math.round(entity.InterestRate * 100)) {
            if (copy == null) copy = this.copy();
            this.interestRate = entity.InterestRate;
        }
        if (Math.round(this.forcedInterest * 100) != Math.round(entity.ForcedInterest * 100)) {
            if (copy == null) copy = this.copy();
            this.forcedInterest = entity.ForcedInterest;
        }
        if (Math.round(this.minimumPayment * 100) != Math.round(entity.MinimumPayment * 100)) {
            if (copy == null) copy = this.copy();
            this.minimumPayment = entity.MinimumPayment;
        }
        if (this.duration != entity.PaymentDuration) {
            if (copy == null) copy = this.copy();
            this.duration = entity.PaymentDuration;
        }
        if (this.durationType != entity.PaymentDurationType) {
            if (copy == null) copy = this.copy();
            this.durationType = entity.PaymentDurationType;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new LoanRequestUpdateEvent(copy, this));
        }

        return copy != null;
    }

    @Override
    public Object[] write() {
        Object[] arr = new Object[16];
        super.write(arr);
        arr[9] = upfront;
        arr[10] = interestType.ordinal();
        arr[11] = interestRate;
        arr[12] = forcedInterest;
        arr[13] = minimumPayment;
        arr[14] = duration;
        arr[15] = durationType.ordinal();
        return arr;
    }

    @Override
    public void load(Object[] raw) {
        super.load(raw);
        upfront = (double) raw[9];
        interestType = InterestType.values[(int) raw[10]];
        interestRate = (double) raw[11];
        forcedInterest = (double) raw[12];
        minimumPayment = (double) raw[13];
        duration = (int) raw[14];
        durationType = PaymentDurationType.values[(int) raw[15]];
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = super.getTypes();
        types.put("upfront", double.class);
        types.put("interest_type", int.class);
        types.put("interest_rate", double.class);
        types.put("forced_interest", double.class);
        types.put("minimum_payment", double.class);
        types.put("duration", int.class);
        types.put("duration_type", int.class);
        return types;
    }

    @Override
    public DBLoanRequest emptyInstance() {
        return new DBLoanRequest();
    }
}
