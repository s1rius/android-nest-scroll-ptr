package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import wtf.s1.android.ptr.demo.FragmentsViewPagerAdapter
import wtf.s1.android.ptr.demo.util.getActivity
import wtf.s1.android.ptr_support_design.R

class MDPtrView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mDrawerLayout: DrawerLayout? = null

    init {
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
            val adapter = FragmentsViewPagerAdapter(it.supportFragmentManager)
            adapter.addFragment(CheeseRecyclerViewFragment(), "Category 1")
            adapter.addFragment(CheeseListViewFragment(), "Category 2")
            adapter.addFragment(TextFragment(), "Category 3")
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