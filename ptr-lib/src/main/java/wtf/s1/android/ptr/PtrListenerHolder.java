package wtf.s1.android.ptr;

import java.util.concurrent.CopyOnWriteArrayList;

import wtf.s1.android.ptr.indicator.PtrStateController;

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
    public void onReset(PtrLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onReset(frame);
        }
    }

    @Override
    public void onPrepare(PtrLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onPrepare(frame);
        }
    }

    @Override
    public void onBegin(PtrLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onBegin(frame);
        }
    }

    @Override
    public void onComplete(PtrLayout frame) {
        for (PtrListener listener : listeners) {
            listener.onComplete(frame);
        }
    }

    @Override
    public void onPositionChange(PtrLayout frame, int status, PtrStateController ptrStateController) {
        for (PtrListener listener : listeners) {
            listener.onPositionChange(frame, status, ptrStateController);
        }
    }
}
