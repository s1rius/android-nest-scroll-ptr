package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import wtf.s1.android.ptr.demo.util.getActivity
import wtf.s1.android.ptr_support_design.R

class MDPtrView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mDrawerLayout: DrawerLayout? = null

    init {
        setBackgroundColor(Color.WHITE)
        LayoutInflater.from(context).inflate(R.layout.md_ptr_view, this, true)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setLogo(R.drawable.ic_menu)

        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        setupDrawerContent(navigationView)

        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        setupViewPager(viewPager)

        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        getActivity()?.let {
            val adapter = object: PagerAdapter() {
                override fun getCount(): Int = 3

                override fun isViewFromObject(view: View, `object`: Any): Boolean {
                    return `object` === view
                }

                override fun instantiateItem(container: ViewGroup, position: Int): Any {
                    val v = when (position) {
                        0 -> HerosRecyclerViewFragment(context)
                        1 -> TextFragment(context)
                        2 -> WebFragment(context)
                        else -> View(context)
                    }
                    container.addView(v)
                    return v
                }

                override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                    container.removeView(`object` as View)
                }

                override fun getPageTitle(position: Int): CharSequence? {
                    return when (position) {
                        0 -> "RecyclerView"
                        1 -> "TextView"
                        2 -> "WebView"
                        else -> super.getPageTitle(position)
                    }
                }
            }
            viewPager.adapter = adapter
        }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            mDrawerLayout!!.closeDrawers()
            true
        }
    }
}