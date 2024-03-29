# 状态机(StateMachine)在下拉刷新控件中的使用

[android-nest-scroll-ptr](https://github.com/s1rius/android-nest-scroll-ptr)

### 背景

在维护下拉刷新控件的过程中，虽然改动的次数很少，但是每一次bug修复和新增功能都很困难，总结问题如下：

- 状态转换，状态判断逻辑复杂

代码中定义了4个下拉刷新的状态以及可以影响刷新状态的其他变量，在手势事件，嵌套滑动，自动刷新等事件的输入情况下，需要对几个变量进行正确判断，再做出响应。

```kotlin
private fun updatePos(change: Int) {
    ...
    // 转换状态的同时，需要进行状态的判断
    // leave initiated position or just refresh complete
    if (mPtrStateController!!.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) {
        changeStatusTo(PTR_STATUS_PREPARE)
    }
    ...
    // Pull to Refresh
    if (mStatus == PTR_STATUS_PREPARE) {
        // reach fresh height while moving from top to bottom
        if (!isAutoRefresh && isPullToRefresh
            && mPtrStateController!!.crossRefreshLineFromTopToBottom()
        ) {
            tryToPerformRefresh()
        }
    }
    ...
}
```
事件产生，状态检查，状态转换，触发动作的代码都耦合在一起。给代码理解，debug，修复问题都增加了障碍。


- 由于判断逻辑复杂，可读性变差，冗余方法来让代码的可读性更好

```
/**
 * just make easier to understand
 */
private fun tryScrollBackToTopWhileLoading() {
    tryScrollBackToTop()
}

/**
 * just make easier to understand
 */
private fun tryScrollBackToTopAfterComplete() {
    tryScrollBackToTop()
}

/**
 * just make easier to understand
 */
private fun tryScrollBackToTopAbortRefresh() {
    tryScrollBackToTop()
}
```

有没有一套机制，能够可以避免阅读长段的 if else 逻辑检查，正确的进行状态变化，并且让代码简洁，可维护性高？

随即想到游戏开发，游戏中的各种状态变化的复杂度会高出几个等级，如何在多个玩家进行位移，技能释放等多种事件输入的场景下来保证逻辑正常运转的？如果只是靠简单的if，else判断，那对维护者来说，绝对是个灾难。
搜索一番，我看到了游戏开发中对状态机使用的一些介绍文章。觉得状态机同样适用在下拉刷新控件中。

### 什么是状态机

> 维基百科
> 
有限状态机（英语：finite-state machine，缩写：FSM）又称有限状态自动机（英语：finite-state automation，缩写：FSA），简称状态机，是表示有限个状态以及在这些状态之间的转移和动作等行为的数学计算模型。

简单的理解，状态机的作用就是，给定一个当前状态和触发事件，状态机能够输出下一个状态。

状态机的几个概念

- 状态（State）：表示对象的某种形态，在当前形态下可能会拥有不同的行为和属性。
- 转移（Transition）：表示状态变更，并且必须满足确使转移发生的条件来执行。
- 动作（Action/SideEffect）：表示在给定时刻要进行的活动。
- 事件（Event）：事件通常会引起状态的变迁，促使状态机从一种状态切换到另一种状态。

### 引入状态机
幸运的是，Kotlin社区已经有了状态机的[开源实现](https://github.com/Tinder/StateMachine)

接下去的任务就是重新梳理下拉刷新中所包含的状态，事件，动作。创建一个状态机。

```kotlin
sealed class State {
    // 空闲状态
    object IDLE : State()
    // 刷新状态
    object REFRESHING : State()
    // 拖动状态
    object DRAG : State()
}
```

能够造成这些状态更改的事件有

```kotlin
sealed class Event {
    // 下拉事件
    object Pull : Event()
    // 放手回到空闲
    object ReleaseToIdle : Event()
    // 放手开始刷新
    object ReleaseToRefreshing : Event()
    // 刷新完成事件
    object RefreshComplete : Event()
    // 自动刷新事件
    object AutoRefresh : Event()
}
```
状态转换时，执行的动作

```kotlin
sealed class SideEffect {
        // 手势取消回到顶部
        object OnReleaseToIdle : SideEffect()
        // 刷新动作
        object OnRefreshing : SideEffect()
        // 开始拖动控件
        object OnPull : SideEffect()
        // 响应刷新完成
        object OnComplete : SideEffect()
    }
```

状态机的定义如下

```kotlin
var stateMachine =
    StateMachine.create<State, Event, SideEffect> {
        // 初始化状态
        initialState(State.IDLE)

        state<State.IDLE> {
            // 当前状态下响应的事件
            on<Event.Pull> {
                // 转换的目标状态，以及转换所触发的动作
                transitionTo(State.DRAG, SideEffect.OnDragBegin)
            }
            on<Event.AutoRefresh> {
                transitionTo(State.REFRESHING, SideEffect.OnRefreshing)
            }
        }
        
        // 刷新状态
        state<State.REFRESHING> {
            // 定义刷新状态下事件触发产生的状态转移
            ...
        }

        // 拖动状态
        state<State.DRAG> {
            // 定义拖动状态下事件触发产生的状态转移
            ...
        }

        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            when (validTransition.sideEffect) {
                //执行状态转移时对应的动作
            }
        }
    }
```
[3bdfc4c8](https://github.com/s1rius/android-nest-scroll-ptr/commit/3bdfc4c8e3d64d79d0f995a3f28c577db66f27ac) 提交中引入状态机重新实现了控件下拉刷新中的状态转换逻辑

<img src="../images/transition.png" width="480" height="300" />

修改后

1. 现在的由于状态转换的触发逻辑，都在`onTransition`的lambda中统一处理，简单易懂

```kotlin
    when (validTransition.sideEffect) {
        SideEffect.OnDragBegin -> { }
        SideEffect.OnComplete -> { 
            // 执行刷新完成的总做
        }
        SideEffect.OnRefreshing -> {
            // 执行刷新动作
        }
        SideEffect.OnCancelToIdle -> {
            // 执行回到顶部的动作
        }
    }
```

2. 通过Event触发状态的转换，触发事件时不需要对状态进行判断，因为状态转换的判断都已经在状态机的内部完成

```kotlin
    // 触发下拉事件
    stateMachine.transition(Event.Pull)
    ...
    // 触发刷新完成事件
    stateMachine.transition(Event.RefreshComplete)
    ...
    // 触发手势抬起产生的事件
    stateMachine.transition(config.generateTouchReleaseEvent())
    ...
    // 触发自动刷新的事件
    stateMachine.transition(Event.AutoRefresh)
        
```

2. 代码逻辑更加简练了，可维护性变好

    状态转换的逻辑基本与手势事件，嵌套滑动等其他的逻辑解耦，debug变简单了

### 结论

引入状态机后，状态的转移流程清晰易懂，代码逻辑变简洁。和原来相比更容易阅读和维护了。


##### 参考

[有限状态机](https://zh.wikipedia.org/wiki/%E6%9C%89%E9%99%90%E7%8A%B6%E6%80%81%E6%9C%BA)

[趣说游戏AI开发：对状态机的褒扬和批判](https://zhuanlan.zhihu.com/p/20476688)

[Unity 教程 | 状态机 1](https://www.bilibili.com/video/BV1St4y1Y7U1)
