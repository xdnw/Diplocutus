package link.locutus.discord.api.generated;

import javax.annotation.Nullable;

import java.util.Date;

public class LoanHistory {
    public Integer LoanId;

    public Integer GiverId;

    public Integer ReciverId;

    public String ReciverName;

    public String GiverName;

    public Integer GiverType;

    public Integer ReciverType;

    // Unique values: Alliance Loan
    public LoanType LoanType;

    // Unique values: Cash
    public ResourceType LoanResourceType;

    public Double InitialAmount;

    public Double UpfrontFee;

    public Double InterestRate;

    public Double ForcedInterest;

    public Double MinimumPayment;

    // Unique values: Weeks
    public PaymentDurationType PaymentDurationType;

    public Integer PaymentDuration;

    public String RequestText;

    public Date GivenDate;

    public Date NextPaymentDueDate;

    public Date DueDate;

    public Date NextInterestDate;

    public Double PercentRepayed;

    public Integer MissedPaymentsInARow;

    public Integer MissedPayments;

    // Unique values: Paid off
    public LoanStatus LoanStatus;

    public Integer Late;

    public Integer SoldOff;

    public Integer FrozenInterest;

    public Double CurrentAmount;

    public Double TotalPayedAmount;

    public Double TotalInterest;

    public String ApprovalText;

    public Double RemainingMinimumPayment;

    public @Nullable Date DebtCollectedDate;

    public Date PayedOffDate;

    // Unique values: None
    public InterestType InterestType;

}
