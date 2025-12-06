Feed 类客户端解决方案文档参见：https://mcnio21fckrv.feishu.cn/wiki/EuXDwW5ztix8BykNsxNcDgmDnZe?from=from_copylink
1. 项目概述
本项目实现一个类今日头条 / 抖音搜索结果页的 Feed 流页面，核心目标：
- 所有内容由“服务端数据”驱动（本地 Mock 模拟）。
- 支持下拉刷新 / 无限加载更多 / 删卡。
- 支持多种卡片样式，且单列 / 双列混排由服务端控制。
- 实现精细的卡片曝光事件（露出、50%、完全露出、消失）和测试工具。
- 进阶挑战：本地缓存、视频自动播放、插件式卡片扩展、性能优化。
下面的设计主要围绕编码思路和项目挑战的解决方案展开。

---
2. 整体架构设计
2.1 模块划分
按职责将代码分为四层：
1. UI 层
  - MainActivity：承载 RecyclerView、SwipeRefreshLayout，处理刷新 / 加载更多 / 错误提示；作为入口控制整体生命周期。
  - DebugExposureActivity：作为“测试工具”，展示曝光日志，验证曝光事件是否准确。
2. 展示层（Adapter & ViewHolder）
  - FeedAdapter：统一管理多种卡片类型，内部持有若干 ViewHolder 子类（文本卡、图文卡、视频卡、Banner 卡等）。
  - 各 ViewHolder 只负责视图绑定和点击长按回调，不直接做数据拉取和业务逻辑。
3. 业务层（Domain / Manager）
  - ContentRepository：对外提供统一的数据接口（下拉刷新 / 加载更多），内部负责网络（Mock）+ 本地缓存的读写策略。
  - ExposureManager：监听 RecyclerView 滚动与子 View attach/detach，计算曝光状态并下发事件。
  - ExposureLogger：记录曝光事件日志，提供给 DebugExposureActivity 展示。
  - VideoPlayerManager：统一管理视频播放实例（播放器池、唯一当前播放卡片、自动播放 / 停止）。
4. 数据层
  - MockDataGenerator：模拟“服务端返回”，组装 FeedItem 列表（控制 cardType、排版方式、图片 / 视频 URL 等）。
  - ContentEntry / FeedItem：数据模型，承载卡片展示所需字段。
  - LocalFeedCache：负责本地缓存读写（例如 JSON + SharedPreferences / 文件），在网络失败时兜底。
5. 扩展 & 工具层
  - BannerCardFactory：示例性的“卡片工厂”，演示通过注册方式扩展新卡片类型。

---
3. 核心功能编码思路
3.1 列表展示与分页加载
核心类：MainActivity、FeedAdapter、ContentRepository、MockDataGenerator、FeedItem
- MainActivity 持有 RecyclerView 和 FeedAdapter，在 onCreate 内：
  - 设置 LayoutManager（如 GridLayoutManager），支持单列 / 双列。
  - 设置 adapter 为 RecyclerView 的适配器。
- 首次进入页面时调用：
repository.refresh(/* callback -> onNewData(List<FeedItem>) */);
- ContentRepository 对外暴露两个接口：
  - refresh()：重置页码，重新拉取第一页数据。
  - loadMore()：基于当前页码继续拉下一页。
- MockDataGenerator：
  - 内部维护一个“数据池”，每次根据页码随机 / 轮询生成一页 List<FeedItem>。
  - 决定：
    - cardType（文字 / 图文 / 视频 / Banner）
    - layoutType（单列 / 双列）
    - 图片和视频的 URL 或本地资源 ID。
数据更新策略
- 下拉刷新：清空当前列表，替换为新数据。
- 加载更多：在当前列表末尾 append 新数据。
- Adapter 使用 notifyItemRangeInserted / DiffUtil（如已实现）尽可能减少不必要的刷新。

---
3.2 下拉刷新功能
核心类：MainActivity、ContentRepository
- 在布局中使用 SwipeRefreshLayout 包裹 RecyclerView。
- MainActivity 注册监听：
swipeRefreshLayout.setOnRefreshListener(() -> {
    repository.refresh(result -> {
        adapter.setItems(result);
        swipeRefreshLayout.setRefreshing(false);
    }, error -> {
        showToast("刷新失败，尝试从本地缓存读取");
        swipeRefreshLayout.setRefreshing(false);
    });
});
- 时间复杂度：刷新一次只拉一页数据 O(n)，其中 n 为每页条数；列表渲染依赖 RecyclerView 复用，整体可控。

---
3.3 加载更多（loadMore）
核心类：MainActivity、ContentRepository
- 在 RecyclerView 上添加 OnScrollListener，当用户滑到底部一定距离时触发加载更多：
recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
        if (!rv.canScrollVertically(1) && !isLoadingMore) {
            loadMore();
        }
    }
});
- loadMore() 内：
  - 通过 ContentRepository.loadMore() 异步获取下一页。
  - 成功则 adapter.appendItems(newPage) 并刷新对应 range。
  - 失败则：展示“加载失败，点击重试”的 footer 或 Toast。
考虑的边界情况
- 连续快速滑动导致多次触发：用 isLoadingMore 标记位防抖。
- 没有更多数据：由 Repository 返回空列表，并在 UI 显示“没有更多内容”。

---
3.4 删卡功能（长按）
核心类：FeedAdapter、MainActivity
- 在每个 ViewHolder 的 itemView.setOnLongClickListener 中抛出回调给 Adapter，再由 Adapter 抛给 Activity：
public interface OnItemLongClickListener {
    void onItemLongClick(FeedItem item, int position);
}
- MainActivity 中实现回调：
  - 弹出 AlertDialog 确认是否删除。
  - 确认后调用：
adapter.removeItem(position);
- Adapter 内部维护 List<FeedItem>，删除后：
  - items.remove(position);
  - 调用 notifyItemRemoved(position)，并根据需要 notifyItemRangeChanged。

---
3.5 多种卡片样式 & 单/双列排版
核心类：FeedAdapter、FeedItem、ContentEntry、BannerCardFactory
1. 多卡片类型
  - FeedItem 内包含：
    - int cardType：如 CARD_TYPE_TEXT / IMAGE / VIDEO / BANNER 等。
    - 展示所需字段：标题、描述、图片 URL / 本地资源、视频 URL 等。
  - FeedAdapter.getItemViewType(int position)：
@Override
public int getItemViewType(int position) {
    return items.get(position).getCardType();
}
  - onCreateViewHolder 根据不同的 viewType inflate 不同布局，或者委托 BannerCardFactory 等工厂类创建。
2. 单列 / 双列混排
  - FeedItem 增加 int layoutType 字段（单列 / 双列）。
  - 使用 GridLayoutManager + SpanSizeLookup：
GridLayoutManager glm = new GridLayoutManager(context, 2);
glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
    @Override
    public int getSpanSize(int position) {
        FeedItem item = adapter.getItem(position);
        return item.isSingleColumn() ? 2 : 1;
    }
});
recyclerView.setLayoutManager(glm);
  - 这样可以由“服务端”（MockDataGenerator）控制每个卡片的排版方式，前端只做解释与呈现。

---
3.6 卡片曝光事件 & 测试工具
核心类：ExposureManager、ExposureLogger、DebugExposureActivity、MainActivity、FeedAdapter
3.6.1 曝光判定逻辑
- ExposureManager 挂在 RecyclerView 上：
  - 监听滚动：addOnScrollListener
  - 监听子 View attach/detach：addOnChildAttachStateChangeListener
- 每次滚动时：
  1. 通过 LayoutManager 获取当前 第一个 / 最后一个可见 position。
  2. 遍历这一段 position：
    - 通过 recyclerView.findViewHolderForAdapterPosition(pos) 拿到 itemView。
    - 计算可见高度：
      - 使用 getGlobalVisibleRect(rect) 与 itemView 本身高度做比例。
    - 得到 visibleRatio = visibleHeight / totalHeight。
  3. 按比例判定事件：
    - 0 < visibleRatio <= 0.5：卡片露出。
    - visibleRatio > 0.5 && visibleRatio < 1：露出超过 50%。
    - visibleRatio == 1：完整露出。
    - 从有可见变为 visibleRatio == 0：卡片消失。
- 为避免重复触发：
  - ExposureManager 内维护一个 Map<String, ExposureState>，
  - key 为 itemId（如 FeedItem 的唯一 ID），ExposureState 记录当前状态，
  - 只有当状态发生变化时才回调并记录。
3.6.2 曝光事件回调设计
- 定义监听接口：
interface ExposureListener {
    void onExpose(FeedItem item);
    void onHalfExpose(FeedItem item);
    void onFullExpose(FeedItem item);
    void onDisappear(FeedItem item);
}
- MainActivity 实现该接口（或注册一个单独的监听实现）：
  - 将事件同时交给：
    - 业务处理（例如上报埋点预留）
    - ExposureLogger 记录。
3.6.3 曝光测试工具
- ExposureLogger：
  - 内部维护一个 List<ExposureLog>。
  - 每次收到事件时写入 时间戳 / 事件类型 / itemId / position / 当前 visibleRatio。
- DebugExposureActivity：
  - 使用 RecyclerView 展示日志。
  - 数据直接从 ExposureLogger.getLogs() 读取。
  - 这样可以在 APP 内实时验证：
    - 滑动慢 / 快 / 快速滑过 / 暂停 等不同场景下，事件是否按照预期触发。

---
4. 进阶挑战与解决方案
4.1 本地数据缓存
核心类：LocalFeedCache、ContentRepository
需求：网络请求失败时，使用本地缓存进行展示。
设计思路：
1. 写缓存时机
  - 每次 refresh() 和 loadMore() 成功返回时，将最新数据写入本地：
LocalFeedCache.savePage(pageIndex, items);
  - 为简化实现，也可以只缓存“最近一页”或“首屏数据”。
2. 数据结构
  - 将 List<FeedItem> 序列化为 JSON 字符串存入：
    - SharedPreferences 或
    - 本地文件（如 /data/data/.../files/feed_cache.json）。
  - FeedItem 只包含基本字段（id、cardType、layoutType、文本、图片/视频 URL 等），避免包含 View 或大对象。
3. 读缓存流程
  - 当网络请求失败时：
    - ContentRepository 捕获异常，尝试：
List<FeedItem> cached = LocalFeedCache.loadLastPage();
if (cached != null && !cached.isEmpty()) {
    callback.onSuccess(cached, fromCache = true);
} else {
    callback.onError();
}
  - UI 层展示时，可根据 fromCache 参数：
    - 提示“当前为缓存数据，可能不是最新”。
4. 缓存失效策略（简化版）
  - 可按时间戳控制：超过 N 分钟认为过期，下次刷新时强制走网络。
  - 当前版本可先不实现复杂策略，仅保留一份最近缓存，满足作业要求。

---
4.2 视频卡片与自动播放
核心类：VideoPlayerManager、FeedAdapter（视频 ViewHolder）、MainActivity/ExposureManager
需求：卡片包含视频，且实现类似抖音搜索单列结果页的自动播放 / 停止。
4.2.1 播放器集中管理
- VideoPlayerManager：
  - 内部持有一个或少量 ExoPlayer 实例（如果实现了 Media3）。
  - 维护当前正在播放的 ViewHolder 引用。
  - 对外暴露方法：
void play(VideoVH holder, FeedItem item);
void stop(VideoVH holder);
void release();
  - play() 逻辑：
    - 如果当前已有正在播放的卡片，先调用 stop(oldHolder)。
    - 设置视频源（URL 或本地资源），绑定到 holder.playerView，启动播放。
4.2.2 自动选择播放卡片
- 在 RecyclerView 滚动静止（SCROLL_STATE_IDLE）时触发选择逻辑：
private void autoPlayCenterVideo() {
    int firstVisible = glm.findFirstVisibleItemPosition();
    int lastVisible = glm.findLastVisibleItemPosition();
    int rvCenterY = recyclerView.getHeight() / 2;

    FeedAdapter.VideoVH bestVH = null;
    int bestDistance = Integer.MAX_VALUE;

    for (int pos = firstVisible; pos <= lastVisible; pos++) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(pos);
        if (holder instanceof FeedAdapter.VideoVH) {
            int centerY = (holder.itemView.getTop() + holder.itemView.getBottom()) / 2;
            int distance = Math.abs(centerY - rvCenterY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestVH = (FeedAdapter.VideoVH) holder;
            }
        }
    }

    if (bestVH != null) {
        videoPlayerManager.play(bestVH, adapter.getItem(bestVH.getBindingAdapterPosition()));
    }
}
- 当列表再次滚动时，优先 stop 当前播放，再在新停下来的位置重新选中。
4.2.3 与曝光系统的配合
- 可选优化：仅当视频卡片“超过 50% 露出”时才允许播放。
- ExposureManager 在判定 onHalfExpose 时：
  - 如果是视频卡片，可将该事件传给 VideoPlayerManager 做一个候选标记。
- 结合“离中线最近”策略，确保不会播放边缘区域的小卡片。

---
4.3 卡片样式的插件式扩展
核心类：FeedAdapter、BannerCardFactory、FeedItem
目标：新增卡片类型时，不修改大量旧代码，只需要：
1. 新增一个数据 + 视图绑定类。
2. 在某个注册表中登记即可。
设计思路：
1. 定义通用接口：
interface CardBinder<VH extends RecyclerView.ViewHolder> {
    VH onCreateViewHolder(ViewGroup parent);
    void onBindViewHolder(VH holder, FeedItem item);
}
1. 在 FeedAdapter 内加一个注册表：
private final Map<Integer, CardBinder<?>> binderMap = new HashMap<>();

public void registerBinder(int cardType, CardBinder<?> binder) {
    binderMap.put(cardType, binder);
}
1. 初始化时注册内置卡片：
adapter.registerBinder(FeedItem.CARD_TYPE_TEXT, new TextCardBinder());
adapter.registerBinder(FeedItem.CARD_TYPE_IMAGE, new ImageCardBinder());
adapter.registerBinder(FeedItem.CARD_TYPE_VIDEO, new VideoCardBinder(videoPlayerManager));
adapter.registerBinder(FeedItem.CARD_TYPE_BANNER, BannerCardFactory.createBinder());
1. onCreateViewHolder / onBindViewHolder：
  - 根据 viewType 从 binderMap 取出对应 CardBinder；
  - 交给 binder 完成创建 / 绑定。
2. 扩展新卡片类型时：
  - 新增 FeedItem.CARD_TYPE_XXX 常量；
  - 新建 XXXCardBinder 类；
  - 在初始化时调用 adapter.registerBinder(...) 即可。
这样可以满足“插件式扩展”的要求，避免在 Adapter 内写越来越长的 switch-case。

---
4.4 性能优化（首屏 / 滑动 / 可感知加载耗时）
核心类：FeedAdapter、MainActivity、VideoPlayerManager、图片加载库（如 Glide）
4.4.1 首屏加载优化
- 懒加载 + 占位符
  - 首屏只请求一页（比如 10–15 条），降低首屏数据量。
  - 图片使用 Glide 等库时设置占位图，避免白块闪烁。
- UI 线程避免重计算
  - 在数据生成阶段（MockDataGenerator）完成 cardType / layoutType 等计算；
  - Adapter 的 onBindViewHolder 只做简单赋值和 Glide.load() 等轻量操作。
4.4.2 滑动流畅性
- 严格控制 onBindViewHolder 内逻辑：
  - 不做磁盘 IO 或网络请求；
  - 不做复杂图像处理；
  - 避免频繁 new 大对象（可复用 StringBuilder 等）。
- （如使用）setHasStableIds(true) + 合理实现 getItemId()，配合 DiffUtil，减少闪烁与重建。
4.4.3 图片预加载（可感知加载耗时）
- 在 OnScrollListener.onScrolled() 中，根据当前 lastVisiblePosition 提前拿出“预加载区域”的若干位置（如 +3 ～ +5）：
int preloadStart = lastVisible + 1;
int preloadEnd = Math.min(lastVisible + PRELOAD_COUNT, adapter.getItemCount() - 1);
for (int i = preloadStart; i <= preloadEnd; i++) {
    FeedItem item = adapter.getItem(i);
    if (item.hasImage()) {
        Glide.with(context).load(item.getImageUrl()).preload();
    }
}
- 这样用户滑到新卡片时，图片已基本在缓存中，减少“停一停等图片”的感觉。
4.4.4 视频预加载 / 播放性能
- 控制同一时间只播放一个视频，避免多路解码拖垮性能。
- 复用单一 ExoPlayer 实例，而不是为每个 ViewHolder 创建新的播放器。
- 只对“候选视频卡片”做 prepare()，真正播放时再 play()，以平衡内存与流畅性。

---
5. 后续可优化方向（简要）
- LocalCache 扩展为 Room 数据库，支持多种列表（推荐流 / 关注流）同时缓存。
- 视频卡片增加“静音自动播放 + 点击开启声音”的交互。
- 将曝光事件上报封装成可插拔的SDK，方便后期切换到真后端。
