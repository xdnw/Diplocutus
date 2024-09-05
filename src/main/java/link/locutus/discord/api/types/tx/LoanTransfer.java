package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.*;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.loan.LoanUpdateEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class LoanTransfer implements DBEntity<LoanHistory, LoanTransfer> {
    // giver/receiver type 0 = nation, 1 = alliance
    public int LoanId;
    public int GiverId;
    public int ReciverId;
    public int GiverType;
    public int ReciverType;
    public LoanType LoanType;
    public ResourceType LoanResourceType;
    public double InitialAmount;
    public double UpfrontFee;
    public double InterestRate;
    public double ForcedInterest;
    public double MinimumPayment;
    public PaymentDurationType PaymentDurationType;
    public int PaymentDuration;
    public String RequestText;
    public long GivenDate;
    public long NextPaymentDueDate;
    public long DueDate;
    public long NextInterestDate;
    public double PercentRepayed;
    public int MissedPaymentsInARow;
    public int MissedPayments;
    public LoanStatus LoanStatus;
    public int Late;
    public int SoldOff;
    public int FrozenInterest;
    public double CurrentAmount;
    public double TotalPayedAmount;
    public double TotalInterest;
    public String ApprovalText;
    public double RemainingMinimumPayment;
    public long DebtCollectedDate; // may be null
    public long PayedOffDate;
    public InterestType InterestType;

    @Override
    public String getTableName() {
        return "loan_transfer";
    }

    @Override
    public boolean update(LoanHistory entity, Consumer<Event> eventConsumer) {
        LoanTransfer copy = null;
        if (entity.LoanId != LoanId) {
            if (copy == null) copy = new LoanTransfer();
            LoanId = entity.LoanId;
        }
        if (entity.GiverId != GiverId) {
            if (copy == null) copy = new LoanTransfer();
            GiverId = entity.GiverId;
        }
        if (entity.ReciverId != ReciverId) {
            if (copy == null) copy = new LoanTransfer();
            ReciverId = entity.ReciverId;
        }
        if (entity.GiverType != GiverType) {
            if (copy == null) copy = new LoanTransfer();
            GiverType = entity.GiverType;
        }
        if (entity.ReciverType != ReciverType) {
            if (copy == null) copy = new LoanTransfer();
            ReciverType = entity.ReciverType;
        }
        if (entity.LoanType != LoanType) {
            if (copy == null) copy = new LoanTransfer();
            LoanType = entity.LoanType;
        }
        if (entity.LoanResourceType != LoanResourceType) {
            if (copy == null) copy = new LoanTransfer();
            LoanResourceType = entity.LoanResourceType;
        }
        if (entity.InitialAmount != InitialAmount) {
            if (copy == null) copy = new LoanTransfer();
            InitialAmount = entity.InitialAmount;
        }
        if (entity.UpfrontFee != UpfrontFee) {
            if (copy == null) copy = new LoanTransfer();
            UpfrontFee = entity.UpfrontFee;
        }
        if (entity.InterestRate != InterestRate) {
            if (copy == null) copy = new LoanTransfer();
            InterestRate = entity.InterestRate;
        }
        if (entity.ForcedInterest != ForcedInterest) {
            if (copy == null) copy = new LoanTransfer();
            ForcedInterest = entity.ForcedInterest;
        }
        if (entity.MinimumPayment != MinimumPayment) {
            if (copy == null) copy = new LoanTransfer();
            MinimumPayment = entity.MinimumPayment;
        }
        if (entity.PaymentDurationType != PaymentDurationType) {
            if (copy == null) copy = new LoanTransfer();
            PaymentDurationType = entity.PaymentDurationType;
        }
        if (entity.PaymentDuration != PaymentDuration) {
            if (copy == null) copy = new LoanTransfer();
            PaymentDuration = entity.PaymentDuration;
        }
        if (!entity.RequestText.equals(RequestText)) {
            if (copy == null) copy = new LoanTransfer();
            RequestText = entity.RequestText;
        }
        long newGivenDate = entity.GivenDate != null ? entity.GivenDate.getTime() : 0;
        if (newGivenDate != GivenDate) {
            if (copy == null) copy = new LoanTransfer();
            GivenDate = newGivenDate;
        }
        long newNextPaymentDueDate = entity.NextPaymentDueDate != null ? entity.NextPaymentDueDate.getTime() : 0;
        if (newNextPaymentDueDate != NextPaymentDueDate) {
            if (copy == null) copy = new LoanTransfer();
            NextPaymentDueDate = newNextPaymentDueDate;
        }
        long newDueDate = entity.DueDate != null ? entity.DueDate.getTime() : 0;
        if (newDueDate != DueDate) {
            if (copy == null) copy = new LoanTransfer();
            DueDate = newDueDate;
        }
        long newNextInterestDate = entity.NextInterestDate != null ? entity.NextInterestDate.getTime() : 0;
        if (newNextInterestDate != NextInterestDate) {
            if (copy == null) copy = new LoanTransfer();
            NextInterestDate = newNextInterestDate;
        }
        if (entity.PercentRepayed != PercentRepayed) {
            if (copy == null) copy = new LoanTransfer();
            PercentRepayed = entity.PercentRepayed;
        }
        if (entity.MissedPaymentsInARow != MissedPaymentsInARow) {
            if (copy == null) copy = new LoanTransfer();
            MissedPaymentsInARow = entity.MissedPaymentsInARow;
        }
        if (entity.MissedPayments != MissedPayments) {
            if (copy == null) copy = new LoanTransfer();
            MissedPayments = entity.MissedPayments;
        }
        if (entity.LoanStatus != LoanStatus) {
            if (copy == null) copy = new LoanTransfer();
            LoanStatus = entity.LoanStatus;
        }
        if (entity.Late != Late) {
            if (copy == null) copy = new LoanTransfer();
            Late = entity.Late;
        }
        if (entity.SoldOff != SoldOff) {
            if (copy == null) copy = new LoanTransfer();
            SoldOff = entity.SoldOff;
        }
        if (entity.FrozenInterest != FrozenInterest) {
            if (copy == null) copy = new LoanTransfer();
            FrozenInterest = entity.FrozenInterest;
        }
        if (entity.CurrentAmount != CurrentAmount) {
            if (copy == null) copy = new LoanTransfer();
            CurrentAmount = entity.CurrentAmount;
        }
        if (entity.TotalPayedAmount != TotalPayedAmount) {
            if (copy == null) copy = new LoanTransfer();
            TotalPayedAmount = entity.TotalPayedAmount;
        }
        if (entity.TotalInterest != TotalInterest) {
            if (copy == null) copy = new LoanTransfer();
            TotalInterest = entity.TotalInterest;
        }
        if (!Objects.equals(entity.ApprovalText, ApprovalText)) {
            if (copy == null) copy = new LoanTransfer();
            ApprovalText = entity.ApprovalText;
        }
        if (entity.RemainingMinimumPayment != RemainingMinimumPayment) {
            if (copy == null) copy = new LoanTransfer();
            RemainingMinimumPayment = entity.RemainingMinimumPayment;
        }
        long newDebtCollectedDate = entity.DebtCollectedDate != null ? entity.DebtCollectedDate.getTime() : 0;
        if (newDebtCollectedDate != DebtCollectedDate) {
            if (copy == null) copy = new LoanTransfer();
            DebtCollectedDate = newDebtCollectedDate;
        }
        long newPayedOffDate = entity.PayedOffDate != null ? entity.PayedOffDate.getTime() : 0;
        if (newPayedOffDate != PayedOffDate) {
            if (copy == null) copy = new LoanTransfer();
            PayedOffDate = newPayedOffDate;
        }
        if (entity.InterestType != InterestType) {
            if (copy == null) copy = new LoanTransfer();
            InterestType = entity.InterestType;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new LoanUpdateEvent(copy, this));
        }
        return copy != null;
    }

    @Override
    public Object[] write() {
        return new Object[]{
                LoanId,
                GiverId,
                ReciverId,
                GiverType,
                ReciverType,
                LoanType.ordinal(),
                LoanResourceType.ordinal(),
                InitialAmount,
                UpfrontFee,
                InterestRate,
                ForcedInterest,
                MinimumPayment,
                PaymentDurationType.ordinal(),
                PaymentDuration,
                RequestText,
                GivenDate,
                NextPaymentDueDate,
                DueDate,
                NextInterestDate,
                PercentRepayed,
                MissedPaymentsInARow,
                MissedPayments,
                LoanStatus.ordinal(),
                Late,
                SoldOff,
                FrozenInterest,
                CurrentAmount,
                TotalPayedAmount,
                TotalInterest,
                ApprovalText,
                RemainingMinimumPayment,
                DebtCollectedDate,
                PayedOffDate,
                InterestType.ordinal()
        };
    }

    @Override
    public void load(Object[] raw) {
        LoanId = (int) raw[0];
        GiverId = (int) raw[1];
        ReciverId = (int) raw[2];
        GiverType = (int) raw[3];
        ReciverType = (int) raw[4];
        LoanType = link.locutus.discord.api.generated.LoanType.values[(int) raw[5]];
        LoanResourceType = ResourceType.values[(int) raw[6]];
        InitialAmount = (double) raw[7];
        UpfrontFee = (double) raw[8];
        InterestRate = (double) raw[9];
        ForcedInterest = (double) raw[10];
        MinimumPayment = (double) raw[11];
        PaymentDurationType = link.locutus.discord.api.generated.PaymentDurationType.values[(int) raw[12]];
        PaymentDuration = (int) raw[13];
        RequestText = (String) raw[14];
        GivenDate = (long) raw[15];
        NextPaymentDueDate = (long) raw[16];
        DueDate = (long) raw[17];
        NextInterestDate = (long) raw[18];
        PercentRepayed = (double) raw[19];
        MissedPaymentsInARow = (int) raw[20];
        MissedPayments = (int) raw[21];
        LoanStatus = link.locutus.discord.api.generated.LoanStatus.values[(int) raw[22]];
        Late = (int) raw[23];
        SoldOff = (int) raw[24];
        FrozenInterest = (int) raw[25];
        CurrentAmount = (double) raw[26];
        TotalPayedAmount = (double) raw[27];
        TotalInterest = (double) raw[28];
        ApprovalText = (String) raw[29];
        RemainingMinimumPayment = (double) raw[30];
        DebtCollectedDate = (long) raw[31];
        PayedOffDate = (long) raw[32];
        InterestType = link.locutus.discord.api.generated.InterestType.values[(int) raw[33]];

    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("LoanId", Integer.class);
        types.put("GiverId", Integer.class);
        types.put("ReciverId", Integer.class);
        types.put("GiverType", Integer.class);
        types.put("ReciverType", Integer.class);
        types.put("LoanType", Integer.class);
        types.put("LoanResourceType", Integer.class);
        types.put("InitialAmount", Double.class);
        types.put("UpfrontFee", Double.class);
        types.put("InterestRate", Double.class);
        types.put("ForcedInterest", Double.class);
        types.put("MinimumPayment", Double.class);
        types.put("PaymentDurationType", Integer.class);
        types.put("PaymentDuration", Integer.class);
        types.put("RequestText", String.class);
        types.put("GivenDate", Long.class);
        types.put("NextPaymentDueDate", Long.class);
        types.put("DueDate", Long.class);
        types.put("NextInterestDate", Long.class);
        types.put("PercentRepayed", Double.class);
        types.put("MissedPaymentsInARow", Integer.class);
        types.put("MissedPayments", Integer.class);
        types.put("LoanStatus", Integer.class);
        types.put("Late", Integer.class);
        types.put("SoldOff", Integer.class);
        types.put("FrozenInterest", Integer.class);
        types.put("CurrentAmount", Double.class);
        types.put("TotalPayedAmount", Double.class);
        types.put("TotalInterest", Double.class);
        types.put("ApprovalText", String.class);
        types.put("RemainingMinimumPayment", Double.class);
        types.put("DebtCollectedDate", Long.class);
        types.put("PayedOffDate", Long.class);
        types.put("InterestType", Integer.class);
        return types;
    }

    @Override
    public LoanTransfer emptyInstance() {
        return new LoanTransfer();
    }
}
