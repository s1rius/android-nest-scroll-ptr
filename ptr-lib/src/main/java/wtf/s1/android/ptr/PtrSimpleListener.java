package wtf.s1.android.ptr;

import wtf.s1.android.ptr.indicator.PtrStateController;

public abstract class PtrSimpleListener implements PtrListener{
    @Override
    public void onReset(PtrLayout frame) {}

    @Override
    public void onPrepare(PtrLayout frame) {}

    @Override
    public void onBegin(PtrLayout frame) {}

    @Override
    public void onComplete(PtrLayout frame) {}

    @Override
    public void onPositionChange(PtrLayout frame, int status, PtrStateController ptrStateController) {}
}
