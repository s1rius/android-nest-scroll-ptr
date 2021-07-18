package wtf.s1.android.ptr.demo

import android.app.Application
import android.util.Log
import wtf.s1.pudge.hugo2.Hugo2

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        Hugo2.setLogger(object: Hugo2.Hugo2Logger {

            override fun logI(clazz: String, method: String, params: String) {
                super.logI(clazz, method, params)
                var simpleClazz = ""
                if (clazz.endsWith(SimpleListView::class.java.simpleName)) {
                    simpleClazz = "child"
                    Log.i("hugo", "----> $simpleClazz $method $params")
                } else if (clazz.endsWith(SwipeToRefreshLayout::class.java.simpleName)) {
                    simpleClazz = "ptr"
                    Log.i("hugo", "     ----> $simpleClazz $method $params")
                } else if (clazz.endsWith(CoordinatorLayoutCopy::class.java.simpleName)) {
                    simpleClazz = "grand"
                    Log.i("hugo", "           ----> $simpleClazz $method $params")
                } else {
                    Log.i("hugo", "----> $clazz $method $params")
                }
            }

            override fun logO(clazz: String, method: String) {
                super.logO(clazz, method)
                // Log.i("hugo", "<---- $clazz $method")
            }

        })
    }
}