# PlaceholderHook - papi变量拓展附属

> ib4 提供的变量使用格式为: `%固定前缀(ib4)_子标识符[_参数]%`  
> 其中 `[_参数]` 部分因变量而异

## 变量列表

本附属自带部分基础变量，其他附属也可基于本附属便捷[**注册变量**](./src/main/java/com/illtamer/infinite/bot/expansion/hook/papi/PHandlerEnum.java)

- `%ib4_get-bind-qq%` 获取玩家绑定的 qq
- `%ib4_check-in-group_{group_id}%` 检查玩家是否在机器人也存在的某个群组中
- `%ib4_get-group-card_{group_id}%` 获取玩家在指定群组中的群名片

## 演示

### 传参变量

![](./images/get-group-card.png)