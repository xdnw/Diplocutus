package link.locutus.discord.api.generated;

import java.util.Date;

public class AllianceLoanRequest {
    public Integer LoanId;
    public Integer GiverId;
    public Integer ReciverId;
    public String ReciverName;
    public String GiverName;
    public Integer GiverType;
    public Integer ReciverType;
    public LoanType LoanType;
    public ResourceType LoanResourceType;
    public Double InitialAmount;
    public Double UpfrontFee;
    public InterestType InterestType;
    public Double InterestRate;
    public Double ForcedInterest;
    public Double MinimumPayment;
    public Date RequestedDate;
    public Integer PaymentDuration;
    public PaymentDurationType PaymentDurationType;
    public String RequestText;
}
