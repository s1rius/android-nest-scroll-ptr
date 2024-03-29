package wtf.s1.ui.nsptr


sealed class State {
    // init state
    // 初始化状态
    object IDLE : State()

    // refreshing state
    // 刷新状态
    object REFRESHING : State()

    // drag state
    // 拖动状态
    object DRAG : State()
}

sealed class Event {
    // pull/drag the content
    // 下拉事件
    object Pull : Event()

    // touch release make content view to IDLE
    // 放手回到空闲
    object ReleaseToIdle : Event()

    // touch release make content view to REFRESHING
    // 放手开始刷新
    object ReleaseToRefreshing : Event()

    // refresh complete
    // 刷新完成事件
    object RefreshComplete : Event()

    // auto refresh
    // 自动刷新事件
    object AutoRefresh : Event()
}

sealed class SideEffect {
    // detect touch release or other trigger transition to IDLE
    // 手势取消回到顶部
    object OnReleaseToIdle : SideEffect()

    // detect refreshing action
    // 刷新动作
    object OnRefreshing : SideEffect()

    // detect drag action
    // 开始拖动控件
    object OnPull : SideEffect()

    // detect refresh complete
    // 刷新完成的动作
    object OnComplete : SideEffect()
}

fun createNSPtrFSM(block: (StateMachine.Transition<State, Event, SideEffect>) -> Unit)
:StateMachine<State, Event, SideEffect> {
    return StateMachine.create<State, Event, SideEffect> {
        initialState(State.IDLE)

        state<State.IDLE> {
            on<Event.Pull> {
                transitionTo(State.DRAG, SideEffect.OnPull)
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
                transitionTo(State.IDLE, SideEffect.OnReleaseToIdle)
            }
        }

        state<State.DRAG> {
            on<Event.ReleaseToIdle> {
                transitionTo(State.IDLE, SideEffect.OnReleaseToIdle)
            }
            on<Event.ReleaseToRefreshing> {
                transitionTo(State.REFRESHING, SideEffect.OnRefreshing)
            }
            on<Event.RefreshComplete> {
                transitionTo(State.IDLE, SideEffect.OnComplete)
            }
        }

        onTransition {
            block.invoke(it)
        }
    }
}