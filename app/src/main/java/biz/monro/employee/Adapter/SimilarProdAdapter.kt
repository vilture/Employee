package biz.monro.employee.Adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import biz.monro.employee.databinding.CardviewSimilarprodBinding
import com.squareup.picasso.Picasso


data class similarProd(
    val photo: String,
    val matnr: String,
    val maktx: String,
    val price: String,
    val ra_code: String
) {
    override fun toString(): String {
        return "similarProd(title='$maktx', image='$photo', price='$price', price='$ra_code')"
    }
}

/**
 * создаем класс привязки адаптера для нашего списка товаров
 */
class SimilarProdAdapter :
    RecyclerView.Adapter<SimilarProdAdapter.SpViewHolder>() {
    private var items: List<similarProd> = ArrayList()
    private lateinit var binding: CardviewSimilarprodBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpViewHolder {
        binding =
            CardviewSimilarprodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SpViewHolder(binding)
//        return SpViewHolder(
//            LayoutInflater.from(parent.context)
//                .inflate(R.layout.cardview_similarprod, parent, false)
//        )
    }

    override fun onBindViewHolder(holder: SpViewHolder, position: Int) {
        val similarBean = items[position]
        holder.bind(similarBean)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(spList: List<similarProd>) {
        items = spList
    }


    inner class SpViewHolder constructor(binding: CardviewSimilarprodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val sp_photo = binding.spPhoto
        val sp_matnr = binding.spMatnr
        val sp_maktx = binding.spMaktx
        val sp_price = binding.spPrice
        val sp_racode = binding.spRacode

        @SuppressLint("SetTextI18n")
        fun bind(sp: similarProd) {
            sp_matnr.text = sp.matnr

            Picasso.get().load(sp.photo).into(sp_photo)
            sp_maktx.text = sp.maktx
            sp_price.text = "Цена " + sp.price
            if (sp.ra_code.isNotEmpty()) {
                binding.spRacode.visibility = View.VISIBLE
                sp_racode.text = sp.ra_code
            } else
                binding.spRacode.visibility = View.INVISIBLE

        }
    }

}


/**
 * рисуем красивый прямоугольник
 */
class ItemDecoration(private val padding: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = padding
    }
}

/**
 * отрабатываем нажатия на товар
 */
class RecyclerItemClickListener(
    recyclerView: RecyclerView,
    private val listener: OnItemClickListener?
) : RecyclerView.OnItemTouchListener {


    private var gestureDetector: GestureDetector =
        GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {}
        })

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, matnr: String)
    }

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)


        if (childView != null && listener != null && gestureDetector.onTouchEvent(e)) {
            val binding =
                CardviewSimilarprodBinding.inflate(
                    LayoutInflater.from(childView.context),
                    view,
                    false
                )

            listener.onItemClick(
                childView,
                view.getChildAdapterPosition(childView),
                binding.spMatnr.text.toString()
            )
            return true
        }
        return false
    }

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}
