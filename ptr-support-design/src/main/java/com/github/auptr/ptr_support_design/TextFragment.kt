package com.github.auptr.ptr_support_design

import `in`.srain.cube.views.ptr.PtrFrameLayout
import `in`.srain.cube.views.ptr.PtrUIHandler
import `in`.srain.cube.views.ptr.indicator.PtrIndicator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_text.*

class TextFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ptr_layout.addPtrUIHandler(object : PtrUIHandler {
            override fun onUIReset(frame: PtrFrameLayout) {

            }

            override fun onUIRefreshPrepare(frame: PtrFrameLayout) {

            }

            override fun onUIRefreshBegin(frame: PtrFrameLayout) {

                ptr_layout.postDelayed({
                    if (isDetached) {
                        return@postDelayed
                    }

                    ptr_layout.refreshComplete()
                }, 3000)
            }

            override fun onUIRefreshComplete(frame: PtrFrameLayout) {

            }

            override fun onUIPositionChange(frame: PtrFrameLayout, status: Int, ptrIndicator: PtrIndicator) {

            }
        })
    }
}
