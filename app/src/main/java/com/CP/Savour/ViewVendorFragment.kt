package com.CP.Savour

import android.content.Context
import android.graphics.drawable.ScaleDrawable
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.github.debop.kodatimes.today
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_VENDOR = "vendor"


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ViewVendorFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ViewVendorFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ViewVendorFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var vendor: Vendor
    private lateinit var dealImage: ImageView
    private lateinit var vendorName: TextView
    private lateinit var directionsButton: Button
    private lateinit var followButton: Button
    private lateinit var menuButton: Button
    private lateinit var address: TextView
    private lateinit var hours: TextView
    private lateinit var description: TextView
    private lateinit var seeMore: TextView
    private lateinit var descriptionContainer: ConstraintLayout
    private var descriptionExpanded = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            vendor = it.getParcelable(ARG_VENDOR) as Vendor
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_view_vendor, container, false)

        dealImage = view.findViewById(R.id.view_vendor_image)
        vendorName = view.findViewById(R.id.view_vendor_name)
        address = view.findViewById(R.id.vendor_address)
        hours = view.findViewById(R.id.vendor_hours)
        description = view.findViewById(R.id.description)
        seeMore = view.findViewById(R.id.see_more)
        descriptionContainer = view.findViewById(R.id.description_container)

        vendorName.text = vendor.name
        address.text = vendor.address
        hours.text = vendor.dailyHours[Calendar.DAY_OF_WEEK-1]
        description.text = vendor.description

        Glide.with(this)
                .load(vendor.photo)
                .into(dealImage)

        descriptionContainer.setOnClickListener {
            if (!descriptionExpanded){
                descriptionExpanded = true
                seeMore.text = "tap to see less..."
                val params = description.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                description.layoutParams = params
            }else{
                descriptionExpanded = false
                val scale = resources.displayMetrics.density
                seeMore.text = "tap to see more..."
                val params = description.layoutParams
                params.height = (38 * scale).toInt()
                description.layoutParams = params
            }
        }




        // getting the buttons, and scaling their logo
        val scaledMap = ScaleDrawable(ContextCompat.getDrawable(context!!, R.drawable.icon_business),0, 5f,5f)
        val directionsButton = view.findViewById<Button>(R.id.vendor_directions)
        directionsButton.setCompoundDrawables(null, null,null,scaledMap)
        return view

    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ViewVendorFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(): ViewVendorFragment = ViewVendorFragment()

    }
}
