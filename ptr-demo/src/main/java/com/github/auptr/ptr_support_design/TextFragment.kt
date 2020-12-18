package com.github.auptr.ptr_support_design

import wtf.s1.android.ptr.PtrLayout
import wtf.s1.android.ptr.PtrSimpleListener
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
        ptr_layout.addPtrListener(object : PtrSimpleListener() {

            override fun onBegin(frame: PtrLayout) {

                ptr_layout.postDelayed({
                    if (isDetached) {
                        return@postDelayed
                    }

                    ptr_layout.refreshComplete()
                }, 3000)
            }
        })
    }
}
