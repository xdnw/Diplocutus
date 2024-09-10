package link.locutus.discord.util.sheet;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.RowData;
import link.locutus.discord.db.entities.Activity;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.TimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SheetUtil {
    public static String getLetter(int x) {
        x++;
        String letter = "";
        while (x > 0) {
            int r = (x - 1) % 26;
            int n = (x - 1) / 26;
            letter = ((char) ('A' + r)) + letter;
            x = n;
        }
        return letter;
    }

    public static int getIndex(String column) {
        column = column.toUpperCase();
        int out = 0, len = column.length();
        for (int pos = 0; pos < len; pos++) {
            out += (column.charAt(pos) - 64) * Math.pow(26, len - pos - 1);
        }
        return out;
    }

    public static String getRange(int x, int y) {
        return getLetter(x) + "" + (y + 1);
    }

    public static String getRange(int x1, int y1, int x2, int y2) {
        return getRange(x1, y1) + ":" + getRange(x2, y2);
    }

    public static RowData toRowData(List myList) {
        RowData row = new RowData();
        ArrayList<CellData> cellData = new ArrayList<CellData>();
        for (int i = 0; i < myList.size(); i++) {
            Object obj = myList.get(i);
            if (obj == null) cellData.add(null);

            CellData cell = new CellData();
            String str = obj.toString();
            if (str.startsWith("=")) {
                cell.setUserEnteredValue(new ExtendedValue().setFormulaValue(str));
            } else {
                cell.setUserEnteredValue(new ExtendedValue().setStringValue(str));
            }
            cellData.add(cell);

        }
        row.setValues(cellData);
        return row;
    }

    public static void writeTargets(SpreadSheet sheet, Map<DBNation, List<DBNation>> targets, int turn) throws IOException {
        List<RowData> rowData = new ArrayList<RowData>();

        List<Object> header = new ArrayList<>(Arrays.asList(
                "alliance",
                "nation",
                "land",
                "infra",
                "war_index",
                "score",
                "protection",
                "inactive",
                "login_chance",
                "weekly_activity",
                "att1",
                "att2",
                "att3"
        ));

        rowData.add(SheetUtil.toRowData(header));

        for (Map.Entry<DBNation, List<DBNation>> entry : targets.entrySet()) {
            DBNation defender = entry.getKey();
            List<DBNation> attackers = entry.getValue();
            ArrayList<Object> row = new ArrayList<>();
            row.add(MarkupUtil.sheetUrl(defender.getAllianceName(), defender.getAllianceUrl()));
            row.add(MarkupUtil.sheetUrl(defender.getNation(), defender.getUrl()));

            row.add(defender.getLand());
            row.add(defender.getInfra());
            row.add(defender.getWarIndex() + "");
            row.add(defender.getScore() + "");
            row.add(TimeUtil.secToTime(TimeUnit.MILLISECONDS, defender.getProtectionRemainingMs()) + "");
            row.add(TimeUtil.secToTime(TimeUnit.MINUTES, defender.active_m()));

            Activity activity = defender.getActivity(24 * 14);
            double loginChance = activity.loginChance(turn == -1 ? 23 : turn, 48, false);
            row.add(loginChance);
            row.add(activity.getAverageByDay());

            List<DBNation> myCounters = targets.getOrDefault(defender, Collections.emptyList());

            for (int i = 0; i < myCounters.size(); i++) {
                DBNation counter = myCounters.get(i);
                String counterUrl = MarkupUtil.sheetUrl(counter.getNation(), counter.getUrl());
                row.add(counterUrl);
            }
            RowData myRow = SheetUtil.toRowData(row);
            List<CellData> myRowData = myRow.getValues();
            int attOffset = myRowData.size() - myCounters.size();
            for (int i = 0; i < myCounters.size(); i++) {
                DBNation counter = myCounters.get(i);
                myRowData.get(attOffset + i).setNote(getAttackerNote(counter));
            }
            myRow.setValues(myRowData);

            rowData.add(myRow);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite(null, rowData);
    }

    private static String getAttackerNote(DBNation nation) {
        StringBuilder note = new StringBuilder();
        double score = nation.getScore();
        double minScore = Math.ceil(nation.getScore() * (nation.isInactiveForWar() ? DNS.WAR_RANGE_MIN_MODIFIER_INACTIVE : DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE));
        double maxScore = Math.floor(nation.getScore() * (nation.isInactiveForWar() ? DNS.WAR_RANGE_MAX_MODIFIER_INACTIVE : DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE));
        note.append("War Range: " + MathMan.format(minScore) + "-" + MathMan.format(maxScore) + " (" + score + ")").append("\n");
        note.append("ID: " + nation.getNation_id()).append("\n");
        note.append("Alliance: " + nation.getAllianceName()).append("\n");
        note.append("Land: " + nation.getLand()).append("\n");
        note.append("Infra: " + nation.getInfra()).append("\n");
        note.append("WarIndex: " + nation.getWarIndex()).append("\n");
        return note.toString();
    }
}
