package com.example.tweetmap.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.tweetmap.data.Resource
import com.example.tweetmap.databinding.FragmentTweetListBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TweetListFragment : Fragment() {
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: FragmentTweetListBinding
    private val tweetListViewModel: TweetListViewModel by viewModels()
    private lateinit var locationManager: LocationManager
    private val locationListener = LocationListener {
        moveMapToCurrentLocation(it)
        /*googleMap.addMarker(
                MarkerOptions().position(currentLocation).title("Marker Title").snippet("Marker Description")
            )*/
    }

    @SuppressLint("MissingPermission")
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                googleMap.isMyLocationEnabled = true
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 90 * 1000,
                    10.0.toFloat(), locationListener
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTweetListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.map.onCreate(savedInstanceState)
        locationManager =
            activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setUpGoogleMap()
        tweetListViewModel.getToken()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(keyWord: String?): Boolean {
                keyWord?.let {
                    tweetListViewModel.postRules(it)
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })
        setObservers()

    }

    private fun setUpGoogleMap() {
        binding.map.onResume()
        try {
            MapsInitializer.initialize(requireActivity().applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.map.getMapAsync { mMap ->
            googleMap = mMap
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                googleMap.isMyLocationEnabled = true
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 90 * 1000,
                    10.0.toFloat(), locationListener
                )
            }
        }
    }

    private fun setObservers() {
        tweetListViewModel.streamResponseLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is Resource.Loading -> Log.e("Loading", "true") /*showLoadingView()*/
                is Resource.Success -> Log.e("Success", Gson().toJson(it.data))
                is Resource.DataError -> {
                    /* showDataView(false)
                     status.errorCode?.let { recipesListViewModel.showToastMessage(it) }*/
                }
            }
        })
    }

    private fun moveMapToCurrentLocation(location: Location) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        val cameraPosition = CameraPosition.Builder().target(currentLocation).zoom(12f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }


}