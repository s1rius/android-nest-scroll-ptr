package wtf.s1.android.ptr

import wtf.s1.android.core.StateMachine

class RefreshStateMachine {

    companion object {
        sealed class State {
            // 初始化状态
            class IDLE : State()

            // 刷新状态
            class REFRESHING : State()

            // 拖动状态
            class DRAG : State()
        }

        sealed class Event {
            data class OnPull(val position: Int) : Event()
            class OnReleaseToIdle : Event()
            class OnReleaseToRefreshing : Event()
            object OnComplete : Event()
            object OnAutoRefresh : Event()
        }

        sealed class SideEffect {
            class OnRefreshing : SideEffect()
            class OnDragBegin : SideEffect()
            class OnComplete : SideEffect()
        }

        fun stateMachine(): StateMachine<State, Event, SideEffect> {
            return StateMachine.create<State, Event, SideEffect> {
                initialState(State.IDLE())
                state<State.IDLE> {
                    on<Event.OnPull> {
                        transitionTo(State.DRAG(), SideEffect.OnDragBegin())
                    }
                    on<Event.OnAutoRefresh> {
                        transitionTo(State.REFRESHING(), SideEffect.OnRefreshing())
                    }
                }

                state<State.REFRESHING> {
                    on<Event.OnComplete> {
                        transitionTo(State.IDLE(), SideEffect.OnComplete())
                    }
                }

                state<State.DRAG> {
                    on<Event.OnReleaseToIdle> {
                        transitionTo(State.IDLE(), SideEffect.OnComplete())
                    }
                    on<Event.OnReleaseToRefreshing> {
                        transitionTo(State.REFRESHING(), SideEffect.OnRefreshing())
                    }
                    on<Event.OnComplete> {
                        transitionTo(State.IDLE(), SideEffect.OnComplete())
                    }
                }
            }
        }
    }
}