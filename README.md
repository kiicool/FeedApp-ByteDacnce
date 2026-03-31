Feed 类客户端解决方案文档参见：https://mcnio21fckrv.feishu.cn/wiki/EuXDwW5ztix8BykNsxNcDgmDnZe?from=from_copylink


本项目是一个仿信息流页面的 Android Demo，展示了：

- 下拉刷新与分页加载
- 文本卡片 / 图片卡片 / 视频卡片 / Banner 卡片
- 单列 / 双列混排（由“服务端数据”决定）
- 卡片曝光统计与调试页面
- 简单的本地缓存与视频自动播放

---

## 1. 运行方式

1. 使用 Android Studio 打开本项目目录。
2. 等待 Gradle 同步完成。
3. 直接运行到真机/模拟器。APK包位于app\src\main\java\com\example\feedapp\FeedApp.apk

---

## 2. 目录结构

```text
app/
└── src/
    └── main/
        ├── java/
        │   └── com/example/feedapp/
        │       ├── feed/
        │       │   ├── ui/
        │       │   │   ├── main/
        │       │   │   │   └── MainActivity.java
        │       │   │   ├── adapter/
        │       │   │   │   └── FeedAdapter.java
        │       │   │   └── factory/
        │       │   │       └── BannerCardFactory.java
        │       │   ├── data/
        │       │   │   ├── ContentRepository.java
        │       │   │   ├── LocalFeedCache.java
        │       │   │   └── MockDataGenerator.java
        │       │   ├── model/
        │       │   │   ├── ContentEntry.java
        │       │   │   └── FeedItem.java
        │       │   ├── exposure/
        │       │   │   ├── ExposureLogger.java
        │       │   │   └── ExposureManager.java
        │       │   └── player/
        │       │       └── VideoPlayerManager.java
        │       ├── debug/
        │       │   └── DebugExposureActivity.java
        │       └── FeedApp.apk
        └── res/
            ├── layout/
            │   ├── activity_main.xml
            │   ├── activity_debug_exposure.xml
            │   ├── item_text_card.xml
            │   ├── item_image_card.xml
            │   ├── item_video_card.xml
            │   ├── item_banner_card.xml
            │   └── item_load_more_footer.xml
            └── drawable/
                └── 一些背景渐变、占位图和本地图
```

---

## 3. 核心类说明

### 3.1 页面与入口

- **`MainActivity`**
  - App 主入口。
  - 初始化 RecyclerView、SwipeRefreshLayout、`FeedAdapter`，并注册 `BannerCardFactory`。
  - 处理下拉刷新、滚动到底部自动加载更多。
  - 创建并注册 `ExposureManager`、`VideoPlayerManager` 等核心组件。

- **`DebugExposureActivity`**
  - 曝光日志调试页面。
  - 从 `ExposureLogger` 读取曝光数据，列表展示每条卡片的曝光 / 消失事件，用于验证逻辑正确性。

---

### 3.2 列表与数据模型

- **`FeedAdapter`**
  - RecyclerView 的适配器。
  - 支持多种卡片类型（文本、图片、视频、Banner）。
  - 根据 `FeedItem.cardType` 返回不同的 `viewType`，创建相应 ViewHolder 并绑定数据。
  - 负责删卡（长按删除）、追加数据（分页加载）等列表操作。
  - 通过 `GridLayoutManager.SpanSizeLookup` 配合 `layoutType` 实现单列 / 双列混排。

- **`FeedItem`**
  - 单个卡片的数据模型。
  - 主要字段包括：
    - `cardType`：卡片类型（文本 / 图文 / 视频 / Banner）。
    - `layoutType`：布局类型（单列 / 双列）。
    - 标题、文案、图片 / 视频 URL 或本地资源等。
  - 由“伪服务端”生成并下发，前端只负责展示。

- **`ContentEntry`**
  - 更接近“服务端原始数据”的结构体。
  - 用于 `MockDataGenerator` 与 `ContentRepository` 之间的数据转换。
  - 提供从原始内容到 `FeedItem` 的映射信息，方便后期扩展字段。

---

### 3.3 数据获取与缓存

- **`ContentRepository`**
  - 作为静态内容仓库，维护图文/文本/视频样本池。
  - 对外提供按 index 读取内容与总量查询（`getEntry()` / `getTotalCount()`）。

- **`MockDataGenerator`**
  - 充当“伪服务端”。
  - 从 `ContentRepository` 取样本，按页生成 `FeedItem`。
  - 决定：
    - 每条内容的 `cardType`、`layoutType`；
    - 视频与 Banner 的插入节奏；
    - 图片 / 视频的 URL 或本地资源。

- **`LocalFeedCache`**
  - 列表数据的本地缓存实现。
  - 将 `List<FeedItem>` 序列化后写入 SharedPreferences。
  - 提供读取最近一次缓存，用于在“伪网络失败”时回退显示。

---

### 3.4 曝光统计与调试

- **`ExposureManager`**
  - 绑定在 RecyclerView 上，负责计算每张卡片的曝光状态。
  - 通过：
    - 监听滚动事件与滚动状态；
    - 计算每个 item 的可见比例（0～100%）。
  - 根据可见比例触发事件：
    - 首次露出 / 首次完全可见 / 离开可视区域。
  - 事件回调给业务层，并交给 `ExposureLogger` 记录。

- **`ExposureLogger`**
  - 简单的曝光日志记录器。
  - 记录字段包括时间、事件类型、卡片 ID、位置等。
  - 提供查询接口给 `DebugExposureActivity` 使用。

---

### 3.5 视频播放相关

- **`VideoPlayerManager`**
  - 统一管理视频播放逻辑。
  - 维护一个或少量播放器实例（如 ExoPlayer）。
  - 确保同一时间只有一个视频在播放。
  - 提供：
    - `play(VideoVH holder, String itemKey, String videoUrl)`：开始播放某个卡片的视频。
    - `stop()` / `release()`：停止并释放资源。
  - 配合 RecyclerView 的滚动状态，实现“停下后自动播放中间的视频卡片”。

---

### 3.6 卡片扩展示例

- **`BannerCardFactory`**
  - Banner 卡片的创建 / 绑定工厂示例。
  - 演示如何通过工厂 / 注册的方式，给 `FeedAdapter` 扩展新的卡片类型。
  - 后续要新增其它特殊卡片，可以参考这个模式。

---

## 4. 功能点概览

- 下拉刷新 & 分页加载更多
- 多卡片类型：文本 / 图文 / 视频 / Banner
- 单列 / 双列混排（由数据控制）
- 卡片长按删除
- 曝光统计与调试页面
- 本地缓存兜底
- 视频自动播放（中心卡片优先）

---

## 5. 包结构约定（当前）

- `feed/ui/main`：页面入口与列表编排
- `feed/ui/adapter`：RecyclerView 适配器
- `feed/ui/factory`：可扩展卡片工厂
- `feed/data`：内容源与缓存
- `feed/model`：数据模型
- `feed/exposure`：曝光统计
- `feed/player`：视频播放
- `debug`：调试页面

`common` 目录建议在出现跨业务复用代码时再引入，避免过早抽象。

---

如需更详细的设计和实现思路，可以参考项目说明文档或源码注释。

