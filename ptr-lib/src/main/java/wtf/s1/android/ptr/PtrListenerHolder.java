package wtf.s1.android.ptr;

import java.util.concurrent.CopyOnWriteArrayList;

import wtf.s1.android.ptr.indicator.PtrIndicator;

/**
 * A single linked list to wrap PtrUIHandler
 */
class PtrListenerHolder implements PtrListener {

    CopyOnWriteArrayList<PtrListener> listeners = new CopyOnWriteArrayList<>();

    private PtrListenerHolder() {

    }

    public boolean hasHandler() {
        return listeners.size() > 0;
    }

    public static PtrListenerHolder create() {
        return new PtrListenerHolder();
    }

    public void addListener(PtrListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PtrListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onReset(PtrFrameLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onReset(frame);
        }
    }

    @Override
    public void onPrepare(PtrFrameLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onPrepare(frame);
        }
    }

    @Override
    public void onBegin(PtrFrameLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onBegin(frame);
        }
    }

    @Override
    public void onComplete(PtrFrameLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onComplete(frame);
        }
    }

    @Override
    public void onPositionChange(PtrFrameLayout frame, int status, PtrIndicator ptrIndicator) {
        for (PtrListener listener : listeners) {
            listener.onPositionChange(frame, status, ptrIndicator);
        }
    }
}
