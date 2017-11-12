package applikeysolutions.com.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import applikeysolutions.com.moonrefresh.MoonRefresh
import java.util.ArrayList


class MainActivity : AppCompatActivity() {

    private lateinit var moonRefresh: MoonRefresh

    private var sampleList: ArrayList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val icons = intArrayOf(R.drawable.rectangle1, R.drawable.rectangle2, R.drawable.rectangle3, R.drawable.rectangle4, R.drawable.rectangle5)

        for (i in icons.indices) {
            sampleList.add(icons[i])
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = SampleAdapter()

        moonRefresh = findViewById(R.id.pull_to_refresh)
        moonRefresh.setOnRefreshListener(
                object : MoonRefresh.OnRefreshListener {
                    override fun onRefresh() {
                        moonRefresh.postDelayed({ moonRefresh.setRefreshing() }, REFRESH_DELAY.toLong())
                    }
                })
    }

    private inner class SampleAdapter : RecyclerView.Adapter<SampleHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): SampleHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item, parent, false)
            return SampleHolder(view)
        }

        override fun onBindViewHolder(holder: SampleHolder, pos: Int) {
            val data = sampleList[pos]
            holder.bindData(data)
        }

        override fun getItemCount(): Int {
            return sampleList.size
        }
    }

    private inner class SampleHolder(mRootView: View) : RecyclerView.ViewHolder(mRootView) {
        private val mImageViewIcon: ImageView = mRootView.findViewById(R.id.image)

        fun bindData(data: Int) {
            mImageViewIcon.setImageResource(data)
        }
    }

    companion object {
        private val REFRESH_DELAY = 3000
    }
}
