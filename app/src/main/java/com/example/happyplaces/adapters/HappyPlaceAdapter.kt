package com.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ItemHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel

open class HappyPlaceAdapter(
    private val context:Context,
    private val list:ArrayList<HappyPlaceModel>

): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var onClickListener: OnClickListener?=null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(ItemHappyPlaceBinding.inflate(
            LayoutInflater.from(parent.context),parent,false
        ))
    }

    fun setOnClickListener(onClickListener:OnClickListener){
        this.onClickListener=onClickListener
    }
    //(an adapter cannot have on ClickListener so we cannot add onClickListener directly)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model=list[position]

        if(holder is MyViewHolder){
            holder.ivPlaceImage.setImageURI(Uri.parse(model.image))
            holder.tvTitle.text=model.title
            holder.tvDescription.text=model.description

            //binding setOnClickListener to individual items
            holder.itemView.setOnClickListener {
                if(onClickListener!=null){
                    onClickListener!!.onClick(position,model)
                }
            }
        }
    }



    override fun getItemCount(): Int {
        return list.size
    }

    //step for displaying details in next activity
    //implement a new interface and create a function onClick then pass position and model
    interface OnClickListener{
        fun onClick(position: Int,model: HappyPlaceModel)
    }

    fun notifyEditItem(activity:Activity,position: Int,requestCode:Int){
        val intent= Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
        activity.startActivityForResult(intent,requestCode)
        notifyItemChanged(position)
    }

    fun removeAt(position: Int){
        val dbHandler= DatabaseHandler(context)
        val isDeleted=dbHandler.deleteHappyPlace(list[position])
        if(isDeleted>0){
            list.removeAt(position)
            notifyItemRemoved(position)
        }

    }

    private class MyViewHolder(binding: ItemHappyPlaceBinding):RecyclerView.ViewHolder(binding.root){
        val ivPlaceImage=binding.ivPlaceImage
        val tvTitle=binding.tvTitle
        val tvDescription=binding.tvDescription

    }

}