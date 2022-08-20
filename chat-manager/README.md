# Infinite Bot 3.0.3 - ChatManager(群与游戏消息互通)

互通支持
- @: 群内@成员/@全体成员时，若被@者在服务器内且绑定游戏玩家，则游戏中会同样@该玩家
- 回复: 若被回复QQ绑定玩家且在线，则同样会@该玩家
- 图片: 该功能需要开启 `expand-chat` 配置节点

## 指令

玩家在聊天界面输入 `#change`，即切换自身消息接收状态 `接收`/`屏蔽`

## 配置文件

[[config.yml]](src/main/resources/config.yml)

## 依赖

### 软依赖

- [view-manager](../view-manager) 支持游戏内查看图片

## 演示

### 群 -> 游戏

![](image/01-qq.png)

![](image/01-game.png)

### 游戏 -> 群

![](image/02-game.png)

![](image/02-qq.png)
