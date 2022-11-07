package com.illtamer.infinite.bot.expansion.github.task;

import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.expansion.github.hook.APIHolder;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CheckRunner extends BukkitRunnable {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat PARSE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final List<Long> noticeUserList;
    private final List<Long> noticeGroupList;
    private final boolean enableCommit;
    private final boolean enableRelease;
    private final List<String> repoList;

    public CheckRunner(ExpansionConfig configFile) {
        final FileConfiguration config = configFile.getConfig();
        final ConfigurationSection notice = config.getConfigurationSection("notice");
        this.noticeUserList = notice.getLongList("user");
        this.noticeGroupList = notice.getLongList("group");
        final ConfigurationSection listener = config.getConfigurationSection("repo.listener");
        this.enableCommit = listener.getBoolean("enable-commit");
        this.enableRelease = listener.getBoolean("enable-release");
        this.repoList = listener.getStringList("uri");
        final int periodMinute = listener.getInt("period");
        runTaskTimerAsynchronously(Bootstrap.getInstance(), 0, periodMinute * 60 * 20L);
    }

    @Override
    public void run() {
        try {
            for (String repoURI : repoList) {
                String commitContent = null;
                String releaseContent = null;
                if (enableCommit) commitContent = buildCommits(repoURI);
                if (enableRelease) releaseContent = buildReleases(repoURI);
                String finalCommitContent = commitContent;
                String finalReleaseContent = releaseContent;
                noticeUserList.forEach(user -> {
                    if (finalCommitContent != null)
                        OpenAPIHandling.sendMessage(finalCommitContent, user);
                    if (finalReleaseContent != null)
                        OpenAPIHandling.sendMessage(finalReleaseContent, user);
                });
                noticeGroupList.forEach(group -> {
                    if (finalCommitContent != null)
                        OpenAPIHandling.sendGroupMessage(finalCommitContent, group);
                    if (finalReleaseContent != null)
                        OpenAPIHandling.sendGroupMessage(finalReleaseContent, group);

                });
            }
        } catch (ParseException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private String buildReleases(String repoURI) throws ParseException, UnsupportedEncodingException {
        final List<JsonObject> updateRelease = APIHolder.checkUpdateRelease(repoURI);
        if (updateRelease.size() == 0) return null;
        StringBuilder builder = new StringBuilder();
        builder.append(repoURI).append(" 有新的 release(s)\n");
        for (JsonObject object : updateRelease) {
            final JsonObject author = object.get("author").getAsJsonObject();

            final String releaseName = object.get("name").getAsString();
            final String branch = object.get("target_commitish").getAsString();
            final Date date = PARSE.parse(object.get("published_at").getAsString());
            final String authorName = author.get("login").getAsString();
            final String body = object.get("body").getAsString();
            final String url = object.get("html_url").getAsString();

            builder
                    .append("--------------------\n")
                    .append("名称: ").append(releaseName).append(" (").append(branch).append(")\n")
                    .append("时间: ").append(FORMAT.format(date)).append('\n')
                    .append("成员: ").append(authorName).append('\n')
                    .append("描述: \n").append(new String(body.getBytes("GBK"), StandardCharsets.UTF_8)).append('\n')
                    .append("link: ").append(url);
        }
        return builder.toString();
    }

    @Nullable
    private String buildCommits(String repoURI) throws ParseException {
        final List<JsonObject> updateCommits = APIHolder.checkUpdateCommits(repoURI);
        if (updateCommits.size() == 0) return null;
        StringBuilder builder = new StringBuilder();
        builder.append(repoURI).append(" 有新的 commit(s)\n");
        for (JsonObject object : updateCommits) {
            final String sha = object.get("sha").getAsString();
            final JsonObject commit = object.get("commit").getAsJsonObject();
            final JsonObject author = commit.get("author").getAsJsonObject();

            final Date date = PARSE.parse(author.get("date").getAsString());
            final String name = author.get("name").getAsString();
            final String email = author.get("email").getAsString();
            final String message = commit.get("message").getAsString();
            final String url = APIHolder.parseCommitHTML(repoURI, sha);

            builder
                    .append("--------------------\n")
                    .append("时间: ").append(FORMAT.format(date)).append('\n')
                    .append("成员: ").append(name).append(" (").append(email).append(")\n")
                    .append("描述: \n").append(message).append('\n')
                    .append("link: ").append(url);
        }
        return builder.toString();
    }

}
