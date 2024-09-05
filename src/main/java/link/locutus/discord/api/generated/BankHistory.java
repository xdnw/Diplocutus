package link.locutus.discord.api.generated;

import javax.annotation.Nullable;
import java.util.Date;

public class BankHistory {
    public @Nullable Integer NationId;

    public @Nullable String NationName;

    public @Nullable Integer AllianceId;

    public @Nullable String AllianceName;

    // Unique values: Alliance Grant, Foreign Aid Sent, Foreign Aid Received, Alliance Donation
    public BankType TypeTxt;

    public Double valueAmount;

    public Date TimeStampTxt;

    public @Nullable Integer NationId2;

    public @Nullable String NationName2;

    public @Nullable Integer AllianceId2;

    public @Nullable String AllianceName2;

    public Double CashAmount;

    public Double TechAmount;

    public Double MineralAmount;

    public Double UraniumAmount;

    public Double RareMetalAmount;

    public Double FuelAmount;

    public @Nullable String MessageTxt;

}
