@file:Suppress("DEPRECATION")

package biz.monro.employee.Adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import biz.monro.employee.R
import com.squareup.picasso.Picasso


class GalleryAdapter(private val mActivity: Activity, private val mFileList: List<String>) :
    RecyclerView.Adapter<GalleryAdapter.CustomViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        Picasso.get()
            .load(mFileList[position])
            .centerCrop()
            .into(holder.imageResource)
        val itemPosition = holder.adapterPosition
        holder.imageResource.setOnClickListener {
            Toast.makeText(
                mActivity,
                mFileList[itemPosition],
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount(): Int {
        return mFileList.size
    }

    class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageResource = itemView.findViewById(R.id.foto_img) as ImageView

    }
}