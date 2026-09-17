package de.xrdoge.agent.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.xrdoge.agent.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pager = findViewById<ViewPager2>(R.id.viewPager)
        val tabs = findViewById<TabLayout>(R.id.tabLayout)
        pager.adapter = TabPagerAdapter(this)

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Dashboard"
                1 -> "Dokumentation"
                2 -> "Terminal"
                3 -> "Dateibaum"
                4 -> "AppBuilder"
                else -> "Memory"
            }
        }.attach()
    }
}

class TabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> DashboardFragment()
        1 -> DocumentationFragment()
        2 -> TerminalFragment()
        3 -> FileTreeFragment()
        4 -> AppBuilderFragment()
        else -> MemoryFragment()
    }
}
