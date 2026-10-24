package com.illtamer.infinite.bot.expansion.mvg.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * mvg 单版本资源准备状态
 *
 * @see <a href="http://127.0.0.1:4666/docs">GET /api/v1/resources/{version}</a>
 * */
public class MvgResourceState {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_FAILED = "failed";

    private final String version;
    private final String status;
    private final String stage;
    private final double progress;
    private final String error;
    private final List<String> warnings;

    private MvgResourceState(String version, String status, String stage, double progress,
                             String error, List<String> warnings) {
        this.version = version;
        this.status = status;
        this.stage = stage;
        this.progress = progress;
        this.error = error;
        this.warnings = warnings;
    }

    public static MvgResourceState parse(JsonObject object) {
        if (object == null) {
            return null;
        }
        List<String> warnings = Collections.emptyList();
        JsonElement warningElement = object.get("warnings");
        if (warningElement != null && warningElement.isJsonArray()) {
            JsonArray array = warningElement.getAsJsonArray();
            warnings = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                if (element != null && !element.isJsonNull()) {
                    warnings.add(element.getAsString());
                }
            }
        }
        return new MvgResourceState(
                string(object, "version"),
                string(object, "status"),
                string(object, "stage"),
                number(object, "progress"),
                string(object, "error"),
                warnings
        );
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static double number(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return 0D;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException e) {
            return 0D;
        }
    }

    public String getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public String getStage() {
        return stage;
    }

    public double getProgress() {
        return progress;
    }

    public String getError() {
        return error;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isReady() {
        return STATUS_READY.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    /**
     * 供日志输出的进度描述
     * */
    public String describe() {
        return String.format("%s(%s, %.0f%%)", stage.isEmpty() ? status : stage, status, progress * 100);
    }

    @Override
    public String toString() {
        return "MvgResourceState{version='" + version + "', status='" + status + "', stage='" + stage
                + "', progress=" + progress + ", error='" + error + "'}";
    }

}
