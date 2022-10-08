package com.xvlaze.clover.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.huawei.hms.maps.*
import com.huawei.hms.maps.model.*
import com.huawei.hms.site.api.model.Site
import com.xvlaze.clover.R
import com.xvlaze.clover.databinding.ActivityPharmaBinding
import com.xvlaze.clover.util.Constants.TAG
import timber.log.Timber
import java.util.*

class PharmaActivity : ThemedActivity(), OnMapReadyCallback {
    private var hMap: HuaweiMap? = null
    private lateinit var mMapView: MapView
    private var _hmsapikey: String? = 
    private lateinit var binding: ActivityPharmaBinding
    private lateinit var siteInfo: RelativeLayout
    private lateinit var viewModel: PharmaViewModel
    private lateinit var currentLocation: LatLng
    private var sites = mutableListOf<Site>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.setApiKey(_hmsapikey)

        binding = ActivityPharmaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(
            this,
            PharmaViewModel.MyViewModelFactory(application)
        )[PharmaViewModel::class.java]


        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mMapView = binding.mapView
        mMapView.onCreate(mapViewBundle)
        mMapView.getMapAsync(this)
        siteInfo = binding.siteInfo
        siteInfo.visibility = View.GONE

        viewModel.getCurrentLocation()

    }

    override fun onMapReady(huaweiMap: HuaweiMap) {
        //get map instance in a callback method
        Timber.tag(TAG).d("onMapReady: ")
        hMap = huaweiMap
        hMap!!.mapType = HuaweiMap.MAP_TYPE_NORMAL
        hMap!!.isMyLocationEnabled = true // Enable the my-location overlay.
        hMap!!.uiSettings.isMyLocationButtonEnabled = true // Enable the my-location icon.

        // Si es de noche, activamos el mapa nocturno.
        viewModel.getTheme()
        viewModel.theme.observe(this) { theme ->
            if (theme == 0 || theme == 3) {
                val style: MapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.night_map)
                hMap!!.setMapStyle(style)
            }
        }

        viewModel.lastLocation.observe(this) { location ->
            currentLocation = location
            val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 18f)
            hMap!!.animateCamera(cameraUpdate)
            _hmsapikey?.let { viewModel.searchNearbyPharmacies(location, it) }
        }

        viewModel.pois.observe(this) { sites ->
            this.sites = sites as MutableList<Site>
            if (sites.isEmpty()) {
                Snackbar.make(binding.mapView, getString(R.string.no_sites), Snackbar.LENGTH_INDEFINITE).show()
            }
            else {
                for (site in sites) {
                    val options = MarkerOptions()
                        .position(LatLng(site.location.lat, site.location.lng))
                        .clusterable(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                    hMap!!.addMarker(options)
                }
                hMap!!.setMarkersClustering(true)
            }
        }

        hMap!!.setOnMapClickListener {
            siteInfo.animate()
                .translationY(siteInfo.height.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        siteInfo.visibility = View.GONE
                    }
                })
        }

        hMap!!.setOnMarkerClickListener { marker: Marker ->
            val name: TextView = binding.name
            val address: TextView = binding.address
            val directions: LinearLayout = binding.directions
            for (site in sites) {
                if (site.location.lat == marker.position.latitude &&
                    site.location.lng == marker.position.longitude
                ) {
                    siteInfo.visibility = View.VISIBLE
                    name.text = site.name
                    address.text = site.formatAddress
                    try {
                        val phoneNumber = site.poi.phone
                        val call: LinearLayout = binding.call
                        call.visibility = View.GONE

                        if (phoneNumber != "") {
                            call.visibility = View.VISIBLE
                            call.setOnClickListener {
                                viewModel.handleCall(this)
                                viewModel.isCallPermissionGranted.observe(this) { isGranted ->
                                    if (isGranted) {
                                        val nombreTratamiento =
                                            intent.getStringExtra("nombreTratamiento")
                                        val blisterQuantity =
                                            intent.getIntExtra(
                                                "cantidadPaquete",
                                                0
                                            )
                                        if (nombreTratamiento != null && blisterQuantity != 0) {
                                            val builder =
                                                AlertDialog.Builder(
                                                    this@PharmaActivity,
                                                    R.style.CustomAlertDialog
                                                )
                                            val viewGroup: ViewGroup =
                                                findViewById(R.id.content)
                                            val dialogView: View =
                                                LayoutInflater.from(this@PharmaActivity)
                                                    .inflate(
                                                        R.layout.dialog_call,
                                                        viewGroup,
                                                        false
                                                    )
                                            builder.setView(dialogView)
                                            val dialogCall =
                                                builder.create()
                                            val treatmentName =
                                                dialogView.findViewById<TextView>(
                                                    R.id.nombre_tratamiento
                                                )
                                            treatmentName.text = nombreTratamiento
                                            val quantity =
                                                dialogView.findViewById<TextView>(
                                                    R.id.cantidad_tratamiento
                                                )
                                            // FIXME: Posible bug
                                            quantity.text =
                                                java.lang.String.format(
                                                    Locale.getDefault(),
                                                    applicationContext.resources.getQuantityString(
                                                        R.plurals.blister_qty,
                                                        blisterQuantity,
                                                        blisterQuantity
                                                    ),
                                                    blisterQuantity
                                                )
                                            val btnCall =
                                                dialogView.findViewById<LinearLayout>(
                                                    R.id.call
                                                )
                                            btnCall.setOnClickListener {
                                                call(phoneNumber)
                                                dialogCall.dismiss()
                                            }
                                            val btnBack =
                                                dialogView.findViewById<Button>(
                                                    R.id.btnBack
                                                )
                                            btnBack.setOnClickListener { dialogCall.dismiss() }
                                            dialogCall.show()
                                        } else {
                                            call(phoneNumber)
                                        }
                                    }
                                    else {
                                        showSettingsDialog()
                                    }
                                }
                            }
                        }

                        if (packageManager.getApplicationInfo(
                                "com.huawei.maps.app",
                                0
                            ).enabled
                        ) {
                            directions.setOnClickListener {
                                val sourceLat = currentLocation.latitude
                                val sourceLon = currentLocation.longitude
                                val destLat = site.location.lat
                                val destLon = site.location.lng
                                // FIXME: El %s admite cualquier tipo de datos. Si es null, se lo puede comer y explotará.
                                val uriString = String.format(
                                    "mapapp://route?saddr=%s,%s&daddr=%s,%s",
                                    sourceLat,
                                    sourceLon,
                                    destLat,
                                    destLon
                                )
                                val contentUrl =
                                    Uri.parse(uriString)
                                val intent =
                                    Intent(Intent.ACTION_VIEW, contentUrl)
                                if (intent.resolveActivity(packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        val dirText: TextView = findViewById(R.id.dir_text)
                        dirText.setText(R.string.install_maps)
                        e.printStackTrace()
                        directions.setOnClickListener {
                            val url = getString(R.string.map_app_url)
                            val i = Intent(Intent.ACTION_VIEW)
                            i.data = Uri.parse(url)
                            startActivity(i)
                        }
                    }
                    siteInfo.animate()
                        .translationY(0f)
                        .setListener(null)
                }
            }
            false
        }
    }

    private fun call(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        startActivity(callIntent)
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val viewGroup = findViewById<ViewGroup>(R.id.content)
        val dialogView: View =
            LayoutInflater.from(this).inflate(R.layout.dialog_permissions, viewGroup, false)
        builder.setView(dialogView)
        val dialogCall = builder.create()
        val permissionReq = dialogView.findViewById<TextView>(R.id.permission_req)
        permissionReq.setText(R.string.call_permission)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            dialogCall.cancel()
            openSettings()
        }
        val btnDismiss = dialogView.findViewById<Button>(R.id.btnDismiss)
        btnDismiss.setOnClickListener { dialogCall.cancel() }
        dialogCall.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101) // FIXME
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mMapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView.onDestroy()
        hMap!!.clear()
    }

    override fun onPause() {
        mMapView.onPause()
        super.onPause()
    }

    companion object {
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    }
}