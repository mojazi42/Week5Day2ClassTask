package com.example.currnetlocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    FullscreenImage(R.drawable.ic_launcher_foreground)
                    BottomCardWithMap()
                    LocationCardWithRefresh()
                }
            }
        }
    }
}

@Composable
fun BottomCardWithMap() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val offsetY = with(LocalDensity.current) { (screenHeight / 2.8f).toPx().toInt() }
    val context = LocalContext.current

    var location by remember { mutableStateOf<Location?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                location = it
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0),
            15f
        )
    }

    val mapStyleOptions = remember {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
        } catch (e: Exception) {
            null
        }
    }

    val mapProperties = MapProperties(mapStyleOptions = mapStyleOptions)

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(x = 0, y = offsetY) }
                .fillMaxSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (location != null) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties
                ) {
                    Marker(
                        state = MarkerState(position = LatLng(location!!.latitude, location!!.longitude)),
                        title = "You are here"
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading map...", fontSize = 16.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun LocationCardWithRefresh() {
    val context = LocalContext.current
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLatLng(context, fusedLocationClient) { lat, lon, error ->
                latitude = lat
                longitude = lon
                errorMessage = error
                isLoading = false
            }
        } else {
            errorMessage = "Location permission denied"
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            PermissionChecker.PERMISSION_GRANTED -> {
                fetchLatLng(context, fusedLocationClient) { lat, lon, error ->
                    latitude = lat
                    longitude = lon
                    errorMessage = error
                    isLoading = false
                }
            }
            else -> {
                isLoading = true
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Text("Fetching location...", color = Color.Gray)
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = Color.Red)
                } else {
                    Text("Latitude: $latitude", fontSize = 18.sp, color = Color.Black)
                    Text("Longitude: $longitude", fontSize = 18.sp, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    isLoading = true
                    fetchLatLng(context, fusedLocationClient) { lat, lon, error ->
                        latitude = lat
                        longitude = lon
                        errorMessage = error
                        isLoading = false
                    }
                }) {
                    Text("Refresh")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLatLng(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (Double?, Double?, String?) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            location?.let {
                onResult(it.latitude, it.longitude, null)
            } ?: onResult(null, null, "Location not found")
        }
        .addOnFailureListener {
            onResult(null, null, "Error: ${it.message}")
        }
}

@Composable
fun FullscreenImage(imageRes: Int) {
    val systemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = false

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}