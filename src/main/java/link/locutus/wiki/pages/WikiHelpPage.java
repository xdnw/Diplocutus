package link.locutus.wiki.pages;

import link.locutus.wiki.BotWikiGen;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.util.MarkupUtil;

import java.util.ArrayList;
import java.util.List;

import static link.locutus.discord.util.MarkupUtil.spoiler;

public class WikiHelpPage extends BotWikiGen {
    private final List<BotWikiGen> pages;
    private final String urlPrefix;
    private final List<BotWikiGen> placeholders;
    private final BotWikiGen permsPage;

    public WikiHelpPage(CommandManager2 manager, List<BotWikiGen> pages, List<BotWikiGen> placeholders, BotWikiGen permsPage) {
        super(manager, "home");
        this.pages = new ArrayList<>(pages);
        this.placeholders = placeholders;
        this.permsPage = permsPage;
        this.urlPrefix = "../wiki/";
    }

    @Override
    public String generateMarkdown() {
        StringBuilder pageList = new StringBuilder();
        for (BotWikiGen page : pages) {
            if (page.generateMarkdown().trim().isEmpty()) continue;
            String url = urlPrefix + page.getPageName().replace(" ", "_");
            pageList.append("### " + MarkupUtil.markdownUrl(page.getPageName(), url)).append("\n");
            pageList.append("> " + page.getDescription().replace("\n", "\n> ")).append("\n");
        }

        StringBuilder placeholderList = new StringBuilder();
        for (BotWikiGen page : placeholders) {
            String url = urlPrefix + page.getPageName().replace(" ", "_");
            placeholderList.append("- " + MarkupUtil.markdownUrl(page.getPageName(), url)).append("\n");
        }

        return build(
                "# Command Help",
                spoiler("Command Syntax", MarkupUtil.markdownToHTML("""
                - `<arg>` - A required parameter
                - `[arg]` - An optional parameter
                - `<arg1|arg2>` - Multiple parameters options
                - `<arg=value>` - Default or suggested value
                - `[-f flag]` - A optional command argument flag""")),
                spoiler("Using the help commands",
                        MarkupUtil.markdownToHTML(CM.help.command.cmd.getCallable(true).simpleDesc() + "\n\n" +
                CM.help.command.cmd.toSlashCommand(true) +
                "\n\n---\n\n" +
                CM.help.find_command.cmd.getCallable(true).simpleDesc() + "\n\n" +
                CM.help.find_command.cmd.toSlashCommand(true) +
                "\n\n---\n\n" +
                CM.help.find_nation_placeholder.cmd.getCallable(true).simpleDesc() + "\n\n" +
                CM.help.find_nation_placeholder.cmd.toSlashCommand(true) +
                "\n\n---\n\n" +
                CM.help.nation_placeholder.cmd.getCallable(true).simpleDesc() + "\n\n" +
                CM.help.nation_placeholder.cmd.toSlashCommand(true))),
                spoiler("List available settings",
                        MarkupUtil.markdownToHTML(CM.settings.info.cmd.toSlashCommand(true) +
                "\n\n---\n\n" +
                CM.help.find_setting.cmd.getCallable(true).simpleDesc() + "\n\n" +
                CM.help.find_setting.cmd.toSlashCommand(true))),
                spoiler("List ALL settings",
                MarkupUtil.markdownToHTML(CM.settings.info.cmd.listAll(Boolean.TRUE + "").toSlashCommand(true))),
                spoiler("View a setting",
                        MarkupUtil.markdownToHTML("For example, the `" + GuildKey.ALLIANCE_ID.name() + "` setting" + "\n\n" +
                CM.settings.info.cmd.key(GuildKey.ALLIANCE_ID.name()).toSlashCommand(true))),
                "# Overview of this Wiki",
                pageList.toString(),
                "# Placeholders & Filters",
                "Used in commands to filter a selection, or as placeholders for sheets or messages",
                placeholderList.toString(),
                "# Permissions",
                permsPage.getDescription(),
                MarkupUtil.markdownUrl(permsPage.getPageName(), urlPrefix + permsPage.getPageName().replace(" ", "_"))
        );
    }
}
