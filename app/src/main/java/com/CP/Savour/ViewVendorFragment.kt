package com.CP.Savour

import android.Manifest
import android.app.Activity
import android.graphics.drawable.ScaleDrawable
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import java.util.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.*

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.vision.CameraSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_deals.*
import org.joda.time.DateTime

private const val ARG_VENDOR = "vendor"
private const val POINTS = "points"
private const val SCAN_QR_REQUEST = 1

class ViewVendorFragment : Fragment() {
    private lateinit var vendor: Vendor
    private lateinit var dealImage: ImageView
    private lateinit var dealsHeader: TextView
    private lateinit var vendorName: TextView
    private lateinit var loyaltyText: TextView
    private lateinit var directionsButton: Button
    private lateinit var followButton: Button
    private lateinit var menuButton: Button
    private lateinit var loyaltyButton: Button
    private lateinit var address: TextView
    private lateinit var hours: TextView
    private lateinit var description: TextView
    private lateinit var seeMore: TextView
    private lateinit var descriptionContainer: ConstraintLayout
    private lateinit var loyaltyProgress: ProgressBar
    private var redemptionTime: Long = 0
    var activedeals = mutableMapOf<String, Deal?>()
    var inactivedeals = mutableMapOf<String, Deal?>()
    var dealsArray : List<Deal?> = arrayListOf()

    private var redeem: Boolean = false

    private var layoutManager : RecyclerView.LayoutManager? = null
    private var dealsAdapter : DealsViewVendorRecyclerAdapter? = null

    var firstLocationUpdate = true
    private lateinit var locationService: LocationService
    private lateinit var loyaltyConstraint: ConstraintLayout
    private var user: FirebaseUser = FirebaseAuth.getInstance().currentUser!!
    private var descriptionExpanded = false

    val favoriteRef = FirebaseDatabase.getInstance().getReference("Users").child(user!!.uid).child("favorites")
    private lateinit var dealsRef: Query
    val userInfoRef = FirebaseDatabase.getInstance().getReference("Users").child(user!!.uid)

    private var points: String? = null

    private lateinit var favoritesListener: ValueEventListener
    private lateinit var userListener: ValueEventListener
    private lateinit var dealsListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            vendor = it.getParcelable(ARG_VENDOR) as Vendor
            dealsRef = FirebaseDatabase.getInstance().getReference("Deals").orderByChild("vendor_id").equalTo(vendor.id)
        }
        if (this.activity != null) {
            locationService = LocationService(pActivity = this.activity!!, callback = {
                onLocationChanged(it)
            })
            startLocation()
        } else {
            println("VIEWVENDORFRAGMENT:onCreate:Error getting activity for locationService")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_view_vendor, container, false)
        loyaltyConstraint = view.findViewById(R.id.loyalty_checkin)
        dealImage = view.findViewById(R.id.view_vendor_image)
        vendorName = view.findViewById(R.id.view_vendor_name)
        address = view.findViewById(R.id.vendor_address)
        hours = view.findViewById(R.id.vendor_hours)
        description = view.findViewById(R.id.description)
        seeMore = view.findViewById(R.id.see_more)
        descriptionContainer = view.findViewById(R.id.info_container)
        dealsHeader = view.findViewById(R.id.deals_header)

        menuButton = view.findViewById(R.id.vendor_menu)
        directionsButton = view.findViewById(R.id.vendor_directions)
        followButton = view.findViewById(R.id.vendor_follow)

        loyaltyButton = view.findViewById(R.id.checkin_button)
        loyaltyProgress = view.findViewById(R.id.loyalty_progress)
        loyaltyText = view.findViewById(R.id.loyalty_text)

        layoutManager = LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("triggered")

                println(snapshot.toString())

                if (snapshot.child("following").child(vendor.id!!).exists()) {
                    followButton.background = ContextCompat.getDrawable(context!!, R.drawable.vendor_button)
                    followButton.text = "Following"
                    println("Following")



                } else {
                    followButton.background = ContextCompat.getDrawable(context!!, R.drawable.vendor_button_selected)
                    followButton.text = "Follow"
                    println("Follow")

                }
                if(vendor.loyaltyDeal == "") {
                    val params = loyaltyConstraint.layoutParams
                    params.height = 0
                    loyaltyConstraint.layoutParams = params
                } else {

                    val params = loyaltyConstraint.layoutParams
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    loyaltyConstraint.layoutParams = params

                    if (snapshot.child("loyalty").child(vendor.id!!).exists()) {

                        val userPoints = snapshot.child("loyalty").child(vendor.id!!).child("redemptions").child("count").value
                        if (snapshot.child("loyalty").child(vendor.id!!).child("redemptions").child("time").exists()) {
                            redemptionTime = snapshot.child("loyalty").child(vendor.id!!).child("redemptions").child("time").value as Long
                        } else {
                            redemptionTime = 0
                        }

                        points = userPoints.toString()
                        loyaltyText.text = "$points/${vendor.loyaltyCount}"

                        points?.let {
                            loyaltyProgress.progress = it.toInt()
                        }

                    } else {
                        loyaltyText.text = "0/" + vendor.loyaltyCount
                        loyaltyProgress.progress = 0

                        userInfoRef.child("loyalty").child(vendor.id!!).child("redemptions").child("count").setValue(0)

                    }

                    if (points!!.toInt() >= vendor.loyaltyCount!!.toInt()) {
                        loyaltyButton.text = "Redeem"
                    } else {
                        loyaltyButton.text = "Loyalty Check-in"
                    }
                }


            }

            override fun onCancelled(dbError: DatabaseError) {
                println("cancelled userListener")
            }
        }
        userInfoRef.addValueEventListener(userListener)


        loyaltyButton.setOnClickListener {
            //val time = userInfoRef.child("loyalty").child(vendor.id!!).child("redemptions").child("time")
            println("LOYALTY TIME VALUE")
            println(redemptionTime)

            if (86400000 < (DateTime.now().millis) - redemptionTime) {
            //if (true) {
                val intent = Intent(context, ScanActivity::class.java)
                intent.putExtra(ARG_VENDOR, vendor)

                if (points == null) {
                    points = "0"
                }
                intent.putExtra(POINTS, points)

                startActivityForResult(intent, SCAN_QR_REQUEST)
            } else {
                Toast.makeText(context, "The deal cannot be redeemed yet!", Toast.LENGTH_LONG).show()
            }
        }

        followButton.setOnClickListener {
            if (followButton.text == "Follow") {

                userInfoRef.child("following").child(vendor.id!!).setValue(true)
                println("set value")



            } else {
                userInfoRef.child("following").child(vendor.id!!).removeValue()
                println("remove")
            }
        }
        vendorName.text = vendor.name
        address.text = vendor.address
        hours.text = vendor.dailyHours[Calendar.DAY_OF_WEEK - 1]
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
                val scale = resources.displayMetrics.scaledDensity
                seeMore.text = "tap to see more..."
                val params = description.layoutParams
                params.height = (36 * scale).toInt()
                description.layoutParams = params
            }
        }

        menuButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(vendor.menu))
            startActivity(browserIntent)
        }

        directionsButton.setOnClickListener {
            val url = "http://maps.google.com/maps?daddr="+ vendor.address +"&mode=driving"
            val intent = Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
            startActivity(intent)
        }



        // getting the buttons, and scaling their logo
        val scaledMap = ScaleDrawable(ContextCompat.getDrawable(context!!, R.drawable.icon_business),0, 5f,5f)
        val directionsButton = view.findViewById<Button>(R.id.vendor_directions)
        directionsButton.setCompoundDrawables(null, null,null,scaledMap)
        return view

    }

    fun getFirebaseData(){

        var vendors = mutableMapOf<String, Vendor?>()
        vendors[vendor.id!!] = vendor

        var favUpdated = false
        var favorites = mutableMapOf<String,String>()

        favoritesListener = object : ValueEventListener {//Get favorites
            /**
             * Listening for when the data has been changed
             * and also when we want to access f
             */
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    favorites = dataSnapshot.value as MutableMap<String, String>
                    for (deal in activedeals) {
                        deal.value!!.favorited = favorites.containsKey(deal.key)
                    }
                    for (deal in inactivedeals) {
                        deal.value!!.favorited = favorites.containsKey(deal.key)
                    }
                    if (favUpdated) {
                        if (deal_list != null) {
                            deal_list.adapter!!.notifyDataSetChanged()
                        }
                    }
                } else {
                    favorites.clear()
                    for (deal in activedeals) {
                        deal.value!!.favorited = false
                    }
                    for (deal in inactivedeals) {
                        deal.value!!.favorited = false
                    }
                    if (favUpdated) {
                        if (deal_list != null) {
                            deal_list.adapter!!.notifyDataSetChanged()
                        }
                    }
                }
                if (!favUpdated) { //DONT redo deals if already just updating favorites
                    dealsListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {

                                for (dealSnapshot in dataSnapshot.children) {
                                    val temp = Deal(dealSnapshot,locationService.currentLocation!!,vendor.location!!,user!!.uid, favorites)

                                    //if the deal is not expired or redeemed less than half an hour ago, show it
                                    if (temp.isAvailable()){
                                        if (temp.active!!){
                                            activedeals[temp.id!!] = temp
                                            inactivedeals.remove(temp.id!!)
                                        }else{
                                            inactivedeals[temp.id!!] = temp
                                            activedeals.remove(temp.id!!)
                                        }
                                    }else if (temp.redeemedTime != null){
                                        if (((DateTime().millis/1000) - temp.redeemedTime!!) < 1800){
                                            activedeals[temp.id!!] = temp
                                            inactivedeals.remove(temp.id!!)
                                        }
                                    }else{
                                        activedeals.remove(temp.id!!)
                                        inactivedeals.remove(temp.id!!)
                                    }
                                }
                                onDataChanged()
                            }else{
                                onDataChanged()
                            }

                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            println("cancelled userListener")
                        }
                    }
                    dealsRef.addValueEventListener(dealsListener)

                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        favoriteRef.addValueEventListener(favoritesListener)


    }

    fun checkNoDeals(){
        if (dealsArray.isEmpty()){
            dealsHeader.text = "No Current Offers"
        }else{
            dealsHeader.text = "Current Offers"
        }
    }

    fun onDataChanged(){
        dealsArray = ArrayList(activedeals.values) + ArrayList(inactivedeals.values)//.sortedBy { deal -> deal!!.distanceMiles } .sortedBy { deal -> deal!!.distanceMiles }
        checkNoDeals()
        if (dealsAdapter == null) {
            dealsAdapter = DealsViewVendorRecyclerAdapter(dealsArray,vendor, context!!)
            deal_list.layoutManager = layoutManager
            deal_list.adapter = dealsAdapter

        } else {
            dealsAdapter!!.updateElements(dealsArray)
            dealsAdapter!!.notifyDataSetChanged()
        }
    }

    fun startLocation(){
        if(checkPermission() && !locationService.startedUpdates) {
            locationService.startLocationUpdates()
        }
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        if(firstLocationUpdate){
            firstLocationUpdate = false
            getFirebaseData()
        }else{
            if (deal_list != null){
                for (deal in activedeals){
                    deal.value!!.updateDistance(vendor, location)
                }
                for (deal in inactivedeals){
                    deal.value!!.updateDistance(vendor, location)
                }
            }

        }
    }

    private fun checkPermission() : Boolean {
        if (ContextCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            requestPermissions()
            return false
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this.activity!!, arrayOf("Manifest.permission.ACCESS_FINE_LOCATION"),1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION ) {
                startLocation()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        followButton.setOnClickListener(null)
        if (userListener != null){
            userInfoRef.removeEventListener(userListener)
        }
        if (favoritesListener != null){
            favoriteRef.removeEventListener(favoritesListener)
        }
        if (dealsListener != null){
            dealsRef.removeEventListener(dealsListener)
        }
        locationService.cancel()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        println("ONACTIVITYRESULT FROM FRAGMENT!")

        if (Activity.RESULT_OK == resultCode ) {
            println(data!!.getStringExtra("Test"))
            val pts = data.getIntExtra(POINTS,0)
            userInfoRef.child("loyalty").child(vendor.id!!).child("redemptions").child("count").setValue(pts)
            userInfoRef.child("loyalty").child(vendor.id!!).child("redemptions").child("time").setValue(DateTime.now().millis)

            println("PTS BABY")
            println(pts)

            loyaltyProgress.progress = pts
            loyaltyText.text = "$pts/${vendor.loyaltyCount}"

        }
    }


    companion object {

        @JvmStatic
        fun newInstance(): ViewVendorFragment = ViewVendorFragment()

    }
}
