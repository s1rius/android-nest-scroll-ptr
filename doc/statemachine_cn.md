# StateMachine 状态机在下拉刷新控件库中的使用

文中提到的状态机均为有限状态机

`3bdfc4c8` 提交中引入状态机重新实现了控件下拉刷新中的状态转换逻辑

### 背景

- 状态转换，状态判断逻辑复杂

代码中定义了4个下拉刷新的状态以及可以影响刷新状态的其他变量，在手势事件，嵌套滑动，自动刷新等事件的输入情况下，需要对几个变量进行正确判断，才能做出响应。不宜上手，维护成本较高。

```kotlin
private fun updatePos(change: Int) {
    ...
    // 转换状态的同时，还需要进行状态的判断
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


- 由于判断逻辑复杂，可读性变差，冗余方法来让代码的可读性更强

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

有没有一套机制，能够
 `在触发这些事件的同时，更新到对应的状态，触发相应的动作`
并且让代码简洁，可维护性高？

在思考这个问题的时候，我就想到游戏开发，他们如何在多个玩家进行大量的位移，技能释放等多种事件输入的场景下来保证逻辑正常运转的。如果只是靠简单的if，else判断，那对维护者来说，绝对是个灾难。
随即，我看到了游戏开发中对状态机使用的一些文章。觉得同样适用在下拉刷新中。

### 什么是状态机

> 维基百科
> 
有限状态机（英语：finite-state machine，缩写：FSM）又称有限状态自动机（英语：finite-state automation，缩写：FSA），简称状态机，是表示有限个状态以及在这些状态之间的转移和动作等行为的数学计算模型。

简单的理解，状态机的作用就是，给定一个当前状态和触发事件，状态机能够输出下一个状态。

在软件工程中，状态机有以下几个重要的概念

- 状态（State）：表示对象的某种形态，在当前形态下可能会拥有不同的行为和属性。
- 转移（Transition）：表示状态变更，并且必须满足确使转移发生的条件来执行。
- 动作（Action/SideEffect）：表示在给定时刻要进行的活动。
- 事件（Event）：事件通常会引起状态的变迁，促使状态机从一种状态切换到另一种状态。

### 引入后的效果
幸运的是，kotlin社区已经有了状态机的[开源实现](https://github.com/Tinder/StateMachine)

重新梳理的下拉刷新的逻辑，我们发现，下拉刷新的状态可以简化为3个

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
    // 刷新完成
    object RefreshComplete : Event()
    // 自动刷新
    object AutoRefresh : Event()
}
```
状态转换时，执行的动作

```kotlin
sealed class SideEffect {
        // 手势取消回到顶部
        object OnCancelToIdle : SideEffect()
        // 刷新动作
        object OnRefreshing : SideEffect()
        // 开始拖动控件
        object OnDragBegin : SideEffect()
        // 刷新完成的动作
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

        state<State.REFRESHING> {
            on<Event.RefreshComplete> {
                transitionTo(State.IDLE, SideEffect.OnComplete)
            }
            on<Event.ReleaseToIdle> {
                transitionTo(State.IDLE, SideEffect.OnComplete)
            }
        }

        state<State.DRAG> {
            on<Event.ReleaseToIdle> {
                transitionTo(State.IDLE, SideEffect.OnCancelToIdle)
            }
            on<Event.ReleaseToRefreshing> {
                transitionTo(State.REFRESHING, SideEffect.OnRefreshing)
            }
            on<Event.RefreshComplete> {
                transitionTo(State.IDLE, SideEffect.OnComplete)
            }
        }

        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            when (validTransition.sideEffect) {
                SideEffect.OnDragBegin -> { }
                SideEffect.OnComplete -> { 
                    // complete
                }
                SideEffect.OnRefreshing -> {
                    // refresh
                }
                SideEffect.OnCancelToIdle -> {
                    // back to init
                }
            }
        }
    }
```

修改后

1. 现在的由于状态转换的触发逻辑，都在`onTransition`的lambda中统一处理，简单易懂

    ```
    when (validTransition.sideEffect) {
                SideEffect.OnDragBegin -> { }
                SideEffect.OnComplete -> { 
                    // complete
                }
                SideEffect.OnRefreshing -> {
                    // refresh
                }
                SideEffect.OnCancelToIdle -> {
                    // back to init
                }
            }
    ```

2. 通过Event触发状态的转换，开发者只需要关心，事件的触发是否合理

    ```
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

引入状态机后，抽象程度提高，代码逻辑变简洁。和原来相比更容易阅读和维护了。