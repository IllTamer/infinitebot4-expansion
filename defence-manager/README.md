# Infinite Bot 3 - DefenceManager(白名单登录验证码功能) 1.0

## 指令

> Notice: `[]` 表示监听范围 `()` 表示权限等级

- 清除 IP 访问记录 `[任意]` `(管理)`

    管理员在群内发送 '清空缓存 <数值>'，可清空数值以内的所有攻击者记录(允许尝试加入服务器

     
## Notice

- 为防止机器人被屏蔽无法回复群消息，该验证可与机器人发起临时会话/私聊完成
- 验证码长度会随着短期内玩家尝试登录次数增加，初始为 4，最高可达 6


## 配置文件

[[config.yml]](src/main/resources/config.yml)

## 演示

### 白名单拦截

![defence](image/defence.png)

### Q群验证

![verify](image/verify.png)