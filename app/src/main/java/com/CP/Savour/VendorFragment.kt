package com.CP.Savour

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v7.widget.LinearLayoutManager
import com.firebase.geofire.*

import com.google.firebase.database.*
import com.google.firebase.database.DataSnapshot

import com.firebase.geofire.GeoLocation
import com.google.firebase.database.DatabaseError
import com.firebase.geofire.GeoQueryEventListener
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.CP.Savour.R.id.vendor_list
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import kotlinx.android.synthetic.main.fragment_vendor.*


class VendorFragment : Fragment() {
    private var layoutManager : RecyclerView.LayoutManager? = null
    private var vendorAdapter : VendorRecyclerAdapter? = null

    private var savourImg: ImageView? = null
    private lateinit var locationMessage: TextView
    private lateinit var locationButton: Button
    private lateinit var noVendorsText: TextView
    var vendorArray : List<Vendor?> = arrayListOf()


    var geoRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Vendors_Location")
    var geoFire = GeoFire(geoRef)

    val vendors = mutableMapOf<String, Vendor?>()

    var firstLocationUpdate = true
    private lateinit var locationService: LocationService

    var geoQuery:GeoQuery? = null
    var  vendorReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Vendors")



    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)


        val view = inflater.inflate(R.layout.fragment_vendor, container, false)


        savourImg = view.findViewById(R.id.imageView5) as ImageView
        locationMessage = view.findViewById(R.id.locationMessage) as TextView
        locationButton = view.findViewById(R.id.location_button) as Button
        noVendorsText = view.findViewById(R.id.novendors)

        locationButton.setOnClickListener {
            var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity!!.getPackageName())).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity!!.startActivity(intent)
        }

        Glide.with(this)
                .load(R.drawable.savour_white)
                .into(savourImg!!)

        // retrieving the vendors from the database
        layoutManager = LinearLayoutManager(context)

        if (this.activity != null){
            locationService = LocationService(pActivity = this.activity!!,callback = {
                onLocationChanged(it)
            })
            startLocation()
        }else{
            println("VENDORFRAGMENT:onCreate:Error getting activity for locationService")
        }

        return view
    }

    companion object {
        fun newInstance(): VendorFragment = VendorFragment()
    }

    override fun onStart() {
        super.onStart()
        if (locationService != null){ //check that we didnt get an error before and not init locationService
            startLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationService.cancel()
    }

    private fun getFirebaseData(lat:Double, lng:Double) {
        geoQuery = geoFire.queryAtLocation(GeoLocation(lat, lng), 80.5) // About 50 mile query

        geoQuery!!.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude))
                vendorReference.child(key)
                val vendorListener = object : ValueEventListener {
                    /**
                     * Listening for when the data has been changed
                     * and also when we want to access f
                     */
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {

                            //convert to android Location object
                            val vendorLocation = Location("")
                            vendorLocation.latitude = location.latitude
                            vendorLocation.longitude = location.longitude
                            vendors.put(dataSnapshot.key!!,Vendor(dataSnapshot,locationService.currentLocation!!,vendorLocation))
                            onDataChanged()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }
                vendorReference.child(key).addValueEventListener(vendorListener)
            }

            override fun onKeyExited(key: String) {
                println(String.format("Key %s is no longer in the search area", key))
                vendors.remove(key)
                onDataChanged()
            }

            override fun onKeyMoved(key: String, location: GeoLocation) {
                println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude))
            }

            override fun onGeoQueryReady() {
                println("All initial data has been loaded and events have been fired!")
                checkNoVendors() // should perform a check if any keys enter radius. This will give a false check until firebase gives us data back from those keys
            }

            override fun onGeoQueryError(error: DatabaseError) {
                System.err.println("There was an error with this query: $error")
            }
        })

    }

    fun checkNoVendors(){
        if (vendorArray.count() < 1) {
            noVendorsText!!.setVisibility(View.VISIBLE)
            vendorArray = ArrayList()
        } else {
            noVendorsText!!.setVisibility(View.INVISIBLE)
        }
    }

    fun onDataChanged(){
        vendorArray = ArrayList(vendors.values).sortedBy { vendor -> vendor!!.distanceMiles }
        checkNoVendors()
        if (vendorAdapter == null) {
            vendorAdapter = VendorRecyclerAdapter(vendorArray, context!!)
            vendor_list.layoutManager = layoutManager
            vendor_list.adapter = vendorAdapter

        } else {
            vendorAdapter!!.updateElements(vendorArray)
            vendorAdapter!!.notifyDataSetChanged()
        }
    }

    fun startLocation(){
        if(checkPermission()) {
            locationMessage!!.visibility = View.INVISIBLE
            locationButton!!.visibility = View.INVISIBLE
            if (!locationService.startedUpdates){
                locationService.startLocationUpdates()
            }
        }else {
            //location not on. Tell user to turn it on
            locationMessage!!.visibility = View.VISIBLE
            locationButton!!.visibility = View.VISIBLE
        }
    }


    fun onLocationChanged(location: Location) {
        // New location has now been determined
        if(firstLocationUpdate){
            firstLocationUpdate = false
            getFirebaseData(location.latitude,location.longitude)
        }else{
            //recalculate distances and update recycler
            if (geoQuery!!.center != GeoLocation(location.latitude, location.longitude)) {
                geoQuery!!.center = GeoLocation(location.latitude, location.longitude)
            }
            if (vendor_list != null){
                for (vendor in vendors){
                    vendor.value!!.updateDistance(location)
                }
                onDataChanged()
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



}
