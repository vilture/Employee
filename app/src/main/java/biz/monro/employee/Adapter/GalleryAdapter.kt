package biz.monro.employee.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import biz.monro.employee.Functions.RepFoto
import biz.monro.employee.Other.ImageDetailActivity
import biz.monro.employee.R
import com.squareup.picasso.Picasso
import java.io.File


class RecyclerViewAdapter( private val context: Context, private val imagePathArrayList: ArrayList<String> ) :
    RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {

        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val imgFile = File(imagePathArrayList[position])

        if (imgFile.exists()) {
            Picasso.get().load(imgFile).placeholder(R.drawable.launch_screen)
                .into(holder.imageIV)

            holder.itemView.setOnClickListener {
                val i = Intent(
                    context,
                    ImageDetailActivity::class.java
                )

                i.putExtra("imgPath", imagePathArrayList[position])

                context.startActivity(i)
            }
        }
    }

    override fun getItemCount(): Int {
        return imagePathArrayList.size
    }

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIV: ImageView

        init {
            imageIV = itemView.findViewById(R.id.foto_img)
        }
    }
}
