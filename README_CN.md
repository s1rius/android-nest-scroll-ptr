# android-nest-scroll-ptr

本库实现了下拉刷新的逻辑，且实现了嵌套滑动接口。在嵌套滑动机制下正常工作。易于扩展和自定义。

由 [状态机](https://github.com/Tinder/StateMachine) 驱动

<img src="images/transition.png" width="480" height="300" />

### 支持环境

- minsdk 14/21(compose)
- kotlin | compose

### 特性

- 使用状态机实现
- 支持嵌套滑动
- 支持Jetpack Compose
- 易于扩展


### Demo

- instagram 刷新样式
- 微信首页小程序下拉
- 微信朋友圈下拉
- Android 官方嵌套滑动 demo 适配

<div>

<img src="images/ins.gif" width="160" height="346" />
<img src="images/wechat.gif" width="160" height="346" />
<img src="images/moment.gif" width="160" height="346" />
<img src="images/tab.gif" width="160" height="346" />
<img src="images/nestedscroll.gif" width="160" height="346" />

</div>	


### 开始使用

- 添加依赖

```
repositories {
    ...
    mavenCentral()
    ...
}
dependencies {
    ...
    // android view system 的依赖
    implementation "wtf.s1.ui:nsptr-view:x.x.x"
    // jetpack compose 的依赖
    implementation "wtf.s1.ui:nsptr-compose:x.x.x"
    ...
}
```

- 在Compose中使用

```kotlin
val coroutine = rememberCoroutineScope()
val nsPtrState = remember {
    NSPtrState(
        coroutineScope = coroutine
    ) {
        // todo 刷新逻辑
        it.dispatchPtrEvent(Event.RefreshComplete)
    }
}
NSPtrLayout(
    nsPtrState = nsPtrState,
    modifier = Modifier.fillMaxSize(),
) {
    NSPtrEZHeader(
        modifier = Modifier
            .offset(0.dp, 12.dp),
        nsPtrState = nsPtrState
    )
    LazyColumn(Modifier.ptrContent()) {
        items(10) { index ->
            // todo
        }
    }
}
```

- 用代码实现

```kotlin
addView(
    NSPtrEZLayout(context).apply {
        addView(
            RecyclerView(context).apply {
                // 添加数据和adapter
            },
            NSPtrLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 
                LayoutParams.MATCH_PARENT
            )
        )

        addPtrListener(object : NSPtrListener {
            override fun onRefreshing(ptrLayout: NSPtrLayout) {
                super.onRefreshing(ptrLayout)
                // 下拉刷新开始，请求网路
            }
        })
		// 自动刷新
		isRefreshing = true
    },
    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
)
```

- 用XML实现

```xml
<wtf.s1.ui.nsptr.view.NSPtrEZLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</wtf.s1.ui.nsptr.view.NSPtrEZLayout>
```