package com.illtamer.infinite.bot.expansion.github.hook;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.api.util.HttpRequestUtil;
import com.illtamer.infinite.bot.expansion.github.GithubManager;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class APIHolder {

    private static final String REPO = "https://api.github.com/repos/%s";
    private static final String COMMIT = "https://api.github.com/repos/%s/commits";
    private static final String RELEASE = "https://api.github.com/repos/%s/releases";
    private static final Gson GSON = new Gson();

    public static List<JsonObject> checkUpdateCommits(String uri) {
        return checkUpdateNode("commit", "sha", COMMIT, uri);
    }

    public static List<JsonObject> checkUpdateRelease(String uri) {
        return checkUpdateNode("release", "node_id", RELEASE, uri);
    }

    @SuppressWarnings("unchecked")
    private static List<JsonObject> checkUpdateNode(String configKey, String node, String basicURL, String uri) {
        final Pair<Integer, String> pair = HttpRequestUtil.getJson(String.format(basicURL, uri), null);
        Assert.isTrue(pair.getKey() == 200, "Unexpect state code: " + pair.getKey());
        final JsonArray array = GSON.fromJson(pair.getValue(), JsonArray.class);

        final ExpansionConfig cacheFile = GithubManager.getInstance().getCacheFile();
        final FileConfiguration config = cacheFile.getConfig();
        ConfigurationSection repoData = config.getConfigurationSection("repo." + uri);
        if (repoData == null) {
            repoData = config.createSection("repo." + uri);
        }

        final String commitSHA = repoData.getString(configKey);
        final String latestSHA = array.get(0).getAsJsonObject().get(node).getAsString();
        if (commitSHA == null) {
            repoData.set(configKey, latestSHA);
            cacheFile.save();
            return Collections.singletonList((JsonObject) array.get(0));
        }
        if (commitSHA.equals(latestSHA)) return Collections.EMPTY_LIST;
        // update
        repoData.set(configKey, latestSHA);
        cacheFile.save();
        List<JsonObject> updates = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = (JsonObject) element;
            if (object.get(node).getAsString().equals(commitSHA)) break;
            updates.add(0, object);
        }
        return updates;
    }

    public static String parseCommitHTML(String uri, String sha) {
        return "https://github.com/" + uri + "/commit/" + sha;
    }

}
