package link.locutus.discord.api.generated;

import link.locutus.discord.api.types.MilitaryUnit;

import javax.annotation.Nullable;
import java.util.Date;

// Alliance and Nation
public class EquipmentTransactionHistory {
    public Integer TransactionId;

    public Integer Quantity;

    public Integer Quality;

    public @Nullable String MessageTxt;

    // Unique values: Alliance Grant, Foreign Military Aid Sent, Alliance Donation
    public BankType TypeTxt;

    public Integer NationId;

    public String NationName;

    public Integer NationId2;

    public String NationName2;

    public Integer AllianceId;

    public String AllianceName;

    public Integer AllianceId2;

    public String AllianceName2;

    public Date TimeStampTxt;

    public MilitaryUnit EquipmentType;

}
