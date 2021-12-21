package com.huster.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.huster.myapplication.databinding.ActivityMainBinding
import com.huster.myapplication.models.ImageInfo
import com.huster.myapplication.models.UserModel
import com.huster.myapplication.ui.personal.OnUserChangeListener
import com.huster.myapplication.ui.personal.PersonalActivity
import com.huster.myapplication.ui.personal.UserManager
import com.huster.myapplication.utils.LoadingScreen
import com.huster.myapplication.utils.PermissionUtils
import com.huster.myapplication.utils.PermissionUtils.requestPermission
import android.location.Criteria

import android.location.LocationManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import com.huster.myapplication.models.UserLocationInfo
import com.huster.myapplication.ui.otp.VerifyOtpCodeActivity
import java.util.*


class MainActivity : AppCompatActivity(), OnUserChangeListener, OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, MainActivity::class.java)
            context.startActivity(starter)
        }
    }

    private var permissionDenied = false
    private val firebaseDatabase by lazy {
        Firebase.database
    }

    private val firebaseStorage by lazy {
        Firebase.storage
    }

    private val isLoading = MutableLiveData<Boolean>(false)

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UserManager.addListener(this)

        binding.btnChooseImage.setOnClickListener {
            ImagePicker.with(this)
                .compress(1024)
                .maxResultSize(
                    1080,
                    1080
                ).createIntent { intent ->
                    isLoading.value = true
                    startForProfileImageResult.launch(intent)
                }
        }

        binding.btnUpload.setOnClickListener {
            uriObservable.value?.let {
                upload(it)
            }
        }

        binding.mapView.run {
            onCreate(savedInstanceState)
            getMapAsync(this@MainActivity)
        }

        uriObservable.observe(this, {
            binding.ivImage.setImageURI(it)
            if (it != null) {
                binding.btnUpload.enable()
            } else {
                binding.btnUpload.disable()
            }
        })

        isLoading.observe(this, {
            if(it) {
                LoadingScreen.displayLoadingWithText(this)
            } else {
                LoadingScreen.hideLoading()
            }
        })
    }

    private fun upload(uri: Uri) {
        isLoading.value = true
        try {
            UserManager.currentUserModel?.phoneNumber?.let { number ->
                firebaseStorage.reference.child(number).putFile(uri)
                    .addOnCompleteListener { taskSnapshot ->
                        run {
                            val result = taskSnapshot.result.metadata?.reference?.downloadUrl
                            result?.addOnSuccessListener { uri ->
                                isLoading.value = false
                                upToServer(uri.toString())
                            }?.addOnFailureListener {
                                isLoading.value = false
                                it.printStackTrace()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        isLoading.value = false
                    }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            isLoading.value = false
        }
    }

    private fun upToServer(urlImage: String) {
        isLoading.value = true
        try {
            val myLocation = getMyLocation()
            val info = ImageInfo(urlImage, myLocation.first, myLocation.second, Date().time)

            UserManager.currentUserModel?.phoneNumber?.let { number ->
                val refRoot = firebaseDatabase.reference.child("${UserModel.IMAGES_PATH}/$number")

                refRoot.get().addOnSuccessListener {
                    val value = it.value
                    if (value == null) {
                        try {
                            val newList = mutableListOf(info)
                            val currentUserLocations = UserLocationInfo(info.createAt, newList)

                            refRoot.addImageLocationValue(currentUserLocations)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showErrorToast(e.message)
                        }
                    } else {
                        val jsonValue = Gson().toJson(value)
                        try {
                            val currentUserLocations = Gson().fromJson(jsonValue, UserLocationInfo::class.java)
                            val newList = currentUserLocations.imagesHistory.toMutableList().apply {
                                add(0, info)
                            }
                            currentUserLocations.imagesHistory = newList

                            refRoot.addImageLocationValue(currentUserLocations)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showErrorToast(e.message)
                        }
                    }
                }
                    .addOnFailureListener {
                        it.printStackTrace()
                    }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun DatabaseReference.addImageLocationValue(locationsInfo: UserLocationInfo) {
        this.setValue(locationsInfo)
            .addOnFailureListener {
                isLoading.value = false
                showToast("Upload thất bại, vui lòng thử lại")
            }.addOnCompleteListener {
                isLoading.value = false
                showToast("Upload success")
            }
    }

    private lateinit var map: GoogleMap

    override fun onChange(user: UserModel?) {
        user?.let {
            binding.tvPhoneNumber.text = it.phoneNumber
            binding.tvUserName.text = it.name

            binding.personal.setOnClickListener {
                PersonalActivity.start(this)
            }

            binding.tvAddress.text = it.address
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            val myLocation = getMyLocation()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(myLocation.first?: 0.0, myLocation.second?: 0.0),
                15F
            ))
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    private fun showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true)
            .show(supportFragmentManager, "dialog")
    }


    override fun onMyLocationButtonClick(): Boolean {
        //showToast("MyLocation button clicked")
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
        showToast("Current location:\n$location")
    }

    private val uriObservable = MutableLiveData<Uri?>(null)

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            LoadingScreen.hideLoading()
            val resultCode = result.resultCode
            val data = result.data
            uriObservable.value = data?.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    showToast("Thành công")
                }
                ImagePicker.RESULT_ERROR -> {
                    showToast(ImagePicker.getError(data))
                }
                else -> {
                    showToast("Task Cancelled")
                }
            }
        }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        UserManager.removeListener(this)
        binding.mapView.onDestroy()
    }

    @SuppressLint("MissingPermission")
    fun getMyLocation(): Pair<Double?, Double?> {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()

        val location = try {
            locationManager.getLastKnownLocation(
                locationManager.getBestProvider(criteria, false)!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val latitude = location?.latitude
        val longitude = location?.longitude
        return Pair(latitude, longitude)
    }
}