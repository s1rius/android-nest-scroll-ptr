package wtf.s1.android.ptr;

import wtf.s1.android.ptr.indicator.PtrIndicator;

/**
 *
 */
public interface PtrListener {

    /**
     * When the content view has reached top and refresh has been completed, view will be reset.
     *
     * @param frame
     */
    public void onReset(PtrFrameLayout frame);

    /**
     * prepare for loading
     *
     * @param frame
     */
    public void onPrepare(PtrFrameLayout frame);

    /**
     * perform refreshing UI
     */
    public void onBegin(PtrFrameLayout frame);

    /**
     * perform UI after refresh
     */
    public void onComplete(PtrFrameLayout frame);


    public void onPositionChange(PtrFrameLayout frame, int status, PtrIndicator ptrIndicator);
}
