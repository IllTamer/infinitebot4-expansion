# MessageManager - 自定义消息附属

通过配置驱动的方式监听群消息，并按规则返回文本、执行指令或渲染图片卡片。

## 功能概览

- 支持关键词与正则表达式触发
- 支持 4 类消息形式
  - 纯文本消息
  - 指令消息（`console` / `player`）
  - 纯图片消息
  - 图文组合消息
- 支持 `reply` / 普通群消息发送模式
- 支持绑定校验 `only-bind`
- 支持机器人管理员校验与 LuckPerms 权限校验
- 支持在文本、指令和图片文字层中解析 PlaceholderAPI 变量
- 支持从外部目录加载图片与字体资源
- 自带 `web` 图片编辑器，可拖拽图层并导出 YAML 配置

## 配置文件

- [[config.yml]](./src/main/resources/config.yml)
- [[language-zh_CN.yml]](./src/main/resources/language-zh_CN.yml)

## 配置说明

### 资源目录

- `resources.image-dir`：图片资源目录（绝对路径）
- `resources.font-dir`：字体资源目录（绝对路径）

### 消息匹配规则

每个 `messages.<节点名>` 都是一条独立规则，插件会按配置顺序依次匹配；命中第一条规则后即停止继续处理。

每条规则可组合以下能力：

- `condition.regex`：正则触发
- `condition.keyword`：关键词完全匹配触发
- `permission.admin`：限制机器人管理员
- `permission.luckperms`：限制 LuckPerms 权限节点或继承组
- `only-bind`：限制仅已绑定玩家可触发
- `reply`：控制使用回复消息还是普通群消息

### 消息类型

- `text`：发送纯文本消息
- `command`：执行指令，`identity` 支持 `console` / `player`
- `image`：渲染图片消息
- `text` + `image`：组合消息，先发图片，再发文字

## 示例配置

```yml
messages:
  我的信息:
    reply: true
    condition:
      regex: ''
      keyword:
        - '我的信息'
    permission:
      admin: false
      luckperms: []
    only-bind: true
    image:
      background: 'info_bg.png'
      width: 800
      height: 600
      layers:
        - type: 'text'
          content: '玩家: %player_name%'
          x: 50
          y: 80
          font:
            file: 'MiSans-Regular.ttf'
            size: 28
            color: '#FFFFFF'
            style: 'bold'
        - type: 'image'
          file: 'avatar_frame.png'
          x: 600
          y: 30
          width: 150
          height: 150
```

## Web 图片编辑器

`message-manager/web` 是一个基于 Vue + Vite 的配套前端，用于辅助生成 `image.layers` 配置。

### 支持能力

- 批量加载本地图片与字体文件
- 可视化添加文字层 / 图片层
- 拖拽调整图层位置
- 实时预览画布
- 一键复制 YAML 配置

### 本地开发

```bash
corepack enable
corepack prepare yarn@4.12.0 --activate
yarn --cwd message-manager/web install --immutable
yarn --cwd message-manager/web dev
```

### 生产构建

```bash
yarn --cwd message-manager/web build
```

### 自动部署

仓库已配置 GitHub Pages 工作流；当 `message-manager/web/**` 或对应 workflow 文件在 `main` 分支更新后，页面会自动重新部署。

## 注意事项

- 使用 `permission.luckperms` 前，请确保服务端已安装 LuckPerms
- 使用 PlaceholderAPI 变量前，请确保服务端已安装 PlaceholderAPI
- `command.identity: player` 需要目标绑定玩家在线，否则不会执行玩家指令
- 图片与字体配置中的文件名需要与资源目录中的实际文件名保持一致
