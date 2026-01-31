package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.google.gson.Gson;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.pojo.TimedBlockingCache;
import com.illtamer.perpetua.sdk.Pair;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.distributed.ClientBroadcastCallbackEvent;
import com.illtamer.perpetua.sdk.event.distributed.ClientBroadcastEvent;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.util.Assert;
import lombok.NonNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DistributeHelper {

    private final TimedBlockingCache<String, String> blockingCache = new TimedBlockingCache<>(64);;

    // key: broadcastKey 广播数据唯一标识，如方法名, value: 对应的函数
    private final Map<String, Function> broadcastMap = new HashMap<>();
    private final String expansionName;
    private final Logger logger;

    public DistributeHelper(IExpansion expansion) {
        this.expansionName = expansion.getExpansionName();
        this.logger = expansion.getLogger();
    }

    /**
     * 尝试分发并处理分布式任务
     * @param dataConsumer 汇总数据 consumer
     * @param eConsumer 分布式分发异常 consumer
     * @param clazz 其他客户端返回数据的类型
     * @apiNote 如果存在其他支持此事件的客户端，则进行分发，否则自身正常调用
     * */
    public <T> void tryHandle(String param, Consumer<DistributeContext<T>> dataConsumer, Consumer<Exception> eConsumer, String broadcastKey, Class<T> clazz) {
        Function<String, T> function = (Function<String, T>) broadcastMap.get(broadcastKey);
        Assert.notNull(function, "No function connect broadcastKey: %s", broadcastKey);

        DistributeContext<T> context = new DistributeContext<>();

        Client client = StaticAPI.getClient();
        List<Client> clientList = OpenAPIHandling.getClientList();
        if (!checkStandalone(buildTag(expansionName, broadcastKey), client, clientList)) {
            return;
        }
        context.setClient(client);
        context.setClientList(clientList);

        Pair<List<T>, List<Client>> pair;
        try {
            pair = aggregateClientData(param, broadcastKey, clazz, client, clientList);
            List<T> dataList = pair.getKey();
            // 主节点也执行一次
            dataList.add(function.apply(param));
            context.setDataList(dataList);
            context.setFailedClientList(pair.getValue());
            dataConsumer.accept(context);
        } catch (Exception e) {
            logger.error("汇总客户端数据异常，请检查控制台输出", e);
            if (eConsumer != null) {
                eConsumer.accept(e);
            }
        }
    }

    /**
     * 创建客户端广播监听器
     * */
    public <T> Listener newBroadCastListener(@NonNull Function<String, T> function, @NonNull String broadcastKey) {
        broadcastMap.put(broadcastKey, function);
        return new Listener() {
            // 接收端监听
            @EventHandler
            public void onClientBroadcast(ClientBroadcastEvent event) {
                Client client = StaticAPI.getClient();
                Client sourceClient = event.getClient();
                // 广播一对多，需要筛选消息
                if (event.getData().equals(buildDataKey(sourceClient, client, buildTag(expansionName, broadcastKey)))) {
                    String json = new Gson().toJson(function.apply());
                    OpenAPIHandling.sendBroadcastDataCallback(json, event.getUuid(), sourceClient);
                }
            }

            // 发送端监听
            @EventHandler
            public void onClientBroadcastCallback(ClientBroadcastCallbackEvent event) {
                // 回调多对一，直接接收即可
                Client targetClient = event.getClient();
                blockingCache.put(buildCacheKey(targetClient, broadcastKey), event.getData());
            }
        };
    }

    /**
     * 判断当前模式
     * @param tag 根据其他客户端是否存在功能 Tag 校验客户端模式，建议 插件名+函数名
     * @return true - 单机模式; false - 主从模式，非主节点不响应指令
     * */
    private static boolean checkStandalone(String tag,
                                          Client client, List<Client> clientList) {
        // TODO Client要加一个tag，用tag判断是否为 群组互通模式&&有其他相同插件；如果没有，就切成单机模式
        if (!clientList.isEmpty() && !clientList.get(0).getAppId().equals(client.getAppId())) {
            return true;
        }
        return false;
    }

    /**
     * 收集其他客户端数据
     * @param broadcastKey 广播数据唯一标识，如方法名
     * @param clazz 广播数据类型
     * @return key: dataList, value: timedOutClientList
     * @throws WaitClientDataException 除超时外其他异常
     * */
    private <T> Pair<List<T>, List<Client>> aggregateClientData(String param, String broadcastKey, Class<T> clazz,
                                                                      Client client, List<Client> clientList) {
        List<Client> targetClientList = clientList.stream()
                .filter(c -> !c.getAppId().equals(client.getAppId())).collect(Collectors.toList());

        List<Client> failedClientList = new ArrayList<>();
        List<T> dataList = new ArrayList<>();
        if (!targetClientList.isEmpty()) {
            for (Client targetClient : targetClientList) {
                String dataKey = buildDataKey(client, targetClient, buildTag(expansionName, broadcastKey));
                OpenAPIHandling.sendBroadcastData(appendParam(dataKey, param), targetClient);
            }
            Gson gson = new Gson();
            for (Client targetClient : targetClientList) {
                try {
                    String json = blockingCache.get(buildCacheKey(targetClient, broadcastKey), 2, TimeUnit.SECONDS);
                    T data = gson.fromJson(json, clazz);
                    dataList.add(data);
                } catch (InterruptedException | TimeoutException e) {
                    failedClientList.add(targetClient);
                } catch (Exception e) {
                    throw new RuntimeException(targetClient.toString(), e);
                }
            }
        }
        return new Pair<>(dataList, failedClientList);
    }

    // dataKey 用于标识广播数据 expansionName#methodName#sClientId#tClientId
    private static String buildDataKey(Client sourceClient, Client targetClient, String tag) {
        return String.join("#", tag, sourceClient.getAppId(), targetClient.getAppId());
    }

    // cacheKey 缓存唯一键 tClientId#expansionName#methodName
    private static String buildCacheKey(Client targetClient, String broadcastKey) {
        return String.join("#", targetClient.getAppId(), broadcastKey);
    }

    // tag 校验客户端分布式模式标识 expansionName#methodName
    private static String buildTag(String expansionName, String broadcastKey) {
        return String.join("#", expansionName, broadcastKey);
    }

    private static String appendParam(String dataKey, String param) {
        return String.join("@@", dataKey, param); // dataKey@@null
    }

    private static Pair<String, String> parseParam(String dataKeyWithParam) {
        String[] splits = dataKeyWithParam.split("@@", 2);
        if (splits.length != 2) {
            return new Pair<>(dataKeyWithParam, null);
        }
        return new Pair<>(splits[0], splits[1]);
    }

}
