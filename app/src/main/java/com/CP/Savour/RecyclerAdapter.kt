package com.CP.Savour

import android.content.Context
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.net.URI

/**
 * The recycler adapter class creates the individual cards that are on display in the main activity
 */
class RecyclerAdapter(val vendorMap: MutableMap<String,Any>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {


    private val restaurants = arrayOf("Purple Onion")
    private val resturantDescriptions = arrayOf("The purple onion is yummy!")
    private val images = mutableMapOf<String,String>()
    private var vendors : MutableList<String> = mutableListOf()
    private var testArray = mutableMapOf<String?, Any>()
    public val Context.picasso: Picasso
        get() = Picasso.get()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.card_layout,viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Picasso.get().load(url).into(viewHolder.itemImage)
    }
    override fun getItemCount(): Int {
        println("VendorMap Size: " + vendorMap.size)
        return vendorMap.size
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemImage: ImageView


        init {
            itemImage = itemView.findViewById(R.id.item_image)

        }
    }
}