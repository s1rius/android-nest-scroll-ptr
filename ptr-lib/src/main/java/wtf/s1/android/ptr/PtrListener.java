package wtf.s1.android.ptr;

import wtf.s1.android.ptr.indicator.PtrStateController;

/**
 *
 */
public interface PtrListener {

    /**
     * When the content view has reached top and refresh has been completed, view will be reset.
     *
     * @param frame
     */
    public void onReset(PtrLayout frame);

    /**
     * prepare for loading
     *
     * @param frame
     */
    public void onPrepare(PtrLayout frame);

    /**
     * perform refreshing UI
     */
    public void onBegin(PtrLayout frame);

    /**
     * perform UI after refresh
     */
    public void onComplete(PtrLayout frame);


    public void onPositionChange(PtrLayout frame, int status, PtrStateController ptrStateController);
}
