package link.locutus.wiki.pages;

import link.locutus.wiki.BotWikiGen;
import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.StringMan;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class WikiCustomSheetsPage extends BotWikiGen {
    public WikiCustomSheetsPage(CommandManager2 manager) {
        super(manager, "custom_spreadsheets");
    }

    @Override
    public String generateMarkdown() {
        return build(
                """
                        <bold style="color:red">NOT PUBLICLY AVAILABLE. The documentation below applies to a feature currently in development, and not yet available for public use</bold>
                        - Use premade selections and sheet templates (WIP)
                        - Select the data you want in your sheet
                        - Select the columns you want for that data
                        - Add it as a tab in a custom sheet
                        - Update the sheet's tabs using a command
                        - Update your selection or columns at any time
                        
                        `Note: Ensure the bot owner has edit access to a sheet if using a custom url`
                        
                        email: `jessepaleg@gmail.com` 
                        """,
                "# Premade sheet templates/selections",
                "To be added. NOT currently working with custom sheet templates. For now: See the various disunified sheet commands below",
                MarkupUtil.spoiler("Internal Affairs Sheets",
                MarkupUtil.markdownToHTML(StringMan.join(Arrays.asList(
                CM.audit.sheet.cmd.toString(),
                CM.sheets_ia.ActivitySheet.cmd.toString(),
                CM.sheets_ia.ActivitySheetFromId.cmd.toString()), "\n"))),
                MarkupUtil.spoiler("Milcom Sheets",
                MarkupUtil.markdownToHTML(StringMan.join(Arrays.asList(
                CM.sheets_milcom.MMRSheet.cmd.toString()), "\n"))),
//                MarkupUtil.spoiler("Econ Sheets",
//                MarkupUtil.markdownToHTML(StringMan.join(Arrays.asList(), "\n")),
                MarkupUtil.spoiler("General sheets",
                MarkupUtil.markdownToHTML(StringMan.join(Arrays.asList(
                CM.nation.sheet.NationSheet.cmd.toString(),
                CM.report.sheet.generate.cmd.toString()), "\n"))),
                "## List and configure",
                "List and set the worksheet or tab used for the above commands",
                commandMarkdownSpoiler(CM.settings_sheet.list.cmd),
                commandMarkdownSpoiler(CM.settings_sheet.set.cmd),
                "# Statistic sheets",
                "Add `attachCsv: True` for any graph command to attach a csv file of the selected data",
                "# Selections",
                "Selections are used for sheets, and as inputs to certain commands (such as a selection of nations)",
                "Use a comma separated list of ids/names, alongside filters to select records, entities, or types",
                "See a page below for syntax and a list of supported filters",
                "## Selection types",
                "- " + Locutus.cmd().getV2().getPlaceholders().getTypes().stream()
                        .map(f -> MarkupUtil.markdownUrl(PlaceholdersMap.getClassName(f), PlaceholdersMap.getClassName(f).toLowerCase(Locale.ROOT) + "_placeholders"))
                        .sorted()
                        .collect(Collectors.joining("\n- ")),
                "## Tab autofill",
                "Populate a sheet's tabs based on the first row column",
                """
                - Each tab can have 1 selection. See the list above for supported types
                - Name the tab the selection type, colon, the selection e.g. `nation:Rose,#position>1`
                ![Tab Selection](tab_selection.png)
                - Enter placeholders in the first row, for example, `{nation}` is a placeholder for a nation type.
                - Placeholders can be combined with formulas. 
                - Use `$row` or `$column` to reference the current row or column
                ![Column Placeholder](column_placeholders.png)
                
                Run the command:
                """,
                commandMarkdownSpoiler(CM.sheet_custom.auto.cmd),
                // Ensure the bot owner has edit access to a sheet
                "## Selection alias",
                "A name you set for a selection",
                "To reference your named selection, use e.g. `$myAlias` or `select:myAlias`",
                "### Add an alias",
                commandMarkdownSpoiler(CM.selection_alias.add.nation.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.alliance.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.nationoralliance.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.guild.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.project.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.treaty.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.resourcetype.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.militaryunit.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.treatytype.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.building.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.audittype.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.nationlist.cmd, false) +
                commandMarkdownSpoiler(CM.selection_alias.add.user.cmd, false),
                commandMarkdownSpoiler(CM.selection_alias.add.war.cmd),
                "### List or remove aliases",
                commandMarkdownSpoiler(CM.selection_alias.list.cmd),
                commandMarkdownSpoiler(CM.selection_alias.remove.cmd),
                commandMarkdownSpoiler(CM.selection_alias.rename.cmd),
                "# Sheet templates",
                "A list of columns",
                "See the type pages above for supported placeholders",
                "Use `$row` and `$column` to reference the current row and column",
                "Templates are used alongside a selection to create a sheet tab",
                "## Add columns to a template",
                "A template will be created if one does not already exist",
                "Use the add command multiple times to add more than 25 columns",
                commandMarkdownSpoiler(CM.sheet_template.add.nation.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.alliance.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.nationoralliance.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.guild.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.project.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.treaty.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.resourcetype.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.militaryunit.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.treatytype.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.building.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.audittype.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.nationlist.cmd, false) +
                commandMarkdownSpoiler(CM.sheet_template.add.user.cmd, false),
                commandMarkdownSpoiler(CM.sheet_template.add.war.cmd),
//                commandMarkdownSpoiler(CM.sheet_template.add.transaction.cmd),
//                commandMarkdownSpoiler(CM.sheet_template.add.trade.cmd),
                "### View, list, remove or modify a template",
                commandMarkdownSpoiler(CM.sheet_template.view.cmd),
                commandMarkdownSpoiler(CM.sheet_template.list.cmd),
                commandMarkdownSpoiler(CM.sheet_template.remove.cmd),
                commandMarkdownSpoiler(CM.sheet_template.remove_column.cmd),
                commandMarkdownSpoiler(CM.sheet_template.rename.cmd),
                "# Creating tabbed sheet",
                commandMarkdownSpoiler(CM.sheet_custom.add_tab.cmd),
                commandMarkdownSpoiler(CM.sheet_custom.update.cmd),
                "## List and view custom sheets",
                commandMarkdownSpoiler(CM.sheet_custom.list.cmd),
                commandMarkdownSpoiler(CM.sheet_custom.view.cmd),
                "## Remove a tab from a sheet",
                commandMarkdownSpoiler(CM.sheet_custom.remove_tab.cmd)
        );
       /*
       nationsheet
       alliancesheet
       alliancenationssheet
        */
    }
}
