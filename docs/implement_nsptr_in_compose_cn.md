# 使用 Compose 实现下拉刷新控件

### 定义下拉刷新状态
在 Compose 中，状态是基础，所有的 UI 都是对当前状态的一种展示，状态改变驱动 UI 改变。根据之前使用 View 实现下拉刷新的经验，给出状态的定义

```kotlin
@Stable
class NSPtrState(
    val contentInitPosition: Dp = 0.dp,// 初始位置
    val contentRefreshPosition: Dp = 54.dp,// 刷新位置
    val pullFriction: Float = 0.56f,// 拖动的摩擦力参数
    coroutineScope: CoroutineScope, 
    onRefresh: (suspend (NSPtrState) -> Unit)? = null, // 触发刷新的回调
) {
    ...
    // 当前 content view 所处的位置
    var contentPositionPx: Float by mutableStateOf(0f)

    // 最近一次状态转变的对象
    var lastTransition: StateMachine.Transition<State, Event, SideEffect>? = null
    
    // 内部用来处理状态转变逻辑的状态机
    private val _stateMachine = createNSPtrFSM {}

    // 当前的下拉刷新状态
    var state: State by mutableStateOf(_stateMachine.state)

    // 触发状态转变事件
    fun dispatchPtrEvent(event: Event) {
        _stateMachine.transition(event)
    }

    // content view 的位移方法
    private suspend fun animateContentTo(
        value: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec()
    ) {
        // 动画实现
    }
    ...
}

```


### 了解 Compose 中实现自定义控件的流程
一般自定义 ViewGroup 控件在 View 中的实现步骤分为以下的几个步骤

1. 继承 ViewGroup
2. 重写 onMeasure 和 onLayout 方法

这两个步骤在 Compose 中都有如下的对应

1. 创建 Composable 方法，并在方法中调用 Layout() 方法

参照 Android codelabs 的[示例](https://developer.android.com/codelabs/jetpack-compose-layouts#6),  创建 NSPtrLayout 的 Composable 方法

```kotlin
@Composable
fun NSPtrLayout(
	modifier: Modifier,
	content: @Composable () -> Unit
) {
    ...
	Layout(
		modifier: Modifier = Modifier,
		measurePolicy: MeasurePolicy,
    	content: @Composable () -> Unit,
	)
    ...
}
```

追踪 Layout 代码块的具体实现

```kotlin
@Composable inline fun Layout(
    content: @Composable () -> Unit, // 子控件代码块 
    modifier: Modifier = Modifier, // 布局修饰符
    measurePolicy: MeasurePolicy // 测量和布局策略符
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
        },
        skippableUpdate = materializerOf(modifier),
        content = content
    )
}
```
获取了当前的像素密度，布局方向。最后用 LayoutNodes 去创建一个树形结构实现 UI。
忽略掉底层的实现细节，实现控件的测量和布局的模版方法。

2. 实现测量和布局
   
在 View 的系统中，我们需要重写两个方法，onMeasure 和 onLayout 来实现这个过程。在 Compose 中，我们需要自定义 MeasurePolicy 。

```kotlin
internal fun ptrMeasurePolicy() = MeasurePolicy { measurables, constraints ->
    if (measurables.isEmpty()) {
        return@MeasurePolicy layout(constraints.minWidth, constraints.minHeight) {}
    } else {
        val layoutWidth: Int = constraints.maxWidth
        val layoutHeight: Int = constraints.maxHeight
        val placeables = arrayOfNulls<Placeable>(measurables.size)

        measurables.forEachIndexed { index, measurable ->
            placeables[index] = measurable.measure(constraints)
			   // 测量逻辑
        }

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                	// layout 逻辑
					placeable.place(x, y)
            }
        }
    }
}
```

measurables 是所有子节点的约束合集，通过 measure 确定具体的宽高，返回 Placeable。对应 View 的 onMeasure() 流程就执行完了。
MeasurePolicy 的 measure 方法需要一个 MeasureResult 的返回值，需要调用 layout 方法。
Layout 方法确定子节点的布局位置，返回 MeasureResult

```kotlin
fun layout(
    width: Int,
    height: Int,
    alignmentLines: Map<AlignmentLine, Int> = emptyMap(),
    placementBlock: Placeable.PlacementScope.() -> Unit
) = object : MeasureResult {
    override val width = width
    override val height = height
    override val alignmentLines = alignmentLines
    override fun placeChildren() {
        Placeable.PlacementScope.executeWithRtlMirroringValues(
            width,
            layoutDirection,
            placementBlock
        )
    }
}
```

我们需要自定义下拉控件的位置，需要实现这个 placementBlock 函数，可以拿到之前在 measure 中存储的所有 Placeable 对象，遍历进行布局。

```kotlin
layout(layoutWidth, layoutHeight) {
    placeables.forEachIndexed { index, placeable ->
        placeable.place(x, y)
    }
}
```

到这里，基本的 Compose 中的自定义 Layout 的流程就结束了。

### 手势事件的分发和处理

首先明确目的，在这里我们只关心 down 事件和 up/cancel 事件，不对手势事件做拦截处理。

Compose 中的手势事件的源头是 AndroidComposeView 的 dispatchTouchEvent 方法，通过 PointerInputEventProcessor 桥接，最终调用 Modifier 中定义的 PointerInputScope 的 suspend 扩展方法。先不考虑实现原理，实现模版代码。

```kotlin
suspend fun PointerInputScope.detectDownAndUp(
    onDown: (Offset) -> Unit,
    onUpOrCancel: (Offset?) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            // 首次 down 事件触发
            awaitFirstDown(false).also {
                onDown(it.position)
            }

            val up = waitForUpOrCancel()
            // 所有的手指都离开屏幕，最后触发的 up 或者 cancel 事件
            onUpOrCancel.invoke(up?.position)
        }
    }
}
```

这样，我们下拉刷新所需要处理的手势事件就结束了

### 位移动画的实现
Compose 中的 SuspendAnimation.kt 提供了类似 ValueAnimtor 的实现，下面实现了 NSPtrState 中对 content view 位置属性的动画

```kotlin
private suspend fun animateContentTo(
        value: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec()
) {
    var prevValue = 0f // 存储上一个动画的值
    animate(
        0f,
        (value - contentPositionPx),
        animationSpec = animationSpec
    ) { currentValue, _ ->
        // 获取差值，加给目标值
        contentPositionPx += currentValue - prevValue
        prevValue = currentValue
    }
}
```

本文简单的叙述了下拉刷新控件的实现步骤，后续还需要将这些步骤进行组合，在布局中响应状态中的字段。具体的实现细节可以移步[源码](https://github.com/s1rius/android-nest-scroll-ptr)。