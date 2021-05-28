package wtf.s1.android.ptr

import java.util.concurrent.CopyOnWriteArrayList

/**
 * A single linked list to wrap PtrUIHandler
 */
internal class PtrListenerHolder private constructor() : PtrListener {

    var listeners = CopyOnWriteArrayList<PtrListener>()

    fun hasHandler(): Boolean {
        return listeners.size > 0
    }

    fun addListener(listener: PtrListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PtrListener?) {
        listeners.remove(listener)
    }

    override fun onReset(frame: PtrLayout?) {
        for (listener in listeners) {
            listener.onReset(frame)
        }
    }

    override fun onPrepare(frame: PtrLayout?) {
        for (listener in listeners) {
            listener.onPrepare(frame)
        }
    }

    override fun onBegin(frame: PtrLayout?) {
        for (listener in listeners) {
            listener.onBegin(frame)
        }
    }

    override fun onComplete(frame: PtrLayout?) {
        for (listener in listeners) {
            listener.onComplete(frame)
        }
    }

    override fun onPositionChange(frame: PtrLayout?, status: Int, ptrStateController: PtrStateController?) {
        for (listener in listeners) {
            listener.onPositionChange(frame, status, ptrStateController)
        }
    }

    companion object {
        @JvmStatic
        fun create(): PtrListenerHolder {
            return PtrListenerHolder()
        }
    }
}