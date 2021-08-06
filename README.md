# android-nest-scroll-ptr

[中文介绍](https://github.com/s1rius/android-nest-scroll-ptr/blob/master/README_CN.md)

This library implements pull-to-refresh logic and work with nested-scroll. It is easy to use and extend.

It is driven by a [StateMachine](https://github.com/Tinder/StateMachine)

<img src="images/transition.png" width="480" height="300" />

### Requirements

- minsdk 14/21(compose)
- kotlin | compose

### Features

- driven by a FSM
- work with nested-scroll
- Jetpack Compose implementation
- easy to customize the layout


### Demo

- Ins
- Wechat
- Wechat Moment
- NestedScroll Sample

<div>

<img src="images/ins.gif" width="160" height="346" />
<img src="images/wechat.gif" width="160" height="346" />
<img src="images/moment.gif" width="160" height="346" />
<img src="images/tab.gif" width="160" height="346" />
<img src="images/nestedscroll.gif" width="160" height="346" />
	
</div>	

### Use NSPtr in your application

- add the dependency

```
repositories {
    ...
    mavenCentral()
    ...
}

dependencies {
    ...
    // android view system implementation
    implementation "wtf.s1.ui:nsptr-view:x.x.x"
    // jetpack compose implementation
    implementation "wtf.s1.ui:nsptr-compose:x.x.x"
    ...
}
```

- use in Compose

```kotlin
val coroutine = rememberCoroutineScope()
val nsPtrState = remember {
    NSPtrState(
        coroutineScope = coroutine
    ) {
        // todo refresh block
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

- add a layout to view hierarchy

```kotlin
addView(
    NSPtrEZLayout(context).apply {
        addView(
            RecyclerView(context).apply {
                // add data and adapter
            },
            NSPtrLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 
                LayoutParams.MATCH_PARENT
            )
        )

        addPtrListener(object : NSPtrListener {
            override fun onRefreshing(ptrLayout: NSPtrLayout) {
                super.onRefreshing(ptrLayout)
                // do refresh logic
            }
        })
		// auto refresh
		isRefreshing = true
    },
    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
)
```

- or use in XML layouts

```xml
<wtf.s1.ui.nsptr.view.NSPtrEZLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</wtf.s1.ui.nsptr.view.NSPtrEZLayout>
```