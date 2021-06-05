package wtf.s1.android.ptr

import java.util.concurrent.CopyOnWriteArrayList

/**
 * A single linked list to wrap PtrUIHandler
 */
internal class NSPtrListenerHolder private constructor() : NSPtrListener {

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

    override fun onDrag(frame: NSPtrLayout) {
        for (listener in listeners) {
            listener.onDrag(frame)
        }
    }

    override fun onRefreshing(frame: NSPtrLayout) {
        for (listener in listeners) {
            listener.onRefreshing(frame)
        }
    }

    override fun onComplete(frame: NSPtrLayout) {
        for (listener in listeners) {
            listener.onComplete(frame)
        }
    }

    override fun onPositionChange(frame: NSPtrLayout) {
        for (listener in listeners) {
            listener.onPositionChange(frame)
        }
    }

    companion object {
        @JvmStatic
        fun create(): NSPtrListenerHolder {
            return NSPtrListenerHolder()
        }
    }
}