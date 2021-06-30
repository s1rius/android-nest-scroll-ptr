package wtf.s1.android.ptr.demo.md

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_text.*
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr_support_design.R

class TextFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ptr_layout.setPTRListener(object: SwipeToRefreshLayout.OnPtrRefreshListener {
            override fun onRefresh() {
                ptr_layout.postDelayed({
                    if (isDetached) {
                        return@postDelayed
                    }

                    ptr_layout.isRefreshing = false
                }, 3000)
            }
        })
    }
}
