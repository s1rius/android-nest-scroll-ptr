package wtf.s1.android.ptr

import java.util.concurrent.CopyOnWriteArrayList

/**
 * A single linked list to wrap PtrUIHandler
 */
internal class NSPtrListenerHolder: NSPtrListener {

    var listeners = CopyOnWriteArrayList<NSPtrListener>()

    fun hasHandler(): Boolean {
        return listeners.size > 0
    }

    fun addListener(listener: NSPtrListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: NSPtrListener?) {
        listeners.remove(listener)
    }

    override fun onComplete(frame: NSPtrLayout) {
        for (listener in listeners) {
            listener.onComplete(frame)
        }
    }

    override fun onPositionChange(ptrLayout: NSPtrLayout, offset: Int) {
        for (listener in listeners) {
            listener.onPositionChange(ptrLayout, offset)
        }
    }

    override fun onDrag(ptrLayout: NSPtrLayout) {
        for (listener in listeners) {
            listener.onDrag(ptrLayout)
        }
    }

    override fun onRefreshing(ptrLayout: NSPtrLayout) {
        for (listener in listeners) {
            listener.onRefreshing(ptrLayout)
        }
    }

    override fun onTransition(ptrLayout: NSPtrLayout, transition: StateMachine.Transition.Valid<NSPtrLayout.State, NSPtrLayout.Event, NSPtrLayout.SideEffect>) {
        for (listener in listeners) {
            listener.onTransition(ptrLayout, transition)
        }
    }

    override fun onEvent(event: NSPtrLayout.Event) {
        for (listener in listeners) {
            listener.onEvent(event)
        }
    }
}