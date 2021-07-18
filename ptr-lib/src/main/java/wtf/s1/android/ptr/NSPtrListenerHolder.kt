package wtf.s1.android.ptr

import wtf.s1.nsptr.Event
import wtf.s1.nsptr.SideEffect
import wtf.s1.nsptr.State
import wtf.s1.nsptr.StateMachine
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A single linked list to wrap PtrUIHandler
 */
internal class NSPtrListenerHolder: NSPtrListener {

    private var listeners = CopyOnWriteArrayList<NSPtrListener>()

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

    override fun onComplete(ptrLayout: NSPtrLayout) {
        for (listener in listeners) {
            listener.onComplete(ptrLayout)
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

    override fun onTransition(ptrLayout: NSPtrLayout, transition: StateMachine.Transition.Valid<State, Event, SideEffect>) {
        for (listener in listeners) {
            listener.onTransition(ptrLayout, transition)
        }
    }
}