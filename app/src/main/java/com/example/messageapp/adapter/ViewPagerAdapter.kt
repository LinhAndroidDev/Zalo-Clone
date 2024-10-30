package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.example.messageapp.R
import java.util.Objects

data class IntroData(
    val title: Int,
    val description: Int,
    val image: Int
)

class ViewPagerAdapter(val context: Context) : PagerAdapter() {
    private val imageList = listOf(
        IntroData(R.string.intro1, R.string.detail1, R.drawable.intro1),
        IntroData(R.string.intro2, R.string.detail2, R.drawable.intro2),
        IntroData(R.string.intro3, R.string.detail3, R.drawable.intro3),
        IntroData(R.string.intro4, R.string.detail4, R.drawable.intro4),
    )
    // on below line we are creating a method
    // as get count to return the size of the list.
    override fun getCount(): Int {
        return imageList.size
    }

    // on below line we are returning the object
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as LinearLayout
    }

    // on below line we are initializing
    // our item and inflating our layout file
    @SuppressLint("ServiceCast")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // on below line we are inflating our custom
        // layout file which we have created.
        val itemView: View = mLayoutInflater.inflate(R.layout.item_page_intro, container, false)

        // on below line we are initializing
        // our image view with the id.
        val imgIntro = itemView.findViewById<View>(R.id.imgIntro) as ImageView
        val tvIntro = itemView.findViewById<View>(R.id.tvIntro) as TextView
        val tvDetail = itemView.findViewById<View>(R.id.tvDetail) as TextView

        val intro = imageList[position]
        imgIntro.setImageResource(intro.image)
        tvIntro.text = context.getString(intro.title)
        tvDetail.text = context.getString(intro.description)

        // on the below line we are adding this
        // item view to the container.
        Objects.requireNonNull(container).addView(itemView)

        // on below line we are simply
        // returning our item view.
        return itemView
    }

    // on below line we are creating a destroy item method.
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // on below line we are removing view
        container.removeView(`object` as LinearLayout)
    }
}