# MvgExpansion - 图像渲染

把游戏内的真实数据交给 [minecraft-view-generator](https://github.com/IllTamer/minecraft-view-generator)（mvg）
渲染成原版界面图片，再回发到 QQ。附属通过 HTTP 调用 mvg，因此渲染服务可以与服务器分离部署。

## 前置条件

1. 部署并启动 mvg 渲染服务（默认监听 `0.0.0.0:4666`），地址在 `mvg.base-url` 中配置；
2. 首次启用时附属会向 mvg 请求准备资源并轮询进度，**资源就绪前查询会收到"正在预热"的提示**；
3. mvg 的渲染接口固定使用 Minecraft `1.21.11` 的资源，服务器版本不同时依然按该版本渲染
   （启动日志会给出提示），请保证 mvg 已完成 `1.21.11` 的资源准备；
4. 附属按 mvg **当前接口**发送报文：`options.scale`、tooltip 正文走 `item.lore`、
   自定义物品走**物品级** `item.customData`。旧版 mvg 会忽略这些字段（放大不生效、tooltip 只剩标题），
   升级附属时请同步升级 mvg。

## 关键词列表

|  关键字   | 说明                          | 权限等级 |
|:------:|:----------------------------|:----:|
| 查看背包   | 渲染绑定玩家的背包界面（含 3D 玩家模型、装备与手持物） |  无   |
| 查看主手   | 渲染绑定玩家主手物品的 tooltip          |  无   |
| 查看副手   | 渲染绑定玩家副手物品的 tooltip          |  无   |
| 查看末影箱  | 渲染绑定玩家的末影箱                  |  无   |
| 查看在线   | 渲染全服在线玩家列表（支持 Velocity 群组服）  |  无   |

群聊与私聊均可使用，群聊中会以"回复 + @"的形式发送图片。

除 `查看在线`（始终渲染全服列表，不接受参数）外，管理员可在关键词后追加玩家名查询他人，
例如 `查看背包 Steve`。

## 配置文件

[[config.yml]](./src/main/resources/config.yml) ｜ [[language-zh_CN.yml]](./src/main/resources/language-zh_CN.yml)

关键配置项：

- `mvg.base-url` / `mvg.api-key`：mvg 服务地址与鉴权 Key（与服务端 `API_KEY` 环境变量一致，留空表示未开启鉴权）；
- `warmup.*`：资源预热的轮询间隔、超时与失败重试；
- `skin.*`：皮肤缓存有效期、进服刷新与来源指纹校验、Mojang 档案查询、贴图下载地址模板；
- `key-word.*`：五个功能的触发词，注释掉任意一项即可关闭对应功能；
- `render.scale.*`：四类视图各自的成品图放大倍率，默认都是 `2`（`0`/`1` 表示不放大，允许小数，上限 16）。
  各视图的原生画布：背包 176×166、末影箱 176×167、tooltip 宽 360 高由内容决定、
  在线列表本身已按 2 倍渲染（再叠加 2 倍等于 4 倍文字）；
- `render.itemsadder`：是否把自定义模型数据作为 `item.customData` 传给 mvg（默认开启）；
- `render.custom-data`：`customData` 的下发范围 —— `always`（默认，带正整数 `custom_model_data`
  就下发，由 mvg 按「材料 + 数值」查索引）或 `itemsadder`（只下发 ItemsAdder API 判定为自定义物品的物品，
  混合服会漏掉复用数值的物品）；
- `render.tooltip-mode`：`chinese` 为附属自绘中文 tooltip，`vanilla` 则交给 mvg 按原版顺序生成
  （附魔与属性行会显示英文）；
- `render.image-upload-mode`：`base64` 直接内联发送（bot 与服务器不同机时使用），
  `file` 先落盘再把绝对路径交给 bot（同机部署时更省带宽）。

## 皮肤来源

按以下顺序解析，全部结果都会转成 mvg 要求的内联 64×64 PNG base64 并缓存：

1. 内存 / 磁盘缓存（`plugins/.../MvgExpansion/skins/`，默认 7 天过期）；
2. [SkinsRestorer](https://www.spigotmc.org/resources/skinsrestorer.2124/) 的开发者 API
   （懒探测 + 30 秒节流重试，并按服务器真实的正版/离线模式查询，离线服也能取到皮肤）；
3. 在线玩家的 `PlayerProfile`（正版验证服）；
4. **Mojang 档案**（`skin.mojang-lookup`，按玩家名查询）：离线服且 SkinsRestorer 只装在代理端时，
   这是拿到真实皮肤的主力来源；
5. 内置的 Steve 默认皮肤（slim 模型使用 slim 版本）。

> mvg 拒绝一切网络图片地址与尺寸不符的贴图，因此附属负责下载与尺寸校验。
> 下载按 `skin.texture-hosts` 的模板依次尝试（`{hash}` / `{uuid}` / `{name}` 占位），
> 国内线路访问 `textures.minecraft.net` 不稳时可加镜像（如 `https://mc-heads.net/skin/{uuid}`）。

**换肤后不会一直显示旧皮肤**：缓存会记录来源指纹（texture hash），进服解析时用
`skin.verify-on-join` 校验，指纹变了就重新下载，不必等 `cache-days` 到期；
升级到本版本后的首次解析会为"没有指纹的历史缓存"重下一次（每名玩家几 KB，仅进服时发生），
下载失败时仍沿用旧缓存；来源临时不可用（网络抖动、插件未就绪）时也不会退回默认皮肤。

**看不懂为什么是默认皮肤时**：日志里会有一条带完整原因链的告警，例如
`[Skin] Wing 未能解析到真实皮肤（skinsrestorer=未接入, profile=档案无纹理, mojang=Mojang 查询失败(ConnectException)）→ 使用内置默认皮肤`，
启动时还会打印皮肤来源顺序。客户端对应的皮肤标记与刷新记录见日志里 `[Skin]` 开头的行。

采集实时数据时优先读内存缓存，未命中会回落到磁盘缓存（单张皮肤只有几 KB），
只有内存和磁盘都没有时才使用内置默认皮肤。附属被 `/ib4 load` 重载后，
内存缓存会被清空，因此启用时会异步把在线玩家的磁盘缓存回填到内存
（日志：`[Skin] 已从磁盘预热 m/n 名在线玩家的皮肤缓存`），不会出现"重载后全员变 Steve"。

## 自定义物品（ItemsAdder）

物品只要带正整数 `custom_model_data`，附属就把 `item.customData`（该数值）连同基础材料传给 mvg，
由 mvg 按用户包里的贴图与模型几何绘制。`render.itemsadder: false` 可强制关闭。

> mvg 只认**物品级** `item.customData`：`item.customModelData`、`components` 里的
> `minecraft:custom_model_data` 与 snake_case 写法都会被静默忽略（实测给同一个物品带上
> `customModelData` 与完全不带，渲染结果逐字节相同），附属因此只下发 `customData`。
>
> **默认 `render.custom-data: always`（不查 ItemsAdder API）**。索引键是「基础材料 + 数值」，
> mvg 查不到就按原版材质渲染并记一条 `WARN`，所以不需要先替 mvg 判断"这是不是自定义物品"。
> 混合服尤其不能查：钓鱼、MMO 武器等插件常常只是复用 ItemsAdder 的 `custom_model_data`
> 数值来吃它的贴图，物品本身并不由 ItemsAdder 创建，`ItemsAdder.isCustomItem()` 对这些物品
> 一律返回 `false` —— 客户端看得见自定义贴图，图片里却是原版材质。
> 只有确实想排除"其它插件写的数值碰巧撞上索引"时，才把 `render.custom-data` 设为 `itemsadder`，
> 那时被跳过的物品会按「材料 + 数值」各打一条一次性日志（`[Items] ... 已跳过 customData`），
> 启动时也会打印一条告警。
>
> **升级注意**：配置里显式写着 `custom-data: itemsadder` 的服务器不会被新默认值影响，
> 需要手改成 `always`（或删掉这一行、让默认值生效）。启动日志会打印当前生效的 `customData 下发范围`。

mvg 侧需要准备 ItemsAdder 的数据（与客户端看到的一致）：

```sh
# mvg 数据目录（docker 部署时为挂载的 ./data）
cp -r <服务器>/plugins/ItemsAdder/contents/* data/contents/
cp <服务器>/plugins/ItemsAdder/storage/items_ids_cache.yml data/items_ids_cache.yml
curl -X POST http://<mvg>/api/v1/resources/custom/reload   # 改完包不重启也能生效
```

索引键是「基础材料 + 数值」，因此附属必须传真实材料（`minecraft:paper` 而不是物品显示名）；
未命中（材料不匹配、索引里没有、资源缺失）时 mvg 会回退原版材质并记一条 `WARN`，HTTP 仍是 200。

## 群组服说明

跨服数据通过 ib4 的分布式事件采集：主节点收到消息后向所有子服广播，
只有玩家真正所在的子服会回传实时数据（背包 / 末影箱 / 手持物 / 皮肤 base64），
在线列表则合并所有子服的结果后统一渲染。

皮肤在**玩家进服时**就异步写入缓存；广播回包（等待上限 3 秒）里只读内存/磁盘缓存，
不会发起任何网络下载。

**跨服不可用时自动降级为单机查询**：若广播链路本身报错（例如本体版本不匹配导致的链接错误），
或部分子服没有在超时前响应，附属会直接采集**当前所在子服**的数据继续渲染，
并额外提示"以下为 XX 的数据"，而不是整条请求无响应。
子服侧收到的广播一律回包（采集异常也会回传 error 回包），不会让主节点空等到超时。

> 附属不依赖 ib4 `4.2.0` 新增的 `MinecraftScheduler`：运行时先反射探测该类，
> 探测不到（`4.1.x` 等旧版本体）就回退到 Bukkit 主线程调度器。
> 因此升级本体前后附属均可正常采集实时数据，无需同步升级。

## 常见问题

- **一直提示"正在预热"**：确认 mvg 服务已启动且 `mvg.base-url` 可访问，日志中会打印资源准备进度；
- **玩家皮肤是默认皮肤**：看启动时的"皮肤来源顺序"和 `[Skin] ... 未能解析到真实皮肤（...）→ 使用内置默认皮肤`
  里的原因链 —— 依次确认 SkinsRestorer 是否后端可用、玩家名能否查到 Mojang 档案、
  `skin.texture-hosts` 里的地址是否可达；皮肤缓存已超过 `skin.cache-days` 也会重新解析；
- **图片没有变大**：mvg 是旧版本，会忽略 `options.scale`，请升级 mvg 或把 `render.scale.*` 当作无效配置；
- **tooltip 只有物品名**：同样说明 mvg 版本过旧 —— 当前接口的正文只认 `item.lore`，旧版只认已删除的 `lines`；
- **tooltip 缺属性区块/行序与原版不一致**：中文模式的行顺序为 附魔 → lore → 属性 → 单值组件，
  属性取自 `ItemMeta`，物品定义自带的属性（普通剑的「在主手时」）会回落到物品类型默认值；
  附魔附带属性（如横扫之刃的横扫伤害比率）Bukkit 未暴露、无法还原；
- **tooltip 里的十六进制颜色显示异常**：`§x§R§R§G§G§B§B` 需要 mvg 侧支持，
  当前线上 mvg 只认 16 个传统色码，升级 mvg 后即可正常显示（附属侧已保证 hex 文本原样透传）；
- **ItemsAdder 物品显示成原版材质**：先看日志有没有 `[Items] ... 已跳过 customData`——
  有就说明 `render.custom-data` 被设成了 `itemsadder`，而该物品只是复用 ItemsAdder 数值
  （`isCustomItem()` 判定为 false），改回 `always` 即可；没有则确认 mvg 侧 `data/contents/` 与
  `data/items_ids_cache.yml` 与服务器一致，改完后调用 `POST /api/v1/resources/custom/reload`，
  并查看 mvg 日志里的 `WARN`（会写明是"索引里没有该材料+数值"还是"包里没人配置这个物品"，
  每个键只记一次）；
- **在线列表图片很大**：可调小 `render.playerlist-max-players`、把 `render.scale.playerlist` 调回 `1`，
  或在人数较多时关闭 `render.online-include-skin`；
- **「获取客户端 xxx 数据超时」**：某个子服没有回包，请确认该子服的附属已启用且
  `[Distribute]` 日志没有异常；此时查询会自动降级到本服数据并给出提示。

## 开发者：离线沙箱 / 端到端验收

无需启动 Minecraft 服务器即可验证与 mvg 的整条链路，并对**当前接口**逐项断言：

```sh
# 默认连 http://127.0.0.1:4666
./gradlew :mvg-expansion:sandbox
# 指定地址
./gradlew :mvg-expansion:sandbox -PmvgUrl=http://127.0.0.1:14666
# 用真实皮肤验证 base64 是否真的被 mvg 使用（任意 64x64/64x32 PNG）
./gradlew :mvg-expansion:sandbox -Pskin=/path/to/skin.png
# 验证 ItemsAdder 自定义物品（取值来自 items_ids_cache.yml，材料 + 数值）
./gradlew :mvg-expansion:sandbox -PiaMaterial=paper -PiaCustomData=11257
```

覆盖内容：

| 检查 | 断言 |
| --- | --- |
| 皮肤缓存 | 内存清空后仍能从磁盘回落，不退回默认皮肤 |
| 皮肤指纹 | 指纹不变复用缓存、换肤（指纹变化）后重新下载、来源不可用时保留旧缓存 |
| 玩家背包 | 默认皮肤 352×332（176×166 ×2）；真实皮肤与默认皮肤渲染结果不同，且差异只出现在玩家预览区 |
| 末影箱 | 352×334（176×167 ×2） |
| tooltip | 只有标题 102×64；标题 + 5 行正文 214×168（正文行高 10px 被保留）；行序为附魔 → lore → 属性 → 单值组件 |
| 属性行 | 攻击伤害/攻击速度合并基础值（「8 攻击伤害」「1.6 攻击速度」），其它属性保持 `+`/`-` |
| 自定义物品 | `customData` 写在物品级、不额外下发 `customModelData`，`0`/负值/无值不写字段（两个模式与总开关逐分支断言）；渲染结果与同材料原版物品不同，且 mvg 索引 `resolved` 计数 > 0 |
| hex 文本 | `&#RRGGBB` 与 `&x&R&R&G&G&B&B` 均转成 `§x§R§R§G§G§B§B`，且 `strip` 后得到纯文本 |
| 在线列表 | 3 人 720×196（360×98 ×2） |

每次请求的报文都会写入 `mvg-expansion/build/sandbox/*.json`，图片写入同名 `.png`，
可以直接用 curl 复跑：

```sh
curl -X POST http://127.0.0.1:4666/api/v1/render \
  -H 'Content-Type: application/json' \
  --data-binary @mvg-expansion/build/sandbox/player-inventory-default.json \
  -o /tmp/inventory.png
```

任一断言失败时 `sandbox` 任务会以非 0 退出。
