package com.schibstedspain.leku

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiClient
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.HuaweiMap.MAP_TYPE_NORMAL
import com.huawei.hms.maps.HuaweiMap.MAP_TYPE_SATELLITE
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.SupportMapFragment
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MapStyleOptions
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.site.api.SearchServiceFactory
import com.schibstedspain.leku.geocoder.AndroidGeocoderDataSource
import com.schibstedspain.leku.geocoder.GeocoderPresenter
import com.schibstedspain.leku.geocoder.GeocoderRepository
import com.schibstedspain.leku.geocoder.GeocoderViewInterface
import com.schibstedspain.leku.geocoder.HuaweiGeocoderDataSource
import com.schibstedspain.leku.geocoder.places.HuaweiSitesDataSource
import com.schibstedspain.leku.geocoder.timezone.HuaweiTimeZoneDataSource
import com.schibstedspain.leku.locale.DefaultCountryLocaleRect
import com.schibstedspain.leku.locale.SearchZoneRect
import com.schibstedspain.leku.permissions.PermissionUtils
import com.schibstedspain.leku.tracker.TrackEvents
import com.schibstedspain.leku.utils.BaseLocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val ZIPCODE = "zipcode"
const val ADDRESS = "address"
const val LOCATION_ADDRESS = "location_address"
const val TRANSITION_BUNDLE = "transition_bundle"
const val LAYOUTS_TO_HIDE = "layouts_to_hide"
const val SEARCH_COUNTRY = "search_country"
const val SEARCH_LANGUAGE = "search_language"
const val SEARCH_POI_TYPES = "search_poi_types"
const val SEARCH_ZONE = "search_zone"
const val SEARCH_ZONE_RECT = "search_zone_rect"
const val SEARCH_ZONE_DEFAULT_LOCALE = "search_zone_default_locale"
const val BACK_PRESSED_RETURN_OK = "back_pressed_return_ok"
const val ENABLE_SATELLITE_VIEW = "enable_satellite_view"
const val ENABLE_LOCATION_PERMISSION_REQUEST = "enable_location_permission_request"
const val ENABLE_GOOGLE_TIME_ZONE = "enable_google_time_zone"
const val POIS_LIST = "pois_list"
const val LEKU_POI = "leku_poi"
const val ENABLE_VOICE_SEARCH = "enable_voice_search"
const val TIME_ZONE_ID = "time_zone_id"
const val TIME_ZONE_DISPLAY_NAME = "time_zone_display_name"
const val MAP_STYLE = "map_style"
const val UNNAMED_ROAD_VISIBILITY = "unnamed_road_visibility"
const val WITH_LEGACY_LAYOUT = "with_legacy_layout"
private const val GEOLOC_API_KEY = "geoloc_api_key"
private const val PLACES_API_KEY = "places_api_key"
private const val LOCATION_KEY = "location_key"
private const val LAST_LOCATION_QUERY = "last_location_query"
private const val OPTIONS_HIDE_STREET = "street"
private const val OPTIONS_HIDE_CITY = "city"
private const val OPTIONS_HIDE_ZIPCODE = "zipcode"
private const val UNNAMED_ROAD_WITH_COMMA = "Unnamed Road, "
private const val UNNAMED_ROAD_WITH_HYPHEN = "Unnamed Road - "
private const val REQUEST_PLACE_PICKER = 6655
private const val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
private const val DEFAULT_ZOOM = 16
private const val WIDER_ZOOM = 6
private const val MIN_CHARACTERS = 2
private const val DEBOUNCE_TIME = 400

class LocationPickerActivity : AppCompatActivity(),
        OnMapReadyCallback,
        HuaweiApiClient.ConnectionCallbacks,
        HuaweiApiClient.OnConnectionFailedListener,
        LocationCallbackX,
        HuaweiMap.OnMapLongClickListener,
        GeocoderViewInterface,
        HuaweiMap.OnMapClickListener {

    private var map: HuaweiMap? = null
    private var huaweiApiClient: HuaweiApiClient? = null
    private var currentLocation: Location? = null
    private var currentLekuPoi: LekuPoi? = null
    private var geocoderPresenter: GeocoderPresenter? = null

    private var adapter: ArrayAdapter<String>? = null
    private var searchView: EditText? = null
    private var street: TextView? = null
    private var coordinates: TextView? = null
    private var longitude: TextView? = null
    private var latitude: TextView? = null
    private var city: TextView? = null
    private var zipCode: TextView? = null
    private var locationInfoLayout: FrameLayout? = null
    private var progressBar: ProgressBar? = null
    private var listResult: ListView? = null
    private var searchResultsList: RecyclerView? = null
    private var searchAdapter: RecyclerView.Adapter<*>? = null
    private lateinit var linearLayoutManager: RecyclerView.LayoutManager
    private var clearSearchButton: ImageView? = null
    private var searchOption: MenuItem? = null
    private var clearLocationButton: ImageButton? = null
    private var searchEditLayout: LinearLayout? = null
    private var searchFrameLayout: FrameLayout? = null

    private val locationList = ArrayList<Address>()
    private var locationNameList: MutableList<String> = ArrayList()
    private var hasWiderZoom = false
    private val bundle = Bundle()
    private var selectedAddress: Address? = null
    private var isLocationInformedFromBundle = false
    private var isStreetVisible = true
    private var isCityVisible = true
    private var isZipCodeVisible = true
    private var shouldReturnOkOnBackPressed = false
    private var enableSatelliteView = true
    private var enableLocationPermissionRequest = true
    private var huaweiSitesApiKey: String? = null
    private var isGoogleTimeZoneEnabled = false
    private var searchZone: String? = null
    private var searchZoneRect: SearchZoneRect? = null
    private var searchCountry: String? = null
    private var searchLanguage: String? = null
    private var searchPoiTypes: String? = null
    private var isSearchZoneWithDefaultLocale = false
    private var poisList: List<LekuPoi>? = null
    private var lekuPoisMarkersMap: MutableMap<String, LekuPoi>? = null
    private var currentMarker: Marker? = null
    private var isVoiceSearchEnabled = true
    private var isUnnamedRoadVisible = true
    private var mapStyle: Int? = null
    private var isLegacyLayoutEnabled = false
    private var isSearchLayoutShown = false
    private lateinit var toolbar: Toolbar
    private lateinit var timeZone: TimeZone

    fun EditText.afterTextChangedDebounce(delayMillis: Long, input: (String) -> Unit) {
        var lastInput = ""
        var debounceJob: Job? = null
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                if (editable != null) {
                    val newtInput = editable.toString()
                    debounceJob?.cancel()
                    if (lastInput != newtInput) {
                        lastInput = newtInput
                        debounceJob = uiScope.launch {
                            delay(delayMillis)
                            if (lastInput == newtInput) {
                                input(newtInput)
                            }
                        }
                    }
                }
            }

            override fun beforeTextChanged(cs: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(cs: CharSequence?, start: Int, before: Int, count: Int) {}
    })}

    private val textChangeAction: ((string: String)->Unit) = { string ->
        if (string.isBlank()) {
            if (isLegacyLayoutEnabled) {
                adapter?.let {
                    it.clear()
                    it.notifyDataSetChanged()
                }
            } else {
                searchAdapter?.let {
                    it.notifyDataSetChanged()
                }
            }
            showLocationInfoLayout()
            clearSearchButton?.visibility = View.INVISIBLE
            searchOption?.setIcon(R.drawable.leku_ic_mic_legacy)
            updateVoiceSearchVisibility()
        } else {
            if (string.length > MIN_CHARACTERS) {
                retrieveLocationFrom(string)
            }
            clearSearchButton?.visibility = View.VISIBLE
            searchOption?.setIcon(R.drawable.leku_ic_search)
            searchOption?.isVisible = true
        }
    }

    private val defaultZoom: Int
        get() {
            return if (hasWiderZoom) {
                WIDER_ZOOM
            } else {
                DEFAULT_ZOOM
            }
        }

    private val locationAddress: String
        get() {
            var locationAddress = ""
            street?.let {
                if (it.text.toString().isNotEmpty()) {
                    locationAddress = if (isUnnamedRoadVisible) {
                        it.text.toString()
                    } else {
                        removeUnnamedRoad(it.text.toString())
                    }
                }
            }
            city?.let {
                if (it.text.toString().isNotEmpty()) {
                    if (locationAddress.isNotEmpty()) {
                        locationAddress += ", "
                    }
                    locationAddress += it.text.toString()
                }
            }
            return locationAddress
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateValuesFromBundle(savedInstanceState)
        setUpContentView()
        setUpMainVariables()
        setUpResultsList()
        setUpToolBar()
        checkLocationPermission()
        setUpSearchView()
        setUpFloatingButtons()
        buildHuaweiApiClient()
        track(TrackEvents.ON_LOAD_LOCATION_PICKER)
    }

    private fun setUpContentView() {
        if (isLegacyLayoutEnabled) {
            setContentView(R.layout.leku_activity_location_picker_legacy)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags: Int = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                window.decorView.systemUiVisibility = flags
            }

            setContentView(R.layout.leku_activity_location_picker)
        }
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    private fun checkLocationPermission() {
        if (enableLocationPermissionRequest &&
                PermissionUtils.shouldRequestLocationStoragePermission(applicationContext)) {
            PermissionUtils.requestLocationPermission(this)
        }
    }

    private fun track(event: TrackEvents) {
        LocationPicker.getTracker().onEventTracked(event)
    }

    private fun setUpMainVariables() {
        var sitesDataSource: HuaweiSitesDataSource? = null
        var timeZoneDataSource: HuaweiTimeZoneDataSource? = null
        if (!huaweiSitesApiKey.isNullOrEmpty()) {
            val encodedKey = URLEncoder.encode(huaweiSitesApiKey, "utf-8")
            val client = SearchServiceFactory.create(this, encodedKey)
            sitesDataSource = HuaweiSitesDataSource(client)
            timeZoneDataSource = HuaweiTimeZoneDataSource(encodedKey)
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        val nativeGeocoder = AndroidGeocoderDataSource(geocoder)
        val huaweiGeocoder = if (sitesDataSource != null) {
            HuaweiGeocoderDataSource(sitesDataSource)
        } else {
            nativeGeocoder
        }
        val geocoderRepository = GeocoderRepository(nativeGeocoder, huaweiGeocoder)
        geocoderPresenter = GeocoderPresenter(
                locationService = BaseLocationService(applicationContext),
                geocoderRepository = geocoderRepository,
                huaweiSitesDataSource = sitesDataSource,
                timeZoneDataSource = timeZoneDataSource
        )
        geocoderPresenter?.setUI(this)
        progressBar = findViewById(R.id.loading_progress_bar)
        progressBar?.visibility = View.GONE
        locationInfoLayout = findViewById(R.id.location_info)
        longitude = findViewById(R.id.longitude)
        latitude = findViewById(R.id.latitude)
        street = findViewById(R.id.street)
        coordinates = findViewById(R.id.coordinates)
        city = findViewById(R.id.city)
        zipCode = findViewById(R.id.zipCode)
        clearSearchButton = findViewById(R.id.leku_clear_search_image)
        clearSearchButton?.setOnClickListener {
            searchView?.setText("")
        }
        locationNameList = ArrayList()
        clearLocationButton = findViewById(R.id.btnClearSelectedLocation)
        clearLocationButton?.setOnClickListener {
            currentLocation = null
            currentLekuPoi = null
            currentMarker?.remove()
            changeLocationInfoLayoutVisibility(View.GONE)
        }
        searchEditLayout = findViewById(R.id.leku_search_touch_zone)
        searchFrameLayout = findViewById(R.id.search_frame_layout)
    }

    private fun setUpResultsList() {
        if (isLegacyLayoutEnabled) {
            listResult = findViewById(R.id.resultlist)
            adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationNameList)
            listResult?.let {
                it.adapter = adapter
                it.setOnItemClickListener { _, _, i, _ ->
                    setNewLocation(locationList[i])
                    changeListResultVisibility(View.GONE)
                    closeKeyboard()
                }
            }
        } else {
            linearLayoutManager = LinearLayoutManager(this)
            searchAdapter = LocationSearchAdapter(locationNameList, object : LocationSearchAdapter.SearchItemClickListener {
                override fun onItemClick(position: Int) {
                    setNewLocation(locationList[position])
                    changeListResultVisibility(View.GONE)
                    closeKeyboard()
                    hideSearchLayout()
                }
            })
            searchResultsList = findViewById<RecyclerView>(R.id.search_result_list).apply {
                setHasFixedSize(true)
                layoutManager = linearLayoutManager
                adapter = searchAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    private fun setUpToolBar() {
        toolbar = findViewById(R.id.map_search_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            if (!isLegacyLayoutEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val drawable = resources.getDrawable(R.drawable.leku_ic_close, theme)
                    drawable.setTint(getThemeColorPrimary())
                    it.setHomeAsUpIndicator(drawable)
                } else {
                    it.setHomeAsUpIndicator(R.drawable.leku_ic_close)
                }
            }
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
    }

    private fun getThemeColorPrimary(): Int {
        val value = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, value, true)
        return value.data
    }

    private fun switchToolbarVisibility() {
        if (isHuaweiServicesAvailable()) {
            toolbar.visibility = View.VISIBLE
        } else {
            toolbar.visibility = View.GONE
        }
    }

    private fun setUpSearchView() {
        searchView = findViewById(R.id.leku_search)
        searchView?.setOnEditorActionListener { v, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH && v.text.toString().isNotEmpty()) {
                retrieveLocationFrom(v.text.toString())
                closeKeyboard()
                handled = true
            }
            handled
        }

        searchView?.afterTextChangedDebounce(DEBOUNCE_TIME.toLong()) {
            textChangeAction(it)
        }

        if (!isLegacyLayoutEnabled) {
            searchView?.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    showSearchLayout()
                }
            }
        }
    }

    private fun showSearchLayout() {
        supportActionBar?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val drawable = resources.getDrawable(R.drawable.leku_ic_back, theme)
                drawable.setTint(getThemeColorPrimary())
                it.setHomeAsUpIndicator(drawable)
            } else {
                it.setHomeAsUpIndicator(R.drawable.leku_ic_back)
            }
        }
        searchFrameLayout?.setBackgroundResource(R.color.leku_white)
        searchEditLayout?.setBackgroundResource(R.drawable.leku_search_text_with_border_background)
        searchResultsList?.visibility = View.VISIBLE
        isSearchLayoutShown = true
    }

    private fun hideSearchLayout() {
        supportActionBar?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val drawable = resources.getDrawable(R.drawable.leku_ic_close, theme)
                drawable.setTint(getThemeColorPrimary())
                it.setHomeAsUpIndicator(drawable)
            } else {
                it.setHomeAsUpIndicator(R.drawable.leku_ic_close)
            }
        }
        searchFrameLayout?.setBackgroundResource(android.R.color.transparent)
        searchEditLayout?.setBackgroundResource(R.drawable.leku_search_text_background)
        searchResultsList?.visibility = View.GONE
        searchView?.clearFocus()
        isSearchLayoutShown = false
    }

    private fun setUpFloatingButtons() {
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btnMyLocation)
        btnMyLocation.setOnClickListener {
            checkLocationPermission()
            geocoderPresenter?.getLastKnownLocation()
            track(TrackEvents.ON_LOCALIZED_ME)
        }

        val btnAcceptLocation = if (isLegacyLayoutEnabled) {
            findViewById<FloatingActionButton>(R.id.btnAccept)
        } else {
            findViewById<Button>(R.id.btnAccept)
        }
        btnAcceptLocation.setOnClickListener { returnCurrentPosition() }

        val btnSatellite = findViewById<FloatingActionButton>(R.id.btnSatellite)
        btnSatellite?.setOnClickListener {
            map?.let {
                it.mapType = if (it.mapType == MAP_TYPE_SATELLITE) MAP_TYPE_NORMAL else MAP_TYPE_SATELLITE
                if (isLegacyLayoutEnabled) {
                    btnSatellite.setImageResource(
                            if (it.mapType == MAP_TYPE_SATELLITE)
                                R.drawable.leku_ic_satellite_off_legacy
                            else
                                R.drawable.leku_ic_satellite_on_legacy)
                } else {
                    btnSatellite.setImageResource(
                            if (it.mapType == MAP_TYPE_SATELLITE)
                                R.drawable.leku_ic_maps
                            else
                                R.drawable.leku_ic_satellite)
                }
            }
        }
        if (enableSatelliteView) btnSatellite.show() else btnSatellite.hide()
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        val transitionBundle = intent.extras
        transitionBundle?.let {
            getTransitionBundleParams(it)
        }
        savedInstanceState?.let {
            getSavedInstanceParams(it)
        }
        updateAddressLayoutVisibility()
        updateVoiceSearchVisibility()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isLegacyLayoutEnabled) {
            val inflater = menuInflater
            inflater.inflate(R.menu.leku_toolbar_menu, menu)
            searchOption = menu.findItem(R.id.action_voice)
            updateVoiceSearchVisibility()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!isLegacyLayoutEnabled && isSearchLayoutShown) {
                hideSearchLayout()
            } else {
                onBackPressed()
            }
            return true
        } else if (id == R.id.action_voice) {
            searchView?.let {
                if (it.text.toString().isEmpty()) {
                    startVoiceRecognitionActivity()
                } else {
                    retrieveLocationFrom(it.text.toString())
                    closeKeyboard()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (PermissionUtils.isLocationPermissionGranted(applicationContext)) {
            geocoderPresenter?.getLastKnownLocation()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PLACE_PICKER -> if (resultCode == Activity.RESULT_OK && data != null) {
                val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                searchView = findViewById(R.id.leku_search)
                retrieveLocationFrom(matches[0])
            }
            else -> {
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        huaweiApiClient?.connect(this)
        geocoderPresenter?.setUI(this)
    }

    override fun onStop() {
        huaweiApiClient?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
        geocoderPresenter?.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        switchToolbarVisibility()
    }

    override fun onDestroy() {
        /*textWatcher?.let {
            searchView?.removeTextChangedListener(it)
        }*/
        huaweiApiClient?.removeConnectionSuccessListener(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!shouldReturnOkOnBackPressed || isLocationInformedFromBundle) {
            setResult(Activity.RESULT_CANCELED)
            track(TrackEvents.CANCEL)
            finish()
        } else {
            returnCurrentPosition()
        }
    }

    override fun onMapReady(map: HuaweiMap) {
        if (this.map == null) {
            this.map = map
            setMapStyle()
            setDefaultMapSettings()
            setCurrentPositionLocation()
            setPois()
        }
    }

    override fun onConnected() {
        if (currentLocation == null) {
            geocoderPresenter?.getLastKnownLocation()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        huaweiApiClient?.connect(this)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST)
            } catch (e: IntentSender.SendIntentException) {
                track(TrackEvents.GOOGLE_API_CONNECTION_FAILED)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        currentLocation?.let {
            savedInstanceState.putParcelable(LOCATION_KEY, it)
        }
        searchView?.let {
            savedInstanceState.putString(LAST_LOCATION_QUERY, it.text.toString())
        }
        if (bundle.containsKey(TRANSITION_BUNDLE)) {
            savedInstanceState.putBundle(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
        }
        poisList?.let {
            savedInstanceState.putParcelableArrayList(POIS_LIST, ArrayList(it))
        }
        savedInstanceState.putBoolean(ENABLE_SATELLITE_VIEW, enableSatelliteView)
        savedInstanceState.putBoolean(ENABLE_LOCATION_PERMISSION_REQUEST, enableLocationPermissionRequest)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastQuery = savedInstanceState.getString(LAST_LOCATION_QUERY, "")
        if ("" != lastQuery) {
            retrieveLocationFrom(lastQuery)
        }
        currentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        if (currentLocation != null) {
            setCurrentPositionLocation()
        }
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        }
        if (savedInstanceState.containsKey(POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(POIS_LIST)
        }
        if (savedInstanceState.containsKey(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = savedInstanceState.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (savedInstanceState.containsKey(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        currentLekuPoi = null
        setNewPosition(latLng)
        track(TrackEvents.ON_LOCALIZED_BY_POI)
    }

    override fun onMapClick(latLng: LatLng) {
        currentLekuPoi = null
        setNewPosition(latLng)
        track(TrackEvents.SIMPLE_ON_LOCALIZE_BY_POI)
    }

    private fun setNewPosition(latLng: LatLng) {
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.leku_network_resource))
        }
        currentLocation?.latitude = latLng.latitude
        currentLocation?.longitude = latLng.longitude
        setCurrentPositionLocation()
    }

    override fun willLoadLocation() {
        progressBar?.visibility = View.VISIBLE
        changeListResultVisibility(View.GONE)
    }

    override fun showLocations(addresses: List<Address>) {
        fillLocationList(addresses)
        if (addresses.isEmpty()) {
            Toast.makeText(applicationContext, R.string.leku_no_search_results, Toast.LENGTH_LONG)
                    .show()
        } else {
            updateLocationNameList(addresses)
            if (hasWiderZoom) {
                //searchView?.setText("")
            }
            if (addresses.size == 1) {
                //setNewLocation(addresses[0])
            }
            if (isLegacyLayoutEnabled) {
                adapter?.notifyDataSetChanged()
            } else {
                searchAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun showDebouncedLocations(addresses: List<Address>) {
        fillLocationList(addresses)
        if (addresses.isNotEmpty()) {
            updateLocationNameList(addresses)
            if (isLegacyLayoutEnabled) {
                adapter?.notifyDataSetChanged()
            } else {
                searchAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun didLoadLocation() {
        progressBar?.visibility = View.GONE

        changeListResultVisibility(if (locationList.size >= 1) View.VISIBLE else View.GONE)

        if (locationList.size == 1) {
            changeLocationInfoLayoutVisibility(View.VISIBLE)
        } else {
            changeLocationInfoLayoutVisibility(View.GONE)
        }
        track(TrackEvents.ON_SEARCH_LOCATIONS)
    }

    private fun changeListResultVisibility(visibility: Int) {
        if (isLegacyLayoutEnabled) {
            listResult?.visibility = visibility
        } else {
            searchResultsList?.visibility = visibility
        }
    }

    private fun changeLocationInfoLayoutVisibility(visibility: Int) {
        locationInfoLayout?.visibility = visibility
    }

    private fun showCoordinatesLayout() {
        longitude?.visibility = View.VISIBLE
        latitude?.visibility = View.VISIBLE
        coordinates?.visibility = View.VISIBLE
        street?.visibility = View.GONE
        city?.visibility = View.GONE
        zipCode?.visibility = View.GONE
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun showAddressLayout() {
        longitude?.visibility = View.GONE
        latitude?.visibility = View.GONE
        coordinates?.visibility = View.GONE
        if (isStreetVisible) {
            street?.visibility = View.VISIBLE
        }
        if (isCityVisible) {
            city?.visibility = View.VISIBLE
        }
        if (isZipCodeVisible) {
            zipCode?.visibility = View.VISIBLE
        }
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun updateAddressLayoutVisibility() {
        street?.visibility = if (isStreetVisible) View.VISIBLE else View.INVISIBLE
        city?.visibility = if (isCityVisible) View.VISIBLE else View.INVISIBLE
        zipCode?.visibility = if (isZipCodeVisible) View.VISIBLE else View.INVISIBLE
        longitude?.visibility = View.VISIBLE
        latitude?.visibility = View.VISIBLE
        coordinates?.visibility = View.VISIBLE
    }

    private fun updateVoiceSearchVisibility() {
        searchOption?.isVisible = isVoiceSearchEnabled
    }

    override fun showLoadLocationError() {
        progressBar?.visibility = View.GONE
        changeListResultVisibility(View.GONE)
        Toast.makeText(this, R.string.leku_load_location_error, Toast.LENGTH_LONG).show()
    }

    override fun willGetLocationInfo(latLng: LatLng) {
        changeLocationInfoLayoutVisibility(View.VISIBLE)
        resetLocationAddress()
        setCoordinatesInfo(latLng)
    }

    override fun showLastLocation(location: Location) {
        currentLocation = location
        didGetLastLocation()
    }

    override fun didGetLastLocation() {
        if (currentLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.leku_no_geocoder_available, Toast.LENGTH_LONG).show()
                return
            }
        }
        setUpDefaultMapLocation()
    }

    override fun showLocationInfo(address: Pair<Address, TimeZone?>) {
        selectedAddress = address.first
        address.second?.let {
            timeZone = it
        }
        selectedAddress?.let {
            setLocationInfo(it)
        }
    }

    private fun setLocationEmpty() {
        this.street?.text = ""
        this.city?.text = ""
        this.zipCode?.text = ""
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    override fun didGetLocationInfo() {
        showLocationInfoLayout()
    }

    override fun showGetLocationInfoError() {
        setLocationEmpty()
    }

    private fun showLocationInfoLayout() {
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun getSavedInstanceParams(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        } else {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            currentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        }
        setUpDefaultMapLocation()
        if (savedInstanceState.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(PLACES_API_KEY)) {
            huaweiSitesApiKey = savedInstanceState.getString(PLACES_API_KEY, "")
        }
        if (savedInstanceState.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = savedInstanceState.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (savedInstanceState.keySet().contains(SEARCH_COUNTRY)) {
            searchCountry = savedInstanceState.getString(SEARCH_COUNTRY)
        }
        if (savedInstanceState.keySet().contains(SEARCH_LANGUAGE)) {
            searchLanguage = savedInstanceState.getString(SEARCH_LANGUAGE)
        }
        if (savedInstanceState.keySet().contains(SEARCH_POI_TYPES)) {
            searchPoiTypes = savedInstanceState.getString(SEARCH_POI_TYPES)
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE)) {
            searchZone = savedInstanceState.getString(SEARCH_ZONE)
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE_RECT)) {
            searchZoneRect = savedInstanceState.getParcelable(SEARCH_ZONE_RECT)
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE_DEFAULT_LOCALE)) {
            isSearchZoneWithDefaultLocale = savedInstanceState.getBoolean(SEARCH_ZONE_DEFAULT_LOCALE, false)
        }
        if (savedInstanceState.keySet().contains(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = savedInstanceState.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (savedInstanceState.keySet().contains(POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(POIS_LIST)
        }
        if (savedInstanceState.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (savedInstanceState.keySet().contains(ENABLE_VOICE_SEARCH)) {
            isVoiceSearchEnabled = savedInstanceState.getBoolean(ENABLE_VOICE_SEARCH, true)
        }
        if (savedInstanceState.keySet().contains(UNNAMED_ROAD_VISIBILITY)) {
            isUnnamedRoadVisible = savedInstanceState.getBoolean(UNNAMED_ROAD_VISIBILITY, true)
        }
        if (savedInstanceState.keySet().contains(MAP_STYLE)) {
            mapStyle = savedInstanceState.getInt(MAP_STYLE)
        }
        if (savedInstanceState.keySet().contains(WITH_LEGACY_LAYOUT)) {
            isLegacyLayoutEnabled = savedInstanceState.getBoolean(WITH_LEGACY_LAYOUT, false)
        }
    }

    private fun getTransitionBundleParams(transitionBundle: Bundle) {
        bundle.putBundle(TRANSITION_BUNDLE, transitionBundle)
        if (transitionBundle.keySet().contains(LATITUDE) && transitionBundle.keySet()
                        .contains(LONGITUDE)) {
            setLocationFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(SEARCH_COUNTRY)) {
            searchCountry = transitionBundle.getString(SEARCH_COUNTRY)
        }
        if (transitionBundle.keySet().contains(SEARCH_LANGUAGE)) {
            searchLanguage = transitionBundle.getString(SEARCH_LANGUAGE)
        }
        if (transitionBundle.keySet().contains(SEARCH_POI_TYPES)) {
            searchPoiTypes = transitionBundle.getString(SEARCH_POI_TYPES)
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE)) {
            searchZone = transitionBundle.getString(SEARCH_ZONE)
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE_RECT)) {
            searchZoneRect = transitionBundle.getParcelable(SEARCH_ZONE_RECT)
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE_DEFAULT_LOCALE)) {
            isSearchZoneWithDefaultLocale = transitionBundle.getBoolean(SEARCH_ZONE_DEFAULT_LOCALE, false)
        }
        if (transitionBundle.keySet().contains(BACK_PRESSED_RETURN_OK)) {
            shouldReturnOkOnBackPressed = transitionBundle.getBoolean(BACK_PRESSED_RETURN_OK)
        }
        if (transitionBundle.keySet().contains(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = transitionBundle.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (transitionBundle.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = transitionBundle.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (transitionBundle.keySet().contains(POIS_LIST)) {
            poisList = transitionBundle.getParcelableArrayList(POIS_LIST)
        }
        if (transitionBundle.keySet().contains(PLACES_API_KEY)) {
            huaweiSitesApiKey = transitionBundle.getString(PLACES_API_KEY, "")
        }
        if (transitionBundle.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = transitionBundle.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (transitionBundle.keySet().contains(ENABLE_VOICE_SEARCH)) {
            isVoiceSearchEnabled = transitionBundle.getBoolean(ENABLE_VOICE_SEARCH, true)
        }
        if (transitionBundle.keySet().contains(UNNAMED_ROAD_VISIBILITY)) {
            isUnnamedRoadVisible = transitionBundle.getBoolean(UNNAMED_ROAD_VISIBILITY, true)
        }
        if (transitionBundle.keySet().contains(MAP_STYLE)) {
            mapStyle = transitionBundle.getInt(MAP_STYLE)
        }
        if (transitionBundle.keySet().contains(WITH_LEGACY_LAYOUT)) {
            isLegacyLayoutEnabled = transitionBundle.getBoolean(WITH_LEGACY_LAYOUT, false)
        }
    }

    private fun setLayoutVisibilityFromBundle(transitionBundle: Bundle) {
        val options = transitionBundle.getString(LAYOUTS_TO_HIDE)
        if (options != null && options.contains(OPTIONS_HIDE_STREET)) {
            isStreetVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_CITY)) {
            isCityVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_ZIPCODE)) {
            isZipCodeVisible = false
        }
    }

    private fun setLocationFromBundle(transitionBundle: Bundle) {
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.leku_network_resource))
        }
        currentLocation?.latitude = transitionBundle.getDouble(LATITUDE)
        currentLocation?.longitude = transitionBundle.getDouble(LONGITUDE)
        setCurrentPositionLocation()
        isLocationInformedFromBundle = true
    }

    private fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.leku_voice_search_promp))
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                getString(R.string.leku_voice_search_extra_language))

        if (isHuaweiServicesAvailable()) {
            try {
                startActivityForResult(intent, REQUEST_PLACE_PICKER)
            } catch (e: ActivityNotFoundException) {
                track(TrackEvents.START_VOICE_RECOGNITION_ACTIVITY_FAILED)
            }
        }
    }

    private fun isHuaweiServicesAvailable(): Boolean {
        /*val googleAPI = HuaweiApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(applicationContext)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, CONNECTION_FAILURE_RESOLUTION_REQUEST).show()
            }
            return false
        }*/
        return true
    }

    private fun setCoordinatesInfo(latLng: LatLng) {
        this.latitude?.text = String.format("%s: %s", getString(R.string.leku_latitude), latLng.latitude)
        this.longitude?.text = String.format("%s: %s", getString(R.string.leku_longitude), latLng.longitude)
        showCoordinatesLayout()
    }

    private fun resetLocationAddress() {
        street?.text = ""
        city?.text = ""
        zipCode?.text = ""
    }

    private fun setLocationInfo(address: Address) {
        street?.let {
            if (isUnnamedRoadVisible) {
                it.text = address.getAddressLine(0)
            } else {
                it.text = removeUnnamedRoad(address.getAddressLine(0))
            }
        }
        city?.text = if (isStreetEqualsCity(address)) "" else address.locality
        zipCode?.text = address.postalCode
        showAddressLayout()
    }

    private fun setLocationInfo(poi: LekuPoi) {
        this.currentLekuPoi = poi
        street?.text = poi.title
        city?.text = poi.address
        zipCode?.text = null
        showAddressLayout()
    }

    private fun isStreetEqualsCity(address: Address): Boolean {
        return address.getAddressLine(0) == address.locality
    }

    private fun setNewMapMarker(latLng: LatLng) {
        if (map != null) {
            currentMarker?.remove()
            val cameraPosition = CameraPosition.Builder().target(latLng)
                    .zoom(defaultZoom.toFloat())
                    .build()
            hasWiderZoom = false
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            currentMarker = addMarker(latLng)
            map?.setOnMarkerDragListener(object : HuaweiMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {
                }

                override fun onMarkerDrag(marker: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                    if (currentLocation == null) {
                        currentLocation = Location(getString(R.string.leku_network_resource))
                    }
                    currentLekuPoi = null
                    currentLocation?.longitude = marker.position.longitude
                    currentLocation?.latitude = marker.position.latitude
                    setCurrentPositionLocation()
                }
            })
        }
    }

    private fun retrieveLocationFrom(query: String) {
        retrieveLocationFromZone(
                query, searchZone, searchZoneRect, searchCountry, searchLanguage, searchPoiTypes
        )
        if (isSearchZoneWithDefaultLocale) {
            retrieveLocationFromDefaultZone(query)
        } else {
            geocoderPresenter?.getFromLocationName(query)
        }
    }

    private fun retrieveLocationFromDefaultZone(query: String) {
        geocoderPresenter?.let {
            if (DefaultCountryLocaleRect.defaultLowerLeft != null) {
                it.getFromLocationName(
                    query = query,
                    lowerLeft = DefaultCountryLocaleRect.defaultLowerLeft!!,
                    upperRight = DefaultCountryLocaleRect.defaultUpperRight!!
                )
            } else {
                it.getFromLocationName(query)
            }
        }
    }

    private fun retrieveLocationFromZone(
            query: String,
            zoneKey: String?,
            zoneRect: SearchZoneRect? = null,
            countryCode: String? = null,
            language: String? = null,
            types: String? = null
    ) {
        val presenter = geocoderPresenter ?: return
        val locale = Locale(zoneKey ?: "")
        val lowerLeft = zoneRect?.lowerLeft ?: DefaultCountryLocaleRect
                .getLowerLeftFromZone(locale)
        val upperRight = zoneRect?.upperRight ?: DefaultCountryLocaleRect
                .getUpperRightFromZone(locale)
        if (lowerLeft != null && upperRight != null) {
            presenter.getFromLocationName(
                    query = query,
                    lowerLeft = lowerLeft,
                    upperRight = upperRight,
                    countryCode = countryCode,
                    language = language,
                    types = types
            )
        } else {
            presenter.getFromLocationName(query, countryCode, language, types)
        }
    }

    private fun returnCurrentPosition() {
        when {
            currentLekuPoi != null -> {
                currentLekuPoi?.let {
                    val returnIntent = Intent()
                    returnIntent.putExtra(LATITUDE, it.location.latitude)
                    returnIntent.putExtra(LONGITUDE, it.location.longitude)
                    if (street != null && city != null) {
                        returnIntent.putExtra(LOCATION_ADDRESS, locationAddress)
                    }
                    returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
                    returnIntent.putExtra(LEKU_POI, it)
                    setResult(Activity.RESULT_OK, returnIntent)
                    track(TrackEvents.RESULT_OK)
                }
            }
            currentLocation != null -> {
                val returnIntent = Intent()
                currentLocation?.let {
                    returnIntent.putExtra(LATITUDE, it.latitude)
                    returnIntent.putExtra(LONGITUDE, it.longitude)
                }
                if (street != null && city != null) {
                    returnIntent.putExtra(LOCATION_ADDRESS, locationAddress)
                }
                zipCode?.let {
                    returnIntent.putExtra(ZIPCODE, it.text)
                }
                returnIntent.putExtra(ADDRESS, selectedAddress)
                if (isGoogleTimeZoneEnabled && ::timeZone.isInitialized) {
                    returnIntent.putExtra(TIME_ZONE_ID, timeZone.id)
                    returnIntent.putExtra(TIME_ZONE_DISPLAY_NAME, timeZone.displayName)
                }
                returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
                setResult(Activity.RESULT_OK, returnIntent)
                track(TrackEvents.RESULT_OK)
            }
            else -> {
                setResult(Activity.RESULT_CANCELED)
                track(TrackEvents.CANCEL)
            }
        }
        finish()
    }

    private fun updateLocationNameList(addresses: List<Address>) {
        locationNameList.clear()
        for (address in addresses) {
            if (address.featureName == null) {
                locationNameList.add(getString(R.string.leku_unknown_location))
            } else {
                locationNameList.add(getFullAddressString(address))
            }
        }
    }

    private fun getFullAddressString(address: Address): String {
        var fullAddress = ""
        address.featureName?.let {
            fullAddress += it
        }
        if (address.subLocality != null && address.subLocality.isNotEmpty()) {
            fullAddress += ", " + address.subLocality
        }
        if (address.locality != null && address.locality.isNotEmpty()) {
            fullAddress += ", " + address.locality
        }
        if (address.countryName != null && address.countryName.isNotEmpty()) {
            fullAddress += ", " + address.countryName
        }
        return fullAddress
    }

    private fun setMapStyle() {
        map?.let { map ->
            mapStyle?.let { style ->
                val loadStyle = MapStyleOptions.loadRawResourceStyle(this, style)
                map.setMapStyle(loadStyle)
            }
        }
    }

    private fun setDefaultMapSettings() {
        map?.let {
            it.mapType = MAP_TYPE_NORMAL
            it.setOnMapLongClickListener(this)
            it.setOnMapClickListener(this)
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isMyLocationButtonEnabled = true
            it.uiSettings.isMapToolbarEnabled = false
        }
    }

    private fun setUpDefaultMapLocation() {
        if (currentLocation != null) {
            setCurrentPositionLocation()
        } else {
            searchView = findViewById(R.id.leku_search)
            retrieveLocationFrom(Locale.getDefault().displayCountry)
            hasWiderZoom = true
        }
    }

    private fun setCurrentPositionLocation() {
        currentLocation?.let {
            setNewMapMarker(LatLng(it.latitude, it.longitude))
            geocoderPresenter?.getInfoFromLocation(LatLng(it.latitude, it.longitude))
        }
    }

    private fun setPois() {
        poisList?.let { pois ->
            if (pois.isNotEmpty()) {
                lekuPoisMarkersMap = HashMap()
                for (lekuPoi in pois) {
                    val location = lekuPoi.location
                    val marker = addPoiMarker(LatLng(location.latitude, location.longitude),
                            lekuPoi.title, lekuPoi.address)
                    lekuPoisMarkersMap?.let {
                        it[marker.id] = lekuPoi
                    }
                }

                map?.setOnMarkerClickListener { marker ->
                    lekuPoisMarkersMap?.let { poisMarkersMap ->
                        val lekuPoi = poisMarkersMap[marker.id]
                        lekuPoi?.let {
                            setLocationInfo(it)
                            centerToPoi(it)
                            track(TrackEvents.SIMPLE_ON_LOCALIZE_BY_LEKU_POI)
                        }
                    }
                    true
                }
            }
        }
    }

    private fun centerToPoi(lekuPoi: LekuPoi) {
        map?.let {
            val location = lekuPoi.location
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude,
                            location.longitude)).zoom(defaultZoom.toFloat()).build()
            hasWiderZoom = false
            it.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    @Synchronized
    private fun buildHuaweiApiClient() {
        val huaweiApiClientBuilder = HuaweiApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)

        huaweiApiClient = huaweiApiClientBuilder.build()
        huaweiApiClient?.connect(this)
    }

    private fun addMarker(latLng: LatLng): Marker {
        return map!!.addMarker(MarkerOptions().position(latLng).draggable(true))
    }

    private fun addPoiMarker(latLng: LatLng, title: String, address: String): Marker {
        return map!!.addMarker(MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title)
                .snippet(address))
    }

    private fun setNewLocation(address: Address) {
        this.selectedAddress = address
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.leku_network_resource))
        }
        currentLocation?.latitude = address.latitude
        currentLocation?.longitude = address.longitude
        setNewMapMarker(LatLng(address.latitude, address.longitude))
        setLocationInfo(address)
        searchView?.setText("")
    }

    private fun fillLocationList(addresses: List<Address>) {
        locationList.clear()
        locationList.addAll(addresses)
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        view?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun removeUnnamedRoad(str: String): String {
        return str.replace(UNNAMED_ROAD_WITH_COMMA, "")
                .replace(UNNAMED_ROAD_WITH_HYPHEN, "")
    }

    class Builder {
        private var locationLatitude: Double? = null
        private var locationLongitude: Double? = null
        private var searchCountry: String? = null
        private var searchLanguage: String? = null
        private var searchPoiTypes: String? = null
        private var searchZoneLocale: String? = null
        private var searchZoneRect: SearchZoneRect? = null
        private var searchZoneDefaultLocale = false
        private var layoutsToHide = ""
        private var enableSatelliteView = true
        private var shouldReturnOkOnBackPressed = false
        private var lekuPois: List<LekuPoi>? = null
        private var geolocApiKey: String? = null
        private var huaweiSitesApiKey: String? = null
        private var googleTimeZoneEnabled = false
        private var voiceSearchEnabled = true
        private var mapStyle: Int? = null
        private var unnamedRoadVisible = true
        private var isLegacyLayoutEnabled = false

        fun withLocation(latitude: Double, longitude: Double): Builder {
            this.locationLatitude = latitude
            this.locationLongitude = longitude
            return this
        }

        fun withLocation(latLng: LatLng?): Builder {
            if (latLng != null) {
                this.locationLatitude = latLng.latitude
                this.locationLongitude = latLng.longitude
            }
            return this
        }

        fun withSearchCountry(country: String): Builder {
            this.searchCountry = country
            return this
        }

        fun withSearchLanguage(language: String): Builder {
            this.searchLanguage = language
            return this
        }

        fun withPoiTypes(types: String): Builder {
            this.searchPoiTypes = types
            return this
        }

        fun withSearchZone(localeZone: String): Builder {
            this.searchZoneLocale = localeZone
            return this
        }

        fun withSearchZone(zoneRect: SearchZoneRect): Builder {
            this.searchZoneRect = zoneRect
            return this
        }

        fun withDefaultLocaleSearchZone(): Builder {
            this.searchZoneDefaultLocale = true
            return this
        }

        fun withSatelliteViewHidden(): Builder {
            this.enableSatelliteView = false
            return this
        }

        fun shouldReturnOkOnBackPressed(): Builder {
            this.shouldReturnOkOnBackPressed = true
            return this
        }

        fun withStreetHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_STREET)
            return this
        }

        fun withCityHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_CITY)
            return this
        }

        fun withZipCodeHidden(): Builder {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_ZIPCODE)
            return this
        }

        fun withPois(pois: List<LekuPoi>): Builder {
            this.lekuPois = pois
            return this
        }

        fun withGeolocApiKey(apiKey: String): Builder {
            this.geolocApiKey = apiKey
            return this
        }

        fun withGooglePlacesApiKey(apiKey: String): Builder {
            this.huaweiSitesApiKey = apiKey
            return this
        }

        fun withHuaweiSitesApiKey(apiKey: String): Builder {
            this.huaweiSitesApiKey = apiKey
            return this
        }

        fun withGoogleTimeZoneEnabled(): Builder {
            this.googleTimeZoneEnabled = true
            return this
        }

        fun withVoiceSearchHidden(): Builder {
            this.voiceSearchEnabled = false
            return this
        }

        fun withUnnamedRoadHidden(): Builder {
            this.unnamedRoadVisible = false
            return this
        }

        fun withMapStyle(@RawRes mapStyle: Int): Builder {
            this.mapStyle = mapStyle
            return this
        }

        fun withLegacyLayout(): Builder {
            this.isLegacyLayoutEnabled = true
            return this
        }

        fun build(context: Context): Intent {
            val intent = Intent(context, LocationPickerActivity::class.java)

            locationLatitude?.let {
                intent.putExtra(LATITUDE, it)
            }
            locationLongitude?.let {
                intent.putExtra(LONGITUDE, it)
            }
            searchCountry?.let {
                intent.putExtra(SEARCH_COUNTRY, it)
            }
            searchLanguage?.let {
                intent.putExtra(SEARCH_LANGUAGE, it)
            }
            searchPoiTypes?.let {
                intent.putExtra(SEARCH_POI_TYPES, it)
            }
            searchZoneLocale?.let {
                intent.putExtra(SEARCH_ZONE, it)
            }
            searchZoneRect?.let {
                intent.putExtra(SEARCH_ZONE_RECT, searchZoneRect)
            }
            intent.putExtra(SEARCH_ZONE_DEFAULT_LOCALE, searchZoneDefaultLocale)
            if (layoutsToHide.isNotEmpty()) {
                intent.putExtra(LAYOUTS_TO_HIDE, layoutsToHide)
            }
            intent.putExtra(BACK_PRESSED_RETURN_OK, shouldReturnOkOnBackPressed)
            intent.putExtra(ENABLE_SATELLITE_VIEW, enableSatelliteView)
            lekuPois?.let {
                if (it.isNotEmpty()) {
                    intent.putExtra(POIS_LIST, ArrayList(it))
                }
            }
            geolocApiKey?.let {
                intent.putExtra(GEOLOC_API_KEY, geolocApiKey)
            }
            huaweiSitesApiKey?.let {
                intent.putExtra(PLACES_API_KEY, huaweiSitesApiKey)
            }
            mapStyle?.let { style -> intent.putExtra(MAP_STYLE, style) }
            intent.putExtra(ENABLE_GOOGLE_TIME_ZONE, googleTimeZoneEnabled)
            intent.putExtra(ENABLE_VOICE_SEARCH, voiceSearchEnabled)
            intent.putExtra(UNNAMED_ROAD_VISIBILITY, unnamedRoadVisible)
            intent.putExtra(WITH_LEGACY_LAYOUT, isLegacyLayoutEnabled)

            return intent
        }
    }
}

interface LocationCallbackX {
    fun onLocationChanged(location: Location)
}
