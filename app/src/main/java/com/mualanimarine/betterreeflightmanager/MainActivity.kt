package com.mualanimarine.betterreeflightmanager

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.ImageDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.util.TouchNetUtil
import com.google.zxing.BinaryBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPointCallback
import com.google.zxing.common.HybridBinarizer
import com.mualanimarine.betterreeflightmanager.device.defaultHandValues
import com.mualanimarine.betterreeflightmanager.device.defaultSchedule
import com.mualanimarine.betterreeflightmanager.device.parseDeviceSnapshot
import com.mualanimarine.betterreeflightmanager.model.TimeLuminance
import com.mualanimarine.betterreeflightmanager.ui.theme.BetterReefLightManagerTheme
import com.mualanimarine.betterreeflightmanager.util.CommandUtil
import com.mualanimarine.betterreeflightmanager.util.HexStringUtil
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BetterReefLightManagerTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}

private enum class AppRoute {
    Home, Network, Control, Airkiss, Esptouch
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppRoute.Home.name) {
        composable(AppRoute.Home.name) {
            HomeScreen(
                onOpenNetwork = { navController.navigate(AppRoute.Network.name) },
                onOpenControl = { navController.navigate(AppRoute.Control.name) }
            )
        }
        composable(AppRoute.Network.name) {
            NetworkScreen(
                onBack = { navController.popBackStack() },
                onOpenControl = { navController.navigate(AppRoute.Control.name) },
                onOpenAirkiss = { navController.navigate(AppRoute.Airkiss.name) },
                onOpenEsptouch = { navController.navigate(AppRoute.Esptouch.name) }
            )
        }
        composable(AppRoute.Control.name) {
            ControlScreen(
                onBack = { navController.popBackStack() },
                onOpenDeviceList = { navController.navigate(AppRoute.Network.name) }
            )
        }
        composable(AppRoute.Airkiss.name) {
            AirkissScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoute.Esptouch.name) {
            EsptouchScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onOpenNetwork: () -> Unit,
    onOpenControl: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BetterReefLightManager", fontWeight = FontWeight.SemiBold) },
                colors = transparentTopBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LandingHeroCard(
                title = "K7 灯光管理",
                body = "用于连接和管理 K7 灯具，可快速连接设备、调整灯光亮度、切换模式，并设置一天中的灯光变化曲线。",
                badges = listOf(
                    "连接设备热点",
                    "搜索局域网设备",
                    "读取设备配置",
                    "调节当前亮度",
                    "设置时间曲线",
                    "模拟日出日落",
                    "快捷导入方案",
                    "网页可视化配套"
                )
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                onClick = onOpenNetwork,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("灯具网络配置", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                onClick = onOpenControl,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("灯具控制调试", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkScreen(
    onBack: () -> Unit,
    onOpenControl: () -> Unit,
    onOpenAirkiss: () -> Unit,
    onOpenEsptouch: () -> Unit
) {
    val context = LocalContext.current
    val dataStore = BaseApplication.instance.dataStore
    var ssid by rememberSaveable { mutableStateOf(dataStore.getNetName().orEmpty()) }
    var password by rememberSaveable { mutableStateOf(dataStore.getNetPass().orEmpty()) }
    var address by rememberSaveable { mutableStateOf(dataStore.getLastAddress()) }
    var apMode by rememberSaveable { mutableStateOf(dataStore.isApModel()) }
    var status by rememberSaveable { mutableStateOf("这里先恢复本地配置，后续再接 Esptouch 配网与设备扫描。") }
    var scannedDeviceSsids by remember { mutableStateOf<List<DeviceHotspot>>(emptyList()) }
    var connectingSsid by remember { mutableStateOf<String?>(null) }
    var pendingConnectSsid by remember { mutableStateOf<String?>(null) }
    var lanInfo by remember { mutableStateOf<LanNetworkInfo?>(null) }
    var lanDevices by remember { mutableStateOf<List<LanDiscoveredDevice>>(emptyList()) }
    var scanningLan by remember { mutableStateOf(false) }

    fun refreshDeviceHotspots() {
        scannedDeviceSsids = scanMatchingSsids(context, "K7_Pro")
        status = if (scannedDeviceSsids.isEmpty()) {
            "未扫描到 K7_Pro 设备热点，请确认设备已进入 AP 模式。"
        } else {
            "已扫描到 ${scannedDeviceSsids.size} 个 K7_Pro 设备热点。"
        }
    }

    fun refreshLanInfo() {
        lanInfo = readCurrentLanNetworkInfo(context)
        if (!apMode) {
            status = if (lanInfo == null) {
                "未读取到当前局域网信息，请确认已连接到家庭路由器。"
            } else {
                "已读取当前局域网，可直接扫描同网段设备。"
            }
        }
    }

    fun scanLanDevices() {
        val currentLanInfo = readCurrentLanNetworkInfo(context)
        lanInfo = currentLanInfo
        if (currentLanInfo == null) {
            status = "未读取到当前局域网信息，请确认已连接到家庭路由器。"
            lanDevices = emptyList()
            return
        }
        scanningLan = true
        lanDevices = emptyList()
        status = "正在扫描同网段设备，请稍等..."
        scanReachableDevices(
            subnetPrefix = currentLanInfo.subnetPrefix,
            port = Constant.DEVICE_PORT,
            onDiscovered = { device ->
                val cachedName = dataStore.getDeviceNameByIp(device.ip)
                val cachedDevice = if (cachedName.isNullOrBlank()) device else device.copy(deviceName = cachedName)
                if (lanDevices.none { it.ip == cachedDevice.ip }) {
                    lanDevices = (lanDevices + cachedDevice).sortedBy { it.ip.ipToSortableValue() }
                }
            },
            onFinished = { devices ->
                scanningLan = false
                lanDevices = devices.map { device ->
                    val cachedName = dataStore.getDeviceNameByIp(device.ip)
                    if (cachedName.isNullOrBlank()) device else device.copy(deviceName = cachedName)
                }.sortedBy { it.ip.ipToSortableValue() }
                status = if (devices.isEmpty()) {
                    "当前网段未发现可连接设备，请确认手机与设备在同一路由器下。"
                } else {
                    "已发现 ${devices.size} 个局域网设备，可直接带入设备 IP。"
                }
            }
        )
    }

    fun performDeviceHotspotConnection(targetSsid: String) {
        connectingSsid = targetSsid
        pendingConnectSsid = null
        password = DEFAULT_DEVICE_AP_PASSWORD
        ssid = targetSsid
        apMode = true
        dataStore.setApModel(true)
        connectToK7ProHotspot(
            context = context,
            ssid = targetSsid,
            password = DEFAULT_DEVICE_AP_PASSWORD,
            onStatus = { message -> status = message },
            onConnected = { gatewayIp ->
                connectingSsid = null
                gatewayIp?.let {
                    address = it
                    dataStore.setLastAddress(it)
                }
                dataStore.setNetNamePass(targetSsid, DEFAULT_DEVICE_AP_PASSWORD)
                status = "已连接设备热点 $targetSsid"
            },
            onError = { message ->
                connectingSsid = null
                status = message
            }
        )
    }

    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (requiredWifiPermissions().all { permission -> result[permission] == true }) {
            pendingConnectSsid?.let { targetSsid ->
                performDeviceHotspotConnection(targetSsid)
            } ?: refreshDeviceHotspots()
        } else {
            pendingConnectSsid = null
            status = "未授予 Wi-Fi 权限：${missingWifiPermissions(context).joinToString()}"
        }
    }

    fun connectToDeviceHotspot(targetSsid: String) {
        if (!hasWifiConnectPermission(context)) {
            pendingConnectSsid = targetSsid
            status = "缺少热点权限：${missingWifiPermissions(context).joinToString()}"
            wifiPermissionLauncher.launch(requiredWifiPermissions())
            return
        }
        performDeviceHotspotConnection(targetSsid)
    }

    fun applyRouterIpIfNeeded() {
        if (!apMode) return
        val routerIp = readCurrentGatewayIp(context)
        if (!routerIp.isNullOrBlank()) {
            address = routerIp
            dataStore.setLastAddress(routerIp)
            status = "AP 模式已自动带入当前路由器 IP。"
        } else {
            status = "未读取到当前路由器 IP，请确认已连接到设备热点或路由器。"
        }
    }

    fun applyLanDeviceIpAndOpenControl(targetDeviceIp: String) {
        apMode = false
        dataStore.setApModel(false)
        address = targetDeviceIp
        dataStore.setLastAddress(targetDeviceIp)
        status = "已带入局域网设备 IP：$targetDeviceIp"
        onOpenControl()
    }

    LaunchedEffect(apMode) {
        if (apMode) {
            password = DEFAULT_DEVICE_AP_PASSWORD
            applyRouterIpIfNeeded()
        } else {
            refreshLanInfo()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("网络配置", fontWeight = FontWeight.SemiBold) },
                colors = transparentTopBarColors(),
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroCard(title = "说明", body = status)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("最近设备地址或网段") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("使用 AP 模式", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = apMode,
                    onCheckedChange = {
                        apMode = it
                        dataStore.setApModel(it)
                        if (it) {
                            applyRouterIpIfNeeded()
                        } else {
                            status = "已切换到局域网模式。"
                        }
                    }
                )
            }
            if (apMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("设备热点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        CompactOutlinedButtonRow(
                            primaryLabel = "扫描设备热点",
                            onPrimaryClick = {
                                if (hasWifiScanPermission(context)) {
                                    refreshDeviceHotspots()
                                } else {
                                    wifiPermissionLauncher.launch(requiredWifiPermissions())
                                }
                            },
                            secondaryLabel = "连接首个设备",
                            onSecondaryClick = {
                                if (hasWifiScanPermission(context)) {
                                    if (scannedDeviceSsids.isEmpty()) {
                                        refreshDeviceHotspots()
                                    }
                                    scannedDeviceSsids.firstOrNull()?.let { connectToDeviceHotspot(it.ssid) }
                                        ?: run { status = "当前没有可连接的 K7_Pro 设备热点。" }
                                } else {
                                    wifiPermissionLauncher.launch(requiredWifiPermissions())
                                }
                            }
                        )
                        scannedDeviceSsids.forEach { hotspot ->
                            CompactOutlinedButtonRow(
                                primaryLabel = "${hotspot.ssid} ${hotspot.signalLabel}",
                                onPrimaryClick = {
                                    connectToDeviceHotspot(hotspot.ssid)
                                },
                                secondaryLabel = if (connectingSsid == hotspot.ssid) "连接中..." else "快速切换",
                                onSecondaryClick = {
                                    connectToDeviceHotspot(hotspot.ssid)
                                }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("局域网发现", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = lanInfo?.let {
                                "当前 Wi-Fi: ${it.ssid}\n本机 IP: ${it.localIp}\n网关 IP: ${it.gatewayIp}\n扫描网段: ${it.subnetPrefix}.0/24"
                            } ?: "尚未读取到当前局域网信息",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "刷新局域网信息",
                            onPrimaryClick = { refreshLanInfo() },
                            secondaryLabel = if (scanningLan) "正在扫描设备" else "扫描同网段设备",
                            onSecondaryClick = { scanLanDevices() }
                        )
                        lanDevices.forEach { device ->
                            CompactOutlinedButtonRow(
                                primaryLabel = device.displayLabel,
                                onPrimaryClick = {
                                    applyLanDeviceIpAndOpenControl(device.ip)
                                },
                                secondaryLabel = "带入设备 IP",
                                onSecondaryClick = {
                                    applyLanDeviceIpAndOpenControl(device.ip)
                                }
                            )
                        }
                    }
                }
            }
            if (!apMode) {
                CompactOutlinedButtonRow(
                    primaryLabel = "打开 Airkiss 配网",
                    onPrimaryClick = onOpenAirkiss,
                    secondaryLabel = "打开 Esptouch 配网",
                    onSecondaryClick = onOpenEsptouch
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (ssid.isBlank()) {
                        status = "SSID 不能为空。"
                    } else {
                        dataStore.setNetNamePass(ssid, password)
                        dataStore.setApModel(apMode)
                        dataStore.setLastAddress(address)
                        status = "网络配置已保存。"
                    }
                }
            ) {
                Text("保存配置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirkissScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentSsid by rememberSaveable { mutableStateOf(readCurrentWifiSsid(context).orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("连接家庭路由器后，输入 Wi-Fi 密码即可发送 Airkiss 配网信息。") }
    var receivedHex by rememberSaveable { mutableStateOf("") }
    var currentSession by remember { mutableStateOf<AirkissSession?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            currentSession?.cancel()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Airkiss 配网", fontWeight = FontWeight.SemiBold) },
                colors = transparentTopBarColors(),
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroCard(
                title = "当前流程",
                body = "1. 保持手机连接家庭路由器\n2. 输入当前路由器密码\n3. 发送 Airkiss 广播\n4. 等待设备通过 UDP 回包"
            )
            HeroCard(title = "状态", body = status)
            OutlinedTextField(
                value = currentSsid,
                onValueChange = { currentSsid = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("当前 Wi-Fi SSID") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Wi-Fi 密码") }
            )
            CompactOutlinedButtonRow(
                primaryLabel = "读取当前 Wi-Fi",
                onPrimaryClick = {
                    currentSsid = readCurrentWifiSsid(context).orEmpty()
                    status = if (currentSsid.isBlank()) {
                        "未读取到当前 Wi-Fi，请确认手机已连接家庭路由器。"
                    } else {
                        "已读取当前 Wi-Fi：$currentSsid"
                    }
                },
                secondaryLabel = if (running) "停止 Airkiss" else "开始 Airkiss",
                onSecondaryClick = {
                    if (running) {
                        currentSession?.cancel()
                        currentSession = null
                        running = false
                        status = "已停止 Airkiss 配网。"
                    } else if (currentSsid.isBlank() || password.isBlank()) {
                        status = "请先确认当前 Wi-Fi 名称，并输入路由器密码。"
                    } else {
                        receivedHex = ""
                        running = true
                        currentSession?.cancel()
                        currentSession = startAirkissProvision(
                            ssid = currentSsid,
                            password = password,
                            onStatus = { status = it },
                            onReceived = { hex ->
                                receivedHex = hex
                                running = false
                                status = "已收到 Airkiss 回包，请继续做局域网设备发现。"
                            },
                            onFinished = {
                                running = false
                            }
                        )
                    }
                }
            )
            if (receivedHex.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("最近回包", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(receivedHex, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EsptouchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSsid by rememberSaveable { mutableStateOf(readCurrentWifiSsid(context).orEmpty()) }
    var currentBssid by rememberSaveable { mutableStateOf(readCurrentWifiBssid(context).orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var useBroadcast by rememberSaveable { mutableStateOf(true) }
    var totalTimeoutSecondsText by rememberSaveable { mutableStateOf("75") }
    var status by rememberSaveable { mutableStateOf("连接家庭路由器后，输入 Wi-Fi 密码即可发送 Esptouch 配网信息。") }
    var running by remember { mutableStateOf(false) }
    var resultLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var esptouchJob by remember { mutableStateOf<Job?>(null) }
    var currentEsptouchTask by remember { mutableStateOf<EsptouchTask?>(null) }
    var startTimestamp by rememberSaveable { mutableStateOf<String?>(null) }
    var diagnostics by remember { mutableStateOf(readEsptouchDiagnostics(context)) }

    fun refreshDiagnostics() {
        diagnostics = readEsptouchDiagnostics(context)
        currentSsid = diagnostics.ssid.orEmpty()
        currentBssid = diagnostics.bssid.orEmpty()
    }

    DisposableEffect(Unit) {
        onDispose {
            esptouchJob?.cancel()
            currentEsptouchTask?.interrupt()
        }
    }

    LaunchedEffect(Unit) {
        refreshDiagnostics()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Esptouch 配网", fontWeight = FontWeight.SemiBold) },
                colors = transparentTopBarColors(),
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeroCard(
                title = "当前流程",
                body = "1. 保持手机连接 2.4G 路由器\n2. 输入路由器密码\n3. 发送 Esptouch 配网\n4. 等待设备返回局域网 IP"
            )
            HeroCard(title = "状态", body = status)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("现场诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("当前 Wi-Fi: ${diagnostics.ssid ?: "未读取"}")
                    Text("当前 BSSID: ${diagnostics.bssid ?: "未读取"}")
                    Text("当前频段: ${diagnostics.frequencyLabel}")
                    Text("本机 IP: ${diagnostics.localIp ?: "未读取"}")
                    Text("网关 IP: ${diagnostics.gatewayIp ?: "未读取"}")
                    Text("定位权限: ${if (diagnostics.hasLocationPermission) "已授权" else "未授权"}")
                    Text("定位服务: ${if (diagnostics.locationServiceEnabled) "已开启" else "未开启"}")
                    startTimestamp?.let { Text("最近开始时间: $it") }
                    if (diagnostics.warnings.isNotEmpty()) {
                        Text(
                            diagnostics.warnings.joinToString(separator = "\n") { "• $it" },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            OutlinedTextField(
                value = currentSsid,
                onValueChange = { currentSsid = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("当前 Wi-Fi SSID") }
            )
            OutlinedTextField(
                value = currentBssid,
                onValueChange = { currentBssid = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("当前 Wi-Fi BSSID") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Wi-Fi 密码") }
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("兼容设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (useBroadcast) "发送方式：广播模式" else "发送方式：组播模式")
                        Switch(
                            checked = useBroadcast,
                            onCheckedChange = { useBroadcast = it }
                        )
                    }
                    OutlinedTextField(
                        value = totalTimeoutSecondsText,
                        onValueChange = { input ->
                            totalTimeoutSecondsText = input.filter { it.isDigit() }.take(3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("总等待秒数（建议 60-90）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            CompactOutlinedButtonRow(
                primaryLabel = "读取当前 Wi-Fi",
                onPrimaryClick = {
                    refreshDiagnostics()
                    status = if (currentSsid.isBlank() || currentBssid.isBlank()) {
                        "未读取到当前 Wi-Fi 信息，请确认手机已连接到 2.4G 路由器。"
                    } else {
                        "已读取当前 Wi-Fi：$currentSsid，${diagnostics.frequencyLabel}"
                    }
                },
                secondaryLabel = if (running) "停止 Esptouch" else "开始 Esptouch",
                onSecondaryClick = {
                    if (running) {
                        esptouchJob?.cancel()
                        currentEsptouchTask?.interrupt()
                        currentEsptouchTask = null
                        esptouchJob = null
                        running = false
                        status = "已停止 Esptouch 配网。"
                    } else {
                        refreshDiagnostics()
                        val startBlockReason = diagnostics.startBlockReason(
                            ssid = currentSsid,
                            bssid = currentBssid
                        )
                        if (startBlockReason != null) {
                            status = startBlockReason
                        } else {
                            running = true
                            resultLines = emptyList()
                            startTimestamp = nowTimeLabel()
                            val timeoutMillis = (totalTimeoutSecondsText.toIntOrNull() ?: 75)
                                .coerceIn(60, 120) * 1000
                            esptouchJob?.cancel()
                            esptouchJob = scope.launch {
                                status = "正在发送 Esptouch 配网信息，请保持设备快闪并等待返回..."
                                val runResult = withTimeoutOrNull(timeoutMillis.toLong() + 5_000L) {
                                    runEsptouchProvision(
                                        context = context,
                                        ssid = currentSsid,
                                        bssid = currentBssid,
                                        password = password,
                                        useBroadcast = useBroadcast,
                                        totalTimeoutMillis = timeoutMillis,
                                        onTaskCreated = { task -> currentEsptouchTask = task }
                                    )
                                } ?: EsptouchRunResult(results = null)
                                if (runResult.results == null) {
                                    currentEsptouchTask?.interrupt()
                                }
                                running = false
                                currentEsptouchTask = null
                                refreshDiagnostics()
                                if (!runResult.errorMessage.isNullOrBlank()) {
                                    status = "Esptouch 执行失败：${runResult.errorMessage}"
                                } else if (runResult.results == null) {
                                    status = "Esptouch 在 ${timeoutMillis / 1000} 秒内未返回结果，请重试并切换广播/组播模式。"
                                } else if (runResult.results.isEmpty()) {
                                    status = "Esptouch 未发现设备，请确认设备已进入配网状态。"
                                } else {
                                    val successResults = runResult.results.filter { it.isSuc && !it.isCancelled }
                                    if (successResults.isEmpty()) {
                                        status = "Esptouch 已结束，但设备未确认配网成功。请重试或切换兼容配网方式。"
                                        return@launch
                                    }
                                    resultLines = successResults.mapNotNull { result ->
                                        result.inetAddress?.hostAddress?.let { "${it}, ${result.bssid}" }
                                    }
                                    status = "Esptouch 完成，已发现 ${resultLines.size} 个设备。"
                                }
                            }
                        }
                    }
                }
            )
            if (resultLines.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("配网结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        resultLines.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlScreen(
    onBack: () -> Unit,
    onOpenDeviceList: () -> Unit
) {
    val context = LocalContext.current
    val dataStore = BaseApplication.instance.dataStore
    var deviceIp by rememberSaveable { mutableStateOf(dataStore.getLastAddress()) }
    var selectedType by rememberSaveable { mutableStateOf(Constant.Type.TYPE_K7) }
    var selectedGroupId by rememberSaveable { mutableStateOf("preset_sps") }
    var connectionState by rememberSaveable { mutableStateOf("未连接") }
    var currentDeviceName by rememberSaveable { mutableStateOf("未知") }
    var showFixedPackets by rememberSaveable { mutableStateOf(false) }
    var showManualBrightnessEditor by rememberSaveable { mutableStateOf(false) }
    var showBatchToolsEditor by rememberSaveable { mutableStateOf(false) }
    var showDetailedCurveEditor by rememberSaveable { mutableStateOf(false) }
    var sendConfigAutoMode by rememberSaveable { mutableStateOf(false) }
    var manualValuesText by rememberSaveable { mutableStateOf(toCsv(defaultHandValues(Constant.Type.TYPE_K7))) }
    var batchStartHourText by rememberSaveable { mutableStateOf("0") }
    var batchEndHourText by rememberSaveable { mutableStateOf("23") }
    var batchValuesText by rememberSaveable { mutableStateOf("0,0,0,0,0,0") }
    var smoothStepText by rememberSaveable { mutableStateOf("50") }
    var jumpDialogMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var solarSummary by rememberSaveable { mutableStateOf("未读取当地日出日落") }
    var solarRangeSummary by rememberSaveable { mutableStateOf("") }
    var pendingSolarAction by remember { mutableStateOf<SolarAction?>(null) }
    var pendingCurveDangerAction by remember { mutableStateOf<CurveDangerAction?>(null) }
    var showImportSchemeDialog by rememberSaveable { mutableStateOf(false) }
    var showCameraScanner by rememberSaveable { mutableStateOf(false) }
    var importSchemeText by rememberSaveable { mutableStateOf("") }
    val logs = remember { mutableStateListOf<String>() }
    val curveGroups = remember { mutableStateListOf<CurveGroup>() }
    var loadedCurveType by rememberSaveable { mutableStateOf(selectedType) }
    var connectThread by remember { mutableStateOf<ConnectThread?>(null) }

    fun updateSolarTimes(onLoaded: ((SolarTimes) -> Unit)? = null) {
        solarSummary = "正在获取当地日出日落..."
        requestSolarTimes(context) { solarTimes ->
            if (solarTimes == null) {
                solarSummary = "未能获取当地日出日落"
                solarRangeSummary = ""
                logs.add("未能获取当地日出日落，请检查定位权限或位置信息")
            } else {
                solarSummary = "日出 ${solarTimes.sunriseLabel} / 日落 ${solarTimes.sunsetLabel}"
                solarRangeSummary = formatSolarRangeSummary(solarTimes)
                logs.add("已获取当地日出日落: ${solarTimes.sunriseLabel} / ${solarTimes.sunsetLabel}")
                onLoaded?.invoke(solarTimes)
            }
        }
    }

    fun performSolarAction(action: SolarAction) {
        updateSolarTimes { solarTimes ->
            when (action) {
                SolarAction.ApplyDayRange -> {
                    batchStartHourText = solarTimes.sunriseHour.toString()
                    batchEndHourText = solarTimes.sunsetHour.toString()
                    logs.add("已带入白昼时段 ${solarTimes.sunriseHour}-${solarTimes.sunsetHour}")
                }
                SolarAction.ApplyNightRange -> {
                    val values = validateValuesCsv(batchValuesText)
                    if (values != null) {
                        logs.add(values)
                    } else {
                        updateCurveGroup(curveGroups, selectedGroupId) { group ->
                            group.copy(items = group.items.map { item ->
                                val hour = item.hour.toIntOrNull()
                                if (hour != null && (hour < solarTimes.sunriseHour || hour > solarTimes.sunsetHour)) {
                                    item.copy(valuesText = batchValuesText)
                                } else {
                                    item
                                }
                            })
                        }
                        logs.add("已将亮度应用到黑夜时段 00-${solarTimes.sunriseHour} 和 ${solarTimes.sunsetHour}-23")
                    }
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            pendingSolarAction?.let { action ->
                pendingSolarAction = null
                performSolarAction(action)
            } ?: updateSolarTimes()
        } else {
            solarSummary = "未授予定位权限"
            solarRangeSummary = ""
            pendingSolarAction = null
            logs.add("未授予定位权限，无法获取当地日出日落")
        }
    }

    fun runSolarAction(action: SolarAction) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFineLocation || hasCoarseLocation) {
            performSolarAction(action)
        } else {
            pendingSolarAction = action
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun applyImportedScheme(rawText: String) {
        val imported = parseImportedScheme(rawText)
        if (imported == null) {
            logs.add("二维码方案格式无效，导入失败")
            return
        }
        val nextIndex = curveGroups.count { !it.isPreset } + 1
        val normalized = imported.copy(
            id = "custom_$nextIndex",
            isPreset = false,
            name = imported.name.ifBlank { "导入方案$nextIndex" }
        )
        curveGroups.add(normalized)
        selectedGroupId = normalized.id
        logs.add("已导入方案 ${normalized.name}")
        importSchemeText = ""
        showImportSchemeDialog = false
    }

    val importFromGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            logs.add("未选择二维码图片")
        } else {
            val qrText = decodeQrTextFromUri(context, uri)
            if (qrText.isNullOrBlank()) {
                logs.add("二维码识别失败，请换更清晰的图片再试")
            } else {
                importSchemeText = qrText
                applyImportedScheme(qrText)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraScanner = true
        } else {
            logs.add("未授予相机权限，无法扫码导入")
        }
    }

    LaunchedEffect(Unit) {
        if (dataStore.isApModel()) {
            readCurrentGatewayIp(context)?.let { gatewayIp ->
                deviceIp = gatewayIp
                dataStore.setLastAddress(gatewayIp)
            }
        }
        val storedGroups = loadCurveGroups(dataStore, selectedType)
        curveGroups.clear()
        curveGroups.addAll(storedGroups)
        selectedGroupId = dataStore.getSelectedCurveGroupId(selectedType)
            ?.takeIf { savedId -> curveGroups.any { it.id == savedId } }
            ?: curveGroups.firstOrNull()?.id
            ?: "preset_sps"
        loadedCurveType = selectedType
    }

    LaunchedEffect(selectedType) {
        if (loadedCurveType != selectedType) {
            saveCurveGroups(dataStore, loadedCurveType, curveGroups, selectedGroupId)
            val storedGroups = loadCurveGroups(dataStore, selectedType)
            curveGroups.clear()
            curveGroups.addAll(storedGroups)
            selectedGroupId = dataStore.getSelectedCurveGroupId(selectedType)
                ?.takeIf { savedId -> curveGroups.any { it.id == savedId } }
                ?: curveGroups.firstOrNull()?.id
                ?: "preset_sps"
            loadedCurveType = selectedType
        }
    }

    LaunchedEffect(curveGroups.toList(), selectedGroupId, loadedCurveType) {
        if (curveGroups.isNotEmpty()) {
            saveCurveGroups(dataStore, loadedCurveType, curveGroups, selectedGroupId)
        }
    }

    val socketHandler = remember {
        Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                Constant.MessageTag.CONNECT_ENABLE -> {
                    connectionState = "已连接 ${msg.obj}"
                    logs.add("连接成功: ${msg.obj}")
                    connectThread?.sendData(CommandUtil.allRead())
                    logs.add("发送: ${HexStringUtil.byteArrayToHex(CommandUtil.allRead())}")
                    true
                }
                Constant.MessageTag.CONNECT_DISABLE -> {
                    connectionState = "连接断开 ${msg.obj}"
                    logs.add("连接断开: ${msg.obj}")
                    connectThread = null
                    true
                }
                Constant.MessageTag.DEVICE_INFO -> {
                    val payload = msg.obj as ByteArray
                    logs.add("接收: ${HexStringUtil.byteArrayToHex(payload)}")
                    parseDeviceSnapshot(payload)?.let { snapshot ->
                        manualValuesText = toCsv(snapshot.handValues)
                        sendConfigAutoMode = snapshot.autoMode
                        logs.add("读取到设备模式: ${if (snapshot.autoMode) "自动" else "手动"}（标记=${payload[203].toInt() and 0xFF}）")
                        val deviceSchemeName = snapshot.deviceName
                            ?.takeIf { it.isNotBlank() }
                            ?: deviceIp.takeIf { it.isNotBlank() }
                            ?: "设备方案"
                        currentDeviceName = deviceSchemeName
                        snapshot.deviceName
                            ?.takeIf { it.isNotBlank() }
                            ?.let { dataStore.setDeviceNameByIp(deviceIp, it) }
                        val deviceItems = snapshot.schedule.map { it.toEditorItem() }
                        val deviceSchemeId = buildDeviceCurveGroupId(deviceSchemeName)
                        val existingSchemeIndex = curveGroups.indexOfFirst { it.id == deviceSchemeId }
                        if (existingSchemeIndex >= 0) {
                            curveGroups[existingSchemeIndex] = curveGroups[existingSchemeIndex].copy(
                                name = deviceSchemeName,
                                items = deviceItems
                            )
                            logs.add("已更新设备方案 $deviceSchemeName")
                        } else {
                            curveGroups.add(
                                CurveGroup(
                                    id = deviceSchemeId,
                                    name = deviceSchemeName,
                                    isPreset = false,
                                    items = deviceItems
                                )
                            )
                            selectedGroupId = deviceSchemeId
                            logs.add("已为设备 $deviceSchemeName 新建设备方案")
                        }
                        logs.add("已解析设备配置并回填到页面")
                    }
                    true
                }
                Constant.MessageTag.COMMAND_RESEND -> {
                    val payload = msg.obj as ByteArray
                    logs.add("重发: ${HexStringUtil.byteArrayToHex(payload)}")
                    connectThread?.sendData(payload)
                    true
                }
                else -> false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            connectThread?.closeQuietly()
        }
    }

    jumpDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { jumpDialogMessage = null },
            confirmButton = {
                TextButton(onClick = { jumpDialogMessage = null }) {
                    Text("知道了")
                }
            },
            title = { Text("检测到亮度跳变过大") },
            text = { Text(message) }
        )
    }

    pendingCurveDangerAction?.let { action ->
        val currentGroupName = currentGroup(curveGroups, selectedGroupId)?.name ?: "当前方案"
        val (title, message, confirmLabel) = when (action) {
            CurveDangerAction.ClearManualValues -> Triple(
                "确认清零手动亮度",
                "这会把当前 6 路手动亮度全部改成 0，不会影响时间曲线。",
                "确认清零"
            )
            CurveDangerAction.ClearAllDayCurve -> Triple(
                "确认全天清零",
                "这会把方案“$currentGroupName”的 24 个时间点全部改成 0。这个操作容易误触，建议确认后再执行。",
                "确认清零"
            )
            CurveDangerAction.ResetPresetGroups -> Triple(
                "确认恢复预置方案",
                "这会按当前设备类型重建 SPS、LPS、SL 三套预置方案，并覆盖现有预置方案内容。",
                "确认重置"
            )
            CurveDangerAction.DeleteCurrentScheme -> Triple(
                "确认删除当前方案",
                "这会删除方案“$currentGroupName”。删除后无法直接恢复，请确认后再继续。",
                "确认删除"
            )
        }
        AlertDialog(
            onDismissRequest = { pendingCurveDangerAction = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            CurveDangerAction.ClearManualValues -> {
                                manualValuesText = "0,0,0,0,0,0"
                                logs.add("已将六路亮度归零")
                            }
                            CurveDangerAction.ClearAllDayCurve -> {
                                batchValuesText = "0,0,0,0,0,0"
                                updateCurveGroup(curveGroups, selectedGroupId) { group ->
                                    group.copy(items = group.items.map { item -> item.copy(valuesText = "0,0,0,0,0,0") })
                                }
                                logs.add("已将当前方案全天亮度归零")
                            }
                            CurveDangerAction.ResetPresetGroups -> {
                                val presetGroups = buildDefaultCurveGroups(selectedType)
                                curveGroups.removeAll { it.isPreset }
                                curveGroups.addAll(0, presetGroups)
                                if (selectedGroupId !in curveGroups.map { it.id }) {
                                    selectedGroupId = "preset_sps"
                                }
                                logs.add("已按当前设备类型恢复预置方案")
                            }
                            CurveDangerAction.DeleteCurrentScheme -> {
                                val current = currentGroup(curveGroups, selectedGroupId)
                                if (current == null) {
                                    logs.add("当前没有可删除的方案")
                                } else if (current.isPreset) {
                                    logs.add("预置方案不能删除")
                                } else {
                                    val removeIndex = curveGroups.indexOfFirst { it.id == current.id }
                                    curveGroups.removeAll { it.id == current.id }
                                    val nextSelection = curveGroups.getOrNull((removeIndex - 1).coerceAtLeast(0))
                                        ?: curveGroups.firstOrNull()
                                    selectedGroupId = nextSelection?.id ?: "preset_sps"
                                    logs.add("已删除方案 ${current.name}")
                                }
                            }
                        }
                        pendingCurveDangerAction = null
                    }
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurveDangerAction = null }) {
                    Text("取消")
                }
            },
            title = { Text(title) },
            text = { Text(message) }
        )
    }

    if (showImportSchemeDialog) {
        AlertDialog(
            onDismissRequest = { showImportSchemeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyImportedScheme(importSchemeText)
                    }
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportSchemeDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("导入二维码方案") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("可直接选择二维码图片、打开相机扫码，或粘贴扫码得到的原始内容。导入后会新增为一套自定义方案，不会覆盖当前预置方案。")
                    CompactOutlinedButtonRow(
                        primaryLabel = "从相册导入",
                        onPrimaryClick = {
                            importFromGalleryLauncher.launch("image/*")
                        },
                        secondaryLabel = "打开相机扫码",
                        onSecondaryClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) {
                                showCameraScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    OutlinedTextField(
                        value = importSchemeText,
                        onValueChange = { importSchemeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("二维码内容") },
                        minLines = 5
                    )
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                importSchemeText = clipText
                            } else {
                                logs.add("剪贴板里没有可导入的方案内容")
                            }
                        }
                    ) {
                        Text("从剪贴板粘贴")
                    }
                }
            }
        )
    }

    if (showCameraScanner) {
        QrCameraScannerDialog(
            onDismiss = { showCameraScanner = false },
            onScanned = { qrText ->
                importSchemeText = qrText
                applyImportedScheme(qrText)
                showCameraScanner = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "控制调试",
                        modifier = Modifier.pointerInput(showFixedPackets) {
                            detectTapGestures(
                                onDoubleTap = {
                                    showFixedPackets = !showFixedPackets
                                    logs.add(if (showFixedPackets) "已显示最近 5 条报文" else "已隐藏最近 5 条报文")
                                }
                            )
                        }
                    )
                },
                colors = transparentTopBarColors(),
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = {
                    TextButton(onClick = onOpenDeviceList) {
                        Text("设备列表")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showFixedPackets) {
                FixedPacketPanel(logs = logs)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ControlSummaryCard(
                    connectionState = connectionState,
                    deviceName = currentDeviceName,
                    deviceType = describeType(selectedType),
                    currentGroup = currentGroup(curveGroups, selectedGroupId)?.name ?: "未选择",
                    deviceIp = deviceIp,
                    onDeviceIpChange = { deviceIp = it }
                )
                ControlPanelCard(
                    title = "设备命令控制区",
                    subtitle = "连接、读取、同步和模式类命令。"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactOutlinedButtonRow(
                            primaryLabel = "断开连接",
                            onPrimaryClick = {
                                connectThread?.closeQuietly()
                                connectThread = null
                                connectionState = "未连接"
                                currentDeviceName = "未知"
                                logs.add("手动断开连接")
                            },
                            secondaryLabel = "开始连接",
                            onSecondaryClick = {
                                if (deviceIp.isBlank()) {
                                    logs.add("请输入设备 IP")
                                } else {
                                    val hadConnection = connectThread != null
                                    connectThread?.closeQuietly()
                                    connectThread = null
                                    dataStore.setLastAddress(deviceIp)
                                    connectionState = "连接中"
                                    currentDeviceName = "读取中"
                                    logs.add(if (hadConnection) "正在重连 $deviceIp:${Constant.DEVICE_PORT}" else "连接 $deviceIp:${Constant.DEVICE_PORT}")
                                    socketHandler.postDelayed({
                                        connectThread = ConnectThread(deviceIp, Constant.DEVICE_PORT, socketHandler).also { it.start() }
                                    }, if (hadConnection) 300L else 0L)
                                }
                            }
                        )

                        Text("基础命令", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        CompactButtonRow(
                            primaryLabel = "读取设备配置",
                            onPrimaryClick = {
                                sendWithLog(connectThread, CommandUtil.allRead(), logs)
                            },
                            secondaryLabel = "同步设备时间",
                            onSecondaryClick = {
                                val calendar = Calendar.getInstance()
                                sendWithLog(
                                    connectThread,
                                    CommandUtil.syncTime(
                                        calendar.get(Calendar.HOUR_OF_DAY).toByte(),
                                        calendar.get(Calendar.MINUTE).toByte(),
                                        calendar.get(Calendar.SECOND).toByte()
                                    ),
                                    logs
                                )
                            }
                        )

                        Text("模式命令", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        CompactButtonRow(
                            primaryLabel = "发送预设亮度",
                            onPrimaryClick = {
                                val values = if (selectedType == Constant.Type.TYPE_K7) Constant.Hand.K7_DEFAULT else Constant.Hand.X4_DEFAULT
                                sendWithLog(connectThread, CommandUtil.handModelLuminance(values), logs)
                            },
                            secondaryLabel = "发送当前亮度",
                            onSecondaryClick = {
                                val manualValues = parseValuesCsv(manualValuesText)
                                if (manualValues == null) {
                                    logs.add("手动亮度格式错误，需要 6 个 0-100 数值")
                                } else {
                                    sendWithLog(connectThread, CommandUtil.handModelLuminance(manualValues), logs)
                                }
                            }
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "切到自动模式",
                            onPrimaryClick = { sendWithLog(connectThread, CommandUtil.changeModel(true), logs) },
                            secondaryLabel = "切到手动模式",
                            onSecondaryClick = { sendWithLog(connectThread, CommandUtil.changeModel(false), logs) }
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "开启演示模式",
                            onPrimaryClick = { sendWithLog(connectThread, CommandUtil.openDemonstration(true), logs) },
                            secondaryLabel = "关闭演示模式",
                            onSecondaryClick = { sendWithLog(connectThread, CommandUtil.openDemonstration(false), logs) }
                        )
                    }
                }

                ControlPanelCard(
                    title = "灯光方案",
                    subtitle = "独立管理当前方案、自定义方案和二维码导入。"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentGroup(curveGroups, selectedGroupId)?.let { selected ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("当前方案", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${selected.name} · ${if (selected.isPreset) "预置方案" else "自定义方案"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                "发送配置模式",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                if (sendConfigAutoMode) "当前将按自动模式发送" else "当前将按手动模式发送",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = sendConfigAutoMode,
                                            onCheckedChange = { sendConfigAutoMode = it }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = selected.name,
                                        onValueChange = { name ->
                                            updateCurveGroup(curveGroups, selectedGroupId) { it.copy(name = name) }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(if (selected.isPreset) "方案名（可改显示名）" else "方案名") }
                                    )
                                    Text(
                                        text = "当前方案包含 ${selected.items.size} 个时间点",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        ChipRow(
                            values = curveGroups.map { it.id to it.name },
                            selected = selectedGroupId,
                            onSelected = { selectedGroupId = it }
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "删除当前方案",
                            onPrimaryClick = {
                                val current = currentGroup(curveGroups, selectedGroupId)
                                if (current == null) {
                                    logs.add("当前没有可删除的方案")
                                } else if (current.isPreset) {
                                    logs.add("预置方案不能删除")
                                } else {
                                    pendingCurveDangerAction = CurveDangerAction.DeleteCurrentScheme
                                }
                            },
                            secondaryLabel = "新建自定义方案",
                            onSecondaryClick = {
                                val nextIndex = curveGroups.count { !it.isPreset } + 1
                                val source = currentGroup(curveGroups, selectedGroupId)
                                val newGroup = CurveGroup(
                                    id = "custom_$nextIndex",
                                    name = "自定义方案$nextIndex",
                                    isPreset = false,
                                    items = source?.items?.map { it.copy() } ?: defaultSchedule(selectedType, Constant.Model.MODEL_SPS).map { it.toEditorItem() }
                                )
                                curveGroups.add(newGroup)
                                selectedGroupId = newGroup.id
                                logs.add("已新增自定义方案 ${newGroup.name}")
                            }
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "复制当前方案",
                            onPrimaryClick = {
                                val source = currentGroup(curveGroups, selectedGroupId)
                                if (source == null) {
                                    logs.add("没有可复制的方案")
                                } else {
                                    val nextIndex = curveGroups.count { !it.isPreset } + 1
                                    val copyGroup = CurveGroup(
                                        id = "custom_$nextIndex",
                                        name = "${source.name} 副本",
                                        isPreset = false,
                                        items = source.items.map { it.copy() }
                                    )
                                    curveGroups.add(copyGroup)
                                    selectedGroupId = copyGroup.id
                                    logs.add("已复制当前方案为 ${copyGroup.name}")
                                }
                            },
                            secondaryLabel = "导入二维码方案",
                            onSecondaryClick = {
                                showImportSchemeDialog = true
                            }
                        )
                        CompactOutlinedButtonRow(
                            primaryLabel = "恢复预置方案",
                            onPrimaryClick = {
                                pendingCurveDangerAction = CurveDangerAction.ResetPresetGroups
                            },
                            secondaryLabel = "发送当前配置",
                            onSecondaryClick = {
                                val manualValues = parseValuesCsv(manualValuesText)
                                val schedule = currentGroup(curveGroups, selectedGroupId)?.items?.toTimeLuminanceList()
                                val maxStep = parseStepThreshold(smoothStepText)
                                val jumpWarnings = currentGroup(curveGroups, selectedGroupId)?.items?.detectCurveJumps(maxStep)
                                if (manualValues == null) {
                                    logs.add("手动亮度格式错误，需要 6 个 0-100 数值")
                                } else if (maxStep == null) {
                                    logs.add("跳变步进无效，请填写 1-100")
                                } else if (schedule == null) {
                                    logs.add("时间曲线格式错误，请检查小时、分钟和亮度值")
                                } else if (!jumpWarnings.isNullOrEmpty()) {
                                    logs.add("检测到亮度跳变过大，已弹出提示并阻止发送")
                                    jumpDialogMessage = buildString {
                                        appendLine("当前曲线存在超过阈值的亮度突变，已阻止发送。")
                                        appendLine()
                                        jumpWarnings.take(5).forEach { appendLine(it) }
                                        appendLine()
                                        append("建议先点击“平滑当前曲线”，或调大允许跳变步进后再发送。")
                                    }
                                } else {
                                    val calendar = Calendar.getInstance()
                                    sendWithLog(
                                        connectThread,
                                        CommandUtil.allSet(
                                            calendar.get(Calendar.HOUR_OF_DAY).toByte(),
                                            calendar.get(Calendar.MINUTE).toByte(),
                                            calendar.get(Calendar.SECOND).toByte(),
                                            manualValues,
                                            schedule,
                                            sendConfigAutoMode
                                        ),
                                        logs
                                    )
                                }
                            }
                        )
                    }
                }

                ControlPanelCard(
                    title = "当前方案曲线",
                    subtitle = "查看并设置当前方案亮度曲线。"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val group = currentGroup(curveGroups, selectedGroupId)
                        if (group == null) {
                            Text("当前没有可编辑的方案")
                        } else {
                            CurvePreviewCard(items = group.items)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("详细编辑", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (showDetailedCurveEditor) {
                                                "已展开 24 个时点编辑。"
                                            } else {
                                                "点击展开 24 个时点编辑。"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = showDetailedCurveEditor,
                                        onCheckedChange = { showDetailedCurveEditor = it }
                                    )
                                }
                            }
                            if (showDetailedCurveEditor) {
                                ScheduleEditorList(
                                    items = group.items,
                                    onItemChange = { index, item ->
                                        updateCurveGroup(curveGroups, selectedGroupId) {
                                            val updated = it.items.toMutableList()
                                            updated[index] = item
                                            it.copy(items = updated)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                ControlPanelCard(
                    title = "曲线配置区",
                    subtitle = "管理手动亮度和批量时段设置。"
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CollapsiblePanelHeader(
                                    title = "手动亮度",
                                    subtitle = if (showManualBrightnessEditor) "已展开六路手动亮度调节。" else "点击展开当前六路手动亮度。",
                                    expanded = showManualBrightnessEditor,
                                    onToggle = { showManualBrightnessEditor = !showManualBrightnessEditor }
                                )
                                if (showManualBrightnessEditor) {
                                    SixChannelValueEditor(
                                        value = manualValuesText,
                                        onValueChange = { manualValuesText = it }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "载入预设亮度",
                                        onPrimaryClick = {
                                            manualValuesText = toCsv(defaultHandValues(selectedType))
                                            logs.add("已填入预设亮度")
                                        },
                                        secondaryLabel = "清零手动亮度",
                                        onSecondaryClick = {
                                            pendingCurveDangerAction = CurveDangerAction.ClearManualValues
                                        }
                                    )
                                    CompactSingleOutlinedButton(
                                        label = "发送当前亮度",
                                        onClick = {
                                            val manualValues = parseValuesCsv(manualValuesText)
                                            if (manualValues == null) {
                                                logs.add("手动亮度格式错误，需要 6 个 0-100 数值")
                                            } else {
                                                sendWithLog(connectThread, CommandUtil.handModelLuminance(manualValues), logs)
                                            }
                                        }
                                    )
                                }

                                CollapsiblePanelHeader(
                                    title = "批量设置工具",
                                    subtitle = if (showBatchToolsEditor) "已展开时段、批量亮度和平滑工具。" else "点击展开时段批量设置工具。",
                                    expanded = showBatchToolsEditor,
                                    onToggle = { showBatchToolsEditor = !showBatchToolsEditor }
                                )
                                if (showBatchToolsEditor) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = batchStartHourText,
                                            onValueChange = { batchStartHourText = it },
                                            modifier = Modifier.width(120.dp),
                                            label = { Text("开始小时") }
                                        )
                                        OutlinedTextField(
                                            value = batchEndHourText,
                                            onValueChange = { batchEndHourText = it },
                                            modifier = Modifier.width(120.dp),
                                            label = { Text("结束小时") }
                                        )
                                    }
                                    SixChannelValueEditor(
                                        value = batchValuesText,
                                        onValueChange = { batchValuesText = it },
                                        title = "批量亮度值"
                                    )
                                    OutlinedTextField(
                                        value = smoothStepText,
                                        onValueChange = { smoothStepText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("允许跳变步进（每小时单路最大变化）") },
                                        isError = validateStepThreshold(smoothStepText) != null,
                                        supportingText = {
                                            validateStepThreshold(smoothStepText)?.let { Text(it) }
                                        }
                                    )
                                    Text(
                                        text = solarSummary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (solarRangeSummary.isNotBlank()) {
                                        Text(
                                            text = solarRangeSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "凌晨 0-5",
                                        onPrimaryClick = {
                                            batchStartHourText = "0"
                                            batchEndHourText = "5"
                                            logs.add("已选择凌晨时段 0-5")
                                        },
                                        secondaryLabel = "白天 6-11",
                                        onSecondaryClick = {
                                            batchStartHourText = "6"
                                            batchEndHourText = "11"
                                            logs.add("已选择白天时段 6-11")
                                        }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "午后 12-17",
                                        onPrimaryClick = {
                                            batchStartHourText = "12"
                                            batchEndHourText = "17"
                                            logs.add("已选择午后时段 12-17")
                                        },
                                        secondaryLabel = "夜间 18-23",
                                        onSecondaryClick = {
                                            batchStartHourText = "18"
                                            batchEndHourText = "23"
                                            logs.add("已选择夜间时段 18-23")
                                        }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "带入全天时段",
                                        onPrimaryClick = {
                                            batchStartHourText = "0"
                                            batchEndHourText = "23"
                                            logs.add("已选择全天时段 0-23")
                                        },
                                        secondaryLabel = "带入白昼时段",
                                        onSecondaryClick = {
                                            runSolarAction(SolarAction.ApplyDayRange)
                                        }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "应用黑夜时段",
                                        onPrimaryClick = {
                                            runSolarAction(SolarAction.ApplyNightRange)
                                        },
                                        secondaryLabel = "应用当前时段",
                                        onSecondaryClick = {
                                            val startHour = batchStartHourText.toIntOrNull()
                                            val endHour = batchEndHourText.toIntOrNull()
                                            val values = validateValuesCsv(batchValuesText)
                                            if (startHour == null || endHour == null || startHour !in 0..23 || endHour !in 0..23 || startHour > endHour) {
                                                logs.add("批量时段无效，请填写 0-23 且开始小时不能大于结束小时")
                                            } else if (values != null) {
                                                logs.add(values)
                                            } else {
                                                updateCurveGroup(curveGroups, selectedGroupId) { group ->
                                                    group.copy(items = group.items.map { item ->
                                                        val hour = item.hour.toIntOrNull()
                                                        if (hour != null && hour in startHour..endHour) {
                                                            item.copy(valuesText = batchValuesText)
                                                        } else {
                                                            item
                                                        }
                                                    })
                                                }
                                                logs.add("已将亮度应用到 $startHour:00-$endHour:59")
                                            }
                                        }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "应用全天曲线",
                                        onPrimaryClick = {
                                            val values = validateValuesCsv(batchValuesText)
                                            if (values != null) {
                                                logs.add(values)
                                            } else {
                                                updateCurveGroup(curveGroups, selectedGroupId) { group ->
                                                    group.copy(items = group.items.map { item -> item.copy(valuesText = batchValuesText) })
                                                }
                                                logs.add("已将亮度应用到当前方案全天")
                                            }
                                        },
                                        secondaryLabel = "带入当前亮度",
                                        onSecondaryClick = {
                                            batchValuesText = manualValuesText
                                            logs.add("已将当前手动亮度带入批量设置")
                                        }
                                    )
                                    CompactOutlinedButtonRow(
                                        primaryLabel = "清零全天曲线",
                                        onPrimaryClick = {
                                            pendingCurveDangerAction = CurveDangerAction.ClearAllDayCurve
                                        },
                                        secondaryLabel = "平滑当前曲线",
                                        onSecondaryClick = {
                                            val maxStep = parseStepThreshold(smoothStepText)
                                            val smoothed = currentGroup(curveGroups, selectedGroupId)?.items?.smoothCurveItems(maxStep)
                                            if (maxStep == null) {
                                                logs.add("跳变步进无效，请填写 1-100")
                                            } else if (smoothed == null) {
                                                logs.add("当前方案存在无效亮度值，无法进行平滑处理")
                                            } else {
                                                updateCurveGroup(curveGroups, selectedGroupId) { group ->
                                                    group.copy(items = smoothed)
                                                }
                                                logs.add("已按步进 $maxStep 对当前方案应用平滑过渡")
                                            }
                                        }
                                    )
                                }

                                SectionLabel("设备类型")
                                ChipRow(
                                    values = listOf(Constant.Type.TYPE_K7 to "K7", Constant.Type.TYPE_X4 to "X4"),
                                    selected = selectedType,
                                    onSelected = { selectedType = it }
                                )
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(8.dp))
            ControlPanelCard(
                title = "通信日志",
                subtitle = "保留最近的收发报文和关键调试信息。"
            ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val latestPacket = logs.lastOrNull { it.startsWith("发送:") || it.startsWith("接收:") || it.startsWith("重发:") }
                        if (latestPacket == null) {
                            logs.add("没有可复制的报文")
                        } else {
                            copyToClipboard(context, latestPacket)
                            logs.add("已复制最近一条报文")
                        }
                    }
                ) {
                    Text("复制最近一条报文")
                }
                OutlinedButton(
                    onClick = {
                        if (logs.isEmpty()) {
                            logs.add("没有可导出的日志")
                        } else {
                            exportLogs(context, logs)
                        }
                    }
                ) {
                    Text("导出日志")
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text("暂无日志")
                    } else {
                        logs.takeLast(30).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            }
            }
        }
        }
    }
}

@Composable
private fun CurvePreviewCard(items: List<ScheduleEditorItem>) {
    val series = remember(items) { buildCurvePreviewSeries(items) }
    val channelColors = curveChannelColors()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val whiteChannelContrastColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val axisLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val axisLabelColor = android.graphics.Color.argb(
        (0.76f * 255).toInt(),
        (MaterialTheme.colorScheme.onSurfaceVariant.red * 255).toInt(),
        (MaterialTheme.colorScheme.onSurfaceVariant.green * 255).toInt(),
        (MaterialTheme.colorScheme.onSurfaceVariant.blue * 255).toInt()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "六路亮度概览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            LegendGrid(channelColors = channelColors)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
            ) {
                val leftPadding = 42f
                val rightPadding = 16f
                val topPadding = 14f
                val bottomPadding = 34f
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas
                val axisPaint = Paint().apply {
                    color = axisLabelColor
                    textSize = 20f
                    isAntiAlias = true
                }

                drawLine(
                    color = axisLineColor,
                    start = Offset(leftPadding, topPadding),
                    end = Offset(leftPadding, topPadding + chartHeight),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = axisLineColor,
                    start = Offset(leftPadding, topPadding + chartHeight),
                    end = Offset(leftPadding + chartWidth, topPadding + chartHeight),
                    strokeWidth = 1.5f
                )

                repeat(5) { step ->
                    val y = topPadding + chartHeight * step / 4f
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(leftPadding + chartWidth, y),
                        strokeWidth = 1f
                    )
                }
                repeat(13) { step ->
                    val x = leftPadding + chartWidth * step / 12f
                    drawLine(
                        color = gridColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = 1f
                    )
                }

                val yLabels = listOf("100", "75", "50", "25", "0")
                yLabels.forEachIndexed { index, label ->
                    val y = topPadding + chartHeight * index / 4f
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        4f,
                        y + 8f,
                        axisPaint
                    )
                }

                val xLabels = listOf("00", "02", "04", "06", "08", "10", "12", "14", "16", "18", "20", "22", "24")
                xLabels.forEachIndexed { index, label ->
                    val x = leftPadding + chartWidth * index / 12f
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x - 10f,
                        topPadding + chartHeight + 28f,
                        axisPaint
                    )
                }

                series.forEachIndexed { channelIndex, values ->
                    if (values.isEmpty()) return@forEachIndexed
                    val path = Path()
                    val lineColor = channelColors[channelIndex]
                    val contrastStrokeColor = if (channelIndex == 0) {
                        whiteChannelContrastColor
                    } else {
                        null
                    }
                    values.forEachIndexed { index, value ->
                        val x = if (values.size == 1) {
                            leftPadding + chartWidth / 2f
                        } else {
                            leftPadding + chartWidth * index / (values.lastIndex.coerceAtLeast(1)).toFloat()
                        }
                        val normalized = value.coerceIn(0f, 100f) / 100f
                        val y = topPadding + chartHeight * (1f - normalized)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        drawCircle(
                            color = lineColor,
                            radius = if (channelIndex == 0) 4.8f else 4f,
                            center = Offset(x, y)
                        )
                    }
                    contrastStrokeColor?.let { shadowColor ->
                        drawPath(
                            path = path,
                            color = shadowColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = if (channelIndex == 0) 4.6f else 4f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsiblePanelHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(expanded) {
                detectTapGestures(onTap = { onToggle() })
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (expanded) "收起" else "展开",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SixChannelValueEditor(
    value: String,
    onValueChange: (String) -> Unit,
    title: String = "6 路亮度值"
) {
    val channelNames = listOf("白光", "深蓝", "绿色", "紫外", "浅蓝", "红光")
    val channelColors = curveChannelColors()
    val channelValues = parseValuesCsvDraft(value)
    val errorText = validateValuesCsv(value)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "$title（十进制 0-100）",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        channelNames.forEachIndexed { index, name ->
            val channelValue = channelValues.getOrElse(index) { "0" }
            val sliderValue = channelValue.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = name,
                    modifier = Modifier.width(44.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = channelColors[index]
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { updated ->
                        onValueChange(updateValuesCsvAtIndex(value, index, updated.toInt().coerceIn(0, 100).toString()))
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = channelColors[index],
                        activeTrackColor = channelColors[index],
                        inactiveTrackColor = channelColors[index].copy(alpha = 0.22f)
                    )
                )
                OutlinedTextField(
                    value = channelValue,
                    onValueChange = { updated ->
                        val filtered = updated.filter { it.isDigit() }.take(3)
                        val normalized = filtered.toIntOrNull()?.coerceIn(0, 100)?.toString() ?: filtered
                        onValueChange(updateValuesCsvAtIndex(value, index, normalized))
                    },
                    modifier = Modifier.width(74.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") }
                )
            }
        }
        errorText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LegendGrid(channelColors: List<Color>) {
    val labels = listOf("白光", "深蓝光", "绿色光", "紫外光", "浅蓝光", "红色光")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(3).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { index, label ->
                    val actualIndex = rowIndex * 3 + index
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.62f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, channelColors[actualIndex].copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(channelColors[actualIndex])
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildCurvePreviewSeries(items: List<ScheduleEditorItem>): List<List<Float>> {
    val channels = List(6) { mutableListOf<Float>() }
    items.forEach { item ->
        val values = parseValuesCsv(item.valuesText)?.map { (it.toInt() and 0xFF).toFloat() } ?: return@forEach
        values.forEachIndexed { index, value ->
            channels[index].add(value)
        }
    }
    return channels
}

@Composable
private fun FixedPacketPanel(logs: List<String>) {
    val packetLogs = logs
        .filter { it.startsWith("发送:") || it.startsWith("接收:") || it.startsWith("重发:") }
        .takeLast(5)
        .reversed()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("最近 5 条报文", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (packetLogs.isEmpty()) {
                Text("暂无报文", style = MaterialTheme.typography.bodyMedium)
            } else {
                packetLogs.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun ControlSummaryCard(
    connectionState: String,
    deviceName: String,
    deviceType: String,
    currentGroup: String,
    deviceIp: String,
    onDeviceIpChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "设备总览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "优先确认连接、设备名称和当前工作组，再进行模式命令和曲线下发。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusGrid(
                    connectionState = connectionState,
                    deviceName = deviceName,
                    deviceType = deviceType,
                    currentGroup = currentGroup
                )
                OutlinedTextField(
                    value = deviceIp,
                    onValueChange = onDeviceIpChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("设备 IP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White.copy(alpha = 0.86f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ControlPanelCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun StatusGrid(
    connectionState: String,
    deviceName: String,
    deviceType: String,
    currentGroup: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val chipWidth = (maxWidth - 12.dp) / 2
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(
                    title = "连接",
                    value = connectionState,
                    modifier = Modifier.width(chipWidth)
                )
                StatusChip(
                    title = "设备名称",
                    value = deviceName,
                    modifier = Modifier.width(chipWidth)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(
                    title = "设备类型",
                    value = deviceType,
                    modifier = Modifier.width(chipWidth)
                )
                StatusChip(
                    title = "当前方案",
                    value = currentGroup,
                    modifier = Modifier.width(chipWidth)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                title == "连接" && value.contains("连接") -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                title == "连接" && value.contains("未") -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                else -> Color.White.copy(alpha = 0.72f)
            }
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            when {
                title == "连接" && value.contains("连接") -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CompactButtonRow(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val buttonWidth = (maxWidth - 10.dp) / 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(50.dp),
                onClick = onPrimaryClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(50.dp),
                onClick = onSecondaryClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactOutlinedButtonRow(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val buttonWidth = (maxWidth - 10.dp) / 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(50.dp),
                onClick = onPrimaryClick,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(50.dp),
                onClick = onSecondaryClick,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactSingleOutlinedButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScheduleEditorList(
    items: List<ScheduleEditorItem>,
    onItemChange: (Int, ScheduleEditorItem) -> Unit
) {
    val sections = remember(items) { buildScheduleSections(items) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sections.size) { sectionIndex ->
                val section = sections[sectionIndex]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = {
                                    val sourceValues = section.items.firstOrNull()?.second?.valuesText ?: return@OutlinedButton
                                    section.items.forEach { indexedItem ->
                                        val index = indexedItem.first
                                        val item = indexedItem.second
                                        onItemChange(index, item.copy(valuesText = sourceValues))
                                    }
                                }
                            ) {
                                Text("整段统一亮度")
                            }
                        }
                        section.items.forEach { indexedItem ->
                            val index = indexedItem.first
                            val item = indexedItem.second
                            val valueError = validateValuesCsv(item.valuesText)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("时点 ${index + 1}", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item.hour,
                                        onValueChange = { onItemChange(index, item.copy(hour = it)) },
                                        modifier = Modifier.width(120.dp),
                                        label = { Text("时") }
                                    )
                                    OutlinedTextField(
                                        value = item.minute,
                                        onValueChange = { onItemChange(index, item.copy(minute = it)) },
                                        modifier = Modifier.width(120.dp),
                                        label = { Text("分") }
                                    )
                                }
                                    OutlinedTextField(
                                        value = item.valuesText,
                                        onValueChange = { onItemChange(index, item.copy(valuesText = it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("亮度值（十进制 0-100，6 路逗号分隔）") },
                                        isError = valueError != null,
                                        supportingText = {
                                            if (valueError != null) {
                                            Text(valueError)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sendWithLog(connectThread: ConnectThread?, payload: ByteArray, logs: MutableList<String>) {
    if (connectThread == null) {
        logs.add("请先连接设备")
        return
    }
    connectThread.sendData(payload)
    logs.add("发送: ${HexStringUtil.byteArrayToHex(payload)}")
}

@Composable
private fun <T> ChipRow(
    values: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(values.size) { index ->
            val (value, label) = values[index]
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(if (selected == value) "$label · 当前" else label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun HeroCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LandingHeroCard(
    title: String,
    body: String,
    badges: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                BadgeGrid(badges = badges)
            }
        }
    }
}

@Composable
private fun BadgeGrid(badges: List<String>) {
    val rows = badges.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowBadges ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowBadges.forEach { badge ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(3 - rowBadges.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun screenBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.44f),
            MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentTopBarColors() = TopAppBarDefaults.centerAlignedTopAppBarColors(
    containerColor = Color.Transparent,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground
)

private fun describeType(value: Int): String = if (value == Constant.Type.TYPE_X4) "X4" else "K7"

private fun describeModel(value: Int): String = when (value) {
    Constant.Model.MODEL_LPS -> "LPS"
    Constant.Model.MODEL_SL -> "SL"
    else -> "SPS"
}

private data class CurveGroup(
    val id: String,
    val name: String,
    val isPreset: Boolean,
    val items: List<ScheduleEditorItem>
)

private enum class CurveDangerAction {
    ClearManualValues,
    ClearAllDayCurve,
    ResetPresetGroups,
    DeleteCurrentScheme
}

private enum class SolarAction {
    ApplyDayRange,
    ApplyNightRange
}

private const val DEFAULT_DEVICE_AP_PASSWORD = "12345678"

private data class DeviceHotspot(
    val ssid: String,
    val level: Int
) {
    val signalLabel: String
        get() = when {
            level >= -55 -> "强"
            level >= -67 -> "良"
            level >= -75 -> "中"
            else -> "弱"
        }
}

private data class LanNetworkInfo(
    val ssid: String,
    val localIp: String,
    val gatewayIp: String,
    val subnetPrefix: String
)

private data class LanDiscoveredDevice(
    val ip: String,
    val deviceName: String? = null
) {
    val displayLabel: String
        get() = when {
            !deviceName.isNullOrBlank() -> "$ip · $deviceName"
            else -> ip
        }
}

private data class EsptouchDiagnostics(
    val ssid: String?,
    val bssid: String?,
    val frequencyMhz: Int?,
    val localIp: String?,
    val gatewayIp: String?,
    val hasLocationPermission: Boolean,
    val locationServiceEnabled: Boolean
) {
    val frequencyLabel: String
        get() = when {
            frequencyMhz == null -> "未读取"
            frequencyMhz in 2400..2500 -> "${frequencyMhz}MHz（2.4G）"
            frequencyMhz in 4900..5900 -> "${frequencyMhz}MHz（5G）"
            else -> "${frequencyMhz}MHz"
        }

    val warnings: List<String>
        get() = buildList {
            if (ssid.isNullOrBlank()) add("未读取到当前 Wi-Fi 名称")
            if (bssid.isNullOrBlank()) add("未读取到当前 Wi-Fi BSSID")
            if (frequencyMhz == null) {
                add("未读取到当前 Wi-Fi 频段")
            } else if (frequencyMhz !in 2400..2500) {
                add("当前不是 2.4G Wi-Fi，Esptouch 大概率会失败")
            }
            if (!hasLocationPermission) add("未授予定位权限")
            if (!locationServiceEnabled) add("系统定位服务未开启")
            if (localIp.isNullOrBlank() || gatewayIp.isNullOrBlank()) add("未读取到完整局域网信息")
        }

    fun startBlockReason(ssid: String, bssid: String): String? {
        if (ssid.isBlank() || bssid.isBlank()) {
            return "请先读取当前 Wi-Fi 名称和 BSSID。"
        }
        if (!hasLocationPermission) {
            return "未授予定位权限，先授权后再开始 Esptouch。"
        }
        if (!locationServiceEnabled) {
            return "系统定位服务未开启，先开启定位服务再开始 Esptouch。"
        }
        if (frequencyMhz == null) {
            return "未读取到当前 Wi-Fi 频段，请先刷新当前 Wi-Fi。"
        }
        if (frequencyMhz !in 2400..2500) {
            return "当前 Wi-Fi 不是 2.4G，请切到 2.4G 后再开始 Esptouch。"
        }
        return null
    }
}

private data class EsptouchRunResult(
    val results: List<IEsptouchResult>?,
    val errorMessage: String? = null
)

private class AirkissSession(
    private val cancelAction: () -> Unit
) {
    fun cancel() = cancelAction()
}

private data class ScheduleEditorItem(
    val hour: String,
    val minute: String,
    val valuesText: String
)

private fun TimeLuminance.toEditorItem(): ScheduleEditorItem {
    return ScheduleEditorItem(
        hour = hour.toString(),
        minute = minute.toString(),
        valuesText = toCsv(luminanceValue)
    )
}

private fun List<ScheduleEditorItem>.toTimeLuminanceList(): List<TimeLuminance>? {
    return map { item ->
        val hour = item.hour.toIntOrNull()
        val minute = item.minute.toIntOrNull()
        val values = parseValuesCsv(item.valuesText)
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59 || values == null) {
            return null
        }
        TimeLuminance(hour = hour, minute = minute, luminanceValue = values)
    }
}

private fun parseValuesCsv(value: String): ByteArray? {
    val parts = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size != 6) return null
    val values = ByteArray(6)
    parts.forEachIndexed { index, part ->
        val number = part.toIntOrNull() ?: return null
        if (number !in 0..100) return null
        values[index] = number.toByte()
    }
    return values
}

private fun parseValuesCsvDraft(value: String): List<String> {
    val parts = value.split(",")
        .map { it.trim() }
        .toMutableList()
    while (parts.size < 6) {
        parts += "0"
    }
    return parts.take(6)
}

private fun updateValuesCsvAtIndex(source: String, index: Int, newValue: String): String {
    val parts = parseValuesCsvDraft(source).toMutableList()
    if (index in parts.indices) {
        parts[index] = newValue
    }
    return parts.joinToString(",")
}

private fun validateValuesCsv(value: String): String? {
    val parts = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size != 6) return "需要填写 6 个数值"
    if (parts.any { it.toIntOrNull() == null }) return "必须全部是数字"
    if (parts.any { (it.toIntOrNull() ?: 0) !in 0..100 }) return "每路范围必须在 0-100"
    return null
}

private fun parseStepThreshold(value: String): Int? {
    val threshold = value.trim().toIntOrNull() ?: return null
    return threshold.takeIf { it in 1..100 }
}

private fun validateStepThreshold(value: String): String? {
    return if (parseStepThreshold(value) == null) "请填写 1-100 的整数" else null
}

private fun List<ScheduleEditorItem>.smoothCurveItems(maxStepPerHour: Int?): List<ScheduleEditorItem>? {
    if (maxStepPerHour == null) return null
    if (isEmpty()) return this
    val parsed = map { item ->
        val values = parseValuesCsv(item.valuesText) ?: return null
        item to values.map { it.toInt() and 0xFF }.toMutableList()
    }.toMutableList()

    for (index in 1 until parsed.size) {
        val previous = parsed[index - 1].second
        val current = parsed[index].second
        if (previous.isAllZeroValues() || current.isAllZeroValues()) {
            continue
        }
        for (channel in 0 until current.size) {
            val delta = current[channel] - previous[channel]
            if (delta > maxStepPerHour) {
                current[channel] = previous[channel] + maxStepPerHour
            } else if (delta < -maxStepPerHour) {
                current[channel] = previous[channel] - maxStepPerHour
            }
        }
    }

    return parsed.map { (item, values) ->
        item.copy(valuesText = values.joinToString(","))
    }
}

private fun List<ScheduleEditorItem>.detectCurveJumps(maxStepPerHour: Int?): List<String>? {
    if (maxStepPerHour == null) return null
    if (isEmpty()) return emptyList()
    val warnings = mutableListOf<String>()
    for (index in 1 until size) {
        val previous = parseValuesCsv(this[index - 1].valuesText)?.map { it.toInt() and 0xFF } ?: return null
        val current = parseValuesCsv(this[index].valuesText)?.map { it.toInt() and 0xFF } ?: return null
        if (previous.isAllZeroValues() || current.isAllZeroValues()) {
            continue
        }
        for (channel in current.indices) {
            val delta = kotlin.math.abs(current[channel] - previous[channel])
            if (delta > maxStepPerHour) {
                warnings += "时点 ${index} -> ${index + 1} 的通道 ${channel + 1} 跳变 ${delta}，超过阈值 $maxStepPerHour"
            }
        }
    }
    return warnings
}

private fun List<Int>.isAllZeroValues(): Boolean = all { it == 0 }

private fun toCsv(values: ByteArray): String {
    return values.joinToString(",") { (it.toInt() and 0xFF).toString() }
}

private fun curveChannelColors(): List<Color> {
    return listOf(
        Color(0xFFBFD9FF),
        Color(0xFF6F8CFF),
        Color(0xFF8AD8A8),
        Color(0xFFC8A6FF),
        Color(0xFF8FE7F7),
        Color(0xFFFFA3A3)
    )
}

private fun buildDefaultCurveGroups(type: Int): List<CurveGroup> {
    return listOf(
        CurveGroup(
            id = "preset_sps",
            name = "SPS",
            isPreset = true,
            items = defaultSchedule(type, Constant.Model.MODEL_SPS).map { it.toEditorItem() }
        ),
        CurveGroup(
            id = "preset_lps",
            name = "LPS",
            isPreset = true,
            items = defaultSchedule(type, Constant.Model.MODEL_LPS).map { it.toEditorItem() }
        ),
        CurveGroup(
            id = "preset_sl",
            name = "SL",
            isPreset = true,
            items = defaultSchedule(type, Constant.Model.MODEL_SL).map { it.toEditorItem() }
        )
    )
}

private fun buildDeviceCurveGroupId(deviceName: String): String {
    val normalized = deviceName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]+"), "_")
        .trim('_')
    return "device_${normalized.ifBlank { "unknown" }}"
}

private fun saveCurveGroups(
    dataStore: com.mualanimarine.betterreeflightmanager.util.SharedPreferencesUtil,
    type: Int,
    groups: List<CurveGroup>,
    selectedGroupId: String
) {
    val payload = JSONArray()
    groups.forEach { group ->
        val items = JSONArray()
        group.items.forEach { item ->
            items.put(
                JSONObject()
                    .put("hour", item.hour)
                    .put("minute", item.minute)
                    .put("valuesText", item.valuesText)
            )
        }
        payload.put(
            JSONObject()
                .put("id", group.id)
                .put("name", group.name)
                .put("isPreset", group.isPreset)
                .put("items", items)
        )
    }
    dataStore.setCurveGroups(type, payload.toString())
    dataStore.setSelectedCurveGroupId(type, selectedGroupId)
}

private fun loadCurveGroups(
    dataStore: com.mualanimarine.betterreeflightmanager.util.SharedPreferencesUtil,
    type: Int
): List<CurveGroup> {
    val raw = dataStore.getCurveGroups(type).orEmpty()
    if (raw.isBlank()) {
        return buildDefaultCurveGroups(type)
    }
    return runCatching {
        val groupsJson = JSONArray(raw)
        buildList {
            for (index in 0 until groupsJson.length()) {
                val group = groupsJson.getJSONObject(index)
                val itemsJson = group.getJSONArray("items")
                val items = buildList {
                    for (itemIndex in 0 until itemsJson.length()) {
                        val item = itemsJson.getJSONObject(itemIndex)
                        add(
                            ScheduleEditorItem(
                                hour = item.optString("hour"),
                                minute = item.optString("minute"),
                                valuesText = item.optString("valuesText")
                            )
                        )
                    }
                }
                add(
                    CurveGroup(
                        id = group.optString("id"),
                        name = group.optString("name"),
                        isPreset = group.optBoolean("isPreset", false),
                        items = items
                    )
                )
            }
        }.ifEmpty { buildDefaultCurveGroups(type) }
    }.getOrElse {
        buildDefaultCurveGroups(type)
    }
}

private fun parseImportedScheme(raw: String): CurveGroup? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    parseImportedRawScheme(trimmed)?.let { return it }
    return runCatching {
        val json = JSONObject(trimmed)
        val itemsJson = json.optJSONArray("items") ?: json.optJSONArray("schedule") ?: return null
        val items = buildList {
            for (index in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(index)
                val valuesText = when {
                    item.has("valuesText") -> item.optString("valuesText")
                    item.has("values") -> {
                        val values = item.getJSONArray("values")
                        buildString {
                            for (valueIndex in 0 until values.length()) {
                                if (valueIndex > 0) append(",")
                                append(values.getInt(valueIndex))
                            }
                        }
                    }
                    else -> return null
                }
                add(
                    ScheduleEditorItem(
                        hour = item.opt("hour")?.toString().orEmpty(),
                        minute = item.opt("minute")?.toString().orEmpty(),
                        valuesText = valuesText
                    )
                )
            }
        }
        if (items.size != 24 || items.any { validateValuesCsv(it.valuesText) != null }) return null
        CurveGroup(
            id = "",
            name = json.optString("name", "导入方案"),
            isPreset = false,
            items = items
        )
    }.getOrNull()
}

private fun parseImportedRawScheme(raw: String): CurveGroup? {
    val cleaned = raw
        .replace("\n", "")
        .replace("\r", "")
        .replace("\t", "")
        .trim()
        .lowercase()

    val payload = when {
        "#" in cleaned -> cleaned.substringAfter("#")
        cleaned.length == 24 * 16 -> cleaned
        else -> return null
    }

    if (payload.length != 24 * 16) return null
    if (!payload.all { it in '0'..'9' || it in 'a'..'f' }) return null

    val items = buildList {
        for (index in 0 until 24) {
            val group = payload.substring(index * 16, (index + 1) * 16)
            val numbers = group.chunked(2).map { it.toInt(16) }
            val valuesText = numbers.drop(2).joinToString(",")
            add(
                ScheduleEditorItem(
                    hour = numbers[0].toString(),
                    minute = numbers[1].toString(),
                    valuesText = valuesText
                )
            )
        }
    }

    if (items.any {
            it.hour.toIntOrNull() !in 0..23 ||
                it.minute.toIntOrNull() !in 0..59 ||
                validateValuesCsv(it.valuesText) != null
        }
    ) {
        return null
    }

    return CurveGroup(
        id = "",
        name = "导入方案",
        isPreset = false,
        items = items
    )
}

private fun decodeQrTextFromUri(context: Context, uri: Uri): String? {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    return decodeQrTextFromBitmap(bitmap)
}

private fun decodeQrTextFromBitmap(bitmap: Bitmap): String? {
    val normalizedBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
    val candidates = buildList {
        add(normalizedBitmap)
        add(rotateBitmap(normalizedBitmap, 90f))
        add(rotateBitmap(normalizedBitmap, 180f))
        add(rotateBitmap(normalizedBitmap, 270f))
    }

    return candidates.firstNotNullOfOrNull { candidate ->
        decodeQrTextFromPixels(candidate)
    }
}

private fun decodeQrTextFromPixels(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
    val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
    return decodeBinaryBitmap(binaryBitmap) ?: decodeBinaryBitmap(invertedBitmap)
}

private fun decodeBinaryBitmap(binaryBitmap: BinaryBitmap): String? {
    val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.NEED_RESULT_POINT_CALLBACK to ResultPointCallback { }
    )
    return runCatching {
        val reader = MultiFormatReader()
        reader.setHints(hints)
        reader.decode(binaryBitmap).text
    }.getOrNull()
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
private fun QrCameraScannerDialog(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scannerExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            scannerExecutor.shutdown()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("扫码导入方案", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "将二维码放在画面中央，识别成功后会自动导入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    factory = { viewContext ->
                        val previewView = PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(viewContext)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { imageAnalysis ->
                                    imageAnalysis.setAnalyzer(scannerExecutor) { imageProxy ->
                                        val text = decodeQrTextFromImageProxy(imageProxy)
                                        imageProxy.close()
                                        if (!text.isNullOrBlank()) {
                                            previewView.post { onScanned(text) }
                                        }
                                    }
                                }
                            runCatching {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(viewContext))
                        previewView
                    }
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text("取消")
                }
            }
        }
    }
}

private fun decodeQrTextFromImageProxy(imageProxy: ImageProxy): String? {
    val bitmap = imageProxyToBitmap(imageProxy) ?: return null
    val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
    return decodeQrTextFromBitmap(rotated)
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val nv21 = imageProxyToNv21(imageProxy) ?: return null
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val stream = ByteArrayOutputStream()
    if (!yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, stream)) {
        return null
    }
    val bytes = stream.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun imageProxyToNv21(imageProxy: ImageProxy): ByteArray? {
    if (imageProxy.planes.size < 3) return null
    val crop = imageProxy.cropRect
    val width = crop.width()
    val height = crop.height()
    val ySize = width * height
    val uvSize = width * height / 2
    val out = ByteArray(ySize + uvSize)
    var outputOffset = 0

    imageProxy.planes.forEachIndexed { planeIndex, plane ->
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val planeCrop = when (planeIndex) {
            0 -> crop
            else -> Rect(crop.left / 2, crop.top / 2, crop.right / 2, crop.bottom / 2)
        }
        val planeWidth = planeCrop.width()
        val planeHeight = planeCrop.height()
        val rowBuffer = ByteArray(rowStride)
        val channelOffset = when (planeIndex) {
            0 -> 0
            1 -> ySize + 1
            2 -> ySize
            else -> return null
        }
        val outputStride = when (planeIndex) {
            0 -> 1
            else -> 2
        }
        outputOffset = channelOffset
        buffer.position(rowStride * planeCrop.top + pixelStride * planeCrop.left)
        for (row in 0 until planeHeight) {
            val length = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                (planeWidth - 1) * pixelStride + 1
            }
            buffer.get(rowBuffer, 0, length)
            if (pixelStride == 1 && outputStride == 1) {
                System.arraycopy(rowBuffer, 0, out, outputOffset, planeWidth)
                outputOffset += planeWidth
            } else {
                for (col in 0 until planeWidth) {
                    out[outputOffset] = rowBuffer[col * pixelStride]
                    outputOffset += outputStride
                }
            }
            if (row < planeHeight - 1) {
                buffer.position(buffer.position() + rowStride - length)
            }
        }
    }
    return out
}

private fun currentGroup(groups: List<CurveGroup>, selectedId: String): CurveGroup? {
    return groups.firstOrNull { it.id == selectedId }
}

private fun updateCurveGroup(
    groups: MutableList<CurveGroup>,
    selectedId: String,
    transform: (CurveGroup) -> CurveGroup
) {
    val index = groups.indexOfFirst { it.id == selectedId }
    if (index >= 0) {
        groups[index] = transform(groups[index])
    }
}

private data class ScheduleSection(
    val title: String,
    val items: List<Pair<Int, ScheduleEditorItem>>
)

private fun buildScheduleSections(items: List<ScheduleEditorItem>): List<ScheduleSection> {
    val indexed = items.mapIndexed { index, item -> index to item }
    return listOf(
        ScheduleSection("凌晨时段 00:00-05:59", indexed.filter { it.second.hour.toIntOrNull() in 0..5 }),
        ScheduleSection("白天时段 06:00-11:59", indexed.filter { it.second.hour.toIntOrNull() in 6..11 }),
        ScheduleSection("午后时段 12:00-17:59", indexed.filter { it.second.hour.toIntOrNull() in 12..17 }),
        ScheduleSection("夜间时段 18:00-23:59", indexed.filter { it.second.hour.toIntOrNull() in 18..23 })
    ).filter { it.items.isNotEmpty() }
}

private data class SolarTimes(
    val sunriseHour: Int,
    val sunriseMinute: Int,
    val sunsetHour: Int,
    val sunsetMinute: Int
) {
    val sunriseLabel: String get() = "%02d:%02d".format(sunriseHour, sunriseMinute)
    val sunsetLabel: String get() = "%02d:%02d".format(sunsetHour, sunsetMinute)
}

private fun formatSolarRangeSummary(solarTimes: SolarTimes): String {
    return buildString {
        append("白昼: ")
        append("%02d".format(solarTimes.sunriseHour))
        append("-")
        append("%02d".format(solarTimes.sunsetHour))
        append("    黑夜: 00-")
        append("%02d".format(solarTimes.sunriseHour))
        append(", ")
        append("%02d".format(solarTimes.sunsetHour))
        append("-23")
    }
}

private fun requestSolarTimes(
    context: Context,
    onResult: (SolarTimes?) -> Unit
) {
    val cachedLocation = findBestLastKnownLocation(context)
    if (cachedLocation != null) {
        onResult(
            calculateSolarTimes(
                latitude = cachedLocation.latitude,
                longitude = cachedLocation.longitude,
                calendar = Calendar.getInstance()
            )
        )
        return
    }

    requestCurrentLocation(context) { location ->
        onResult(
            location?.let {
                calculateSolarTimes(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    calendar = Calendar.getInstance()
                )
            }
        )
    }
}

private fun findBestLastKnownLocation(context: Context): Location? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation && !hasCoarseLocation) return null

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val candidateProviders = buildList {
        if (hasFineLocation) {
            add(LocationManager.GPS_PROVIDER)
        }
        if (hasCoarseLocation) {
            add(LocationManager.NETWORK_PROVIDER)
        }
        add(LocationManager.PASSIVE_PROVIDER)
    }

    return candidateProviders
        .distinct()
        .mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        .maxByOrNull { it.time }
}

private fun requestCurrentLocation(
    context: Context,
    onResult: (Location?) -> Unit
) {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation && !hasCoarseLocation) {
        onResult(null)
        return
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onResult(null)
        return
    }

    val provider = when {
        hasFineLocation && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        hasCoarseLocation && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) -> LocationManager.PASSIVE_PROVIDER
        else -> null
    }
    if (provider == null) {
        onResult(null)
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                onResult(location)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    onResult(location)
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    onResult(null)
                }
            }
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    } catch (_: SecurityException) {
        onResult(null)
    } catch (_: IllegalArgumentException) {
        onResult(null)
    }
}

private fun calculateSolarTimes(
    latitude: Double,
    longitude: Double,
    calendar: Calendar
): SolarTimes? {
    val sunriseMinutes = calculateSolarMinutes(latitude, longitude, calendar, true) ?: return null
    val sunsetMinutes = calculateSolarMinutes(latitude, longitude, calendar, false) ?: return null
    return SolarTimes(
        sunriseHour = sunriseMinutes / 60,
        sunriseMinute = sunriseMinutes % 60,
        sunsetHour = sunsetMinutes / 60,
        sunsetMinute = sunsetMinutes % 60
    )
}

private fun calculateSolarMinutes(
    latitude: Double,
    longitude: Double,
    calendar: Calendar,
    sunrise: Boolean
): Int? {
    val zenith = 90.833
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR).toDouble()
    val lngHour = longitude / 15.0
    val approximateTime = if (sunrise) {
        dayOfYear + ((6.0 - lngHour) / 24.0)
    } else {
        dayOfYear + ((18.0 - lngHour) / 24.0)
    }

    val meanAnomaly = (0.9856 * approximateTime) - 3.289
    var trueLongitude = meanAnomaly +
        (1.916 * sin(Math.toRadians(meanAnomaly))) +
        (0.020 * sin(Math.toRadians(2.0 * meanAnomaly))) +
        282.634
    trueLongitude = ((trueLongitude % 360.0) + 360.0) % 360.0

    var rightAscension = Math.toDegrees(atanSafe(0.91764 * tan(Math.toRadians(trueLongitude))))
    rightAscension = ((rightAscension % 360.0) + 360.0) % 360.0

    val trueLongitudeQuadrant = floor(trueLongitude / 90.0) * 90.0
    val rightAscensionQuadrant = floor(rightAscension / 90.0) * 90.0
    rightAscension += trueLongitudeQuadrant - rightAscensionQuadrant
    rightAscension /= 15.0

    val sinDeclination = 0.39782 * sin(Math.toRadians(trueLongitude))
    val cosDeclination = cos(asinSafe(sinDeclination))
    val cosHourAngle =
        (cos(Math.toRadians(zenith)) - (sinDeclination * sin(Math.toRadians(latitude)))) /
            (cosDeclination * cos(Math.toRadians(latitude)))
    if (cosHourAngle < -1.0 || cosHourAngle > 1.0) return null

    val hourAngle = if (sunrise) {
        360.0 - Math.toDegrees(acos(cosHourAngle))
    } else {
        Math.toDegrees(acos(cosHourAngle))
    } / 15.0

    val localMeanTime = hourAngle + rightAscension - (0.06571 * approximateTime) - 6.622
    val utcHours = ((localMeanTime - lngHour) % 24.0 + 24.0) % 24.0
    val timezoneOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3_600_000.0
    val localHours = ((utcHours + timezoneOffsetHours) % 24.0 + 24.0) % 24.0
    val totalMinutes = (localHours * 60.0).toInt()
    return totalMinutes.coerceIn(0, 23 * 60 + 59)
}

private fun atanSafe(value: Double): Double = kotlin.math.atan(value)

private fun asinSafe(value: Double): Double = kotlin.math.asin(value)

private fun copyToClipboard(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("packet", value))
}

private fun readCurrentGatewayIp(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val gateway = wifiManager.dhcpInfo?.gateway ?: return null
    if (gateway == 0) return null
    return intToIpv4(gateway)
}

@Suppress("DEPRECATION")
private fun readCurrentWifiSsid(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val ssid = wifiManager.connectionInfo?.ssid
        ?.removePrefix("\"")
        ?.removeSuffix("\"")
        ?.trim()
        .orEmpty()
    return ssid.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
}

@Suppress("DEPRECATION")
private fun readCurrentWifiBssid(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    return wifiManager.connectionInfo?.bssid?.takeIf { it.isNotBlank() }
}

@Suppress("DEPRECATION")
private fun readCurrentWifiFrequencyMhz(context: Context): Int? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    return wifiManager.connectionInfo?.frequency?.takeIf { it > 0 }
}

private fun isLocationServiceEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF
        ) != Settings.Secure.LOCATION_MODE_OFF
    }
}

private fun readEsptouchDiagnostics(context: Context): EsptouchDiagnostics {
    val lanInfo = readCurrentLanNetworkInfo(context)
    return EsptouchDiagnostics(
        ssid = readCurrentWifiSsid(context),
        bssid = readCurrentWifiBssid(context),
        frequencyMhz = readCurrentWifiFrequencyMhz(context),
        localIp = lanInfo?.localIp,
        gatewayIp = lanInfo?.gatewayIp,
        hasLocationPermission = requiredWifiPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        },
        locationServiceEnabled = isLocationServiceEnabled(context)
    )
}

@Suppress("DEPRECATION")
private fun readCurrentLanNetworkInfo(context: Context): LanNetworkInfo? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val dhcpInfo = wifiManager.dhcpInfo ?: return null
    val gatewayIp = dhcpInfo.gateway.takeIf { it != 0 }?.let(::intToIpv4) ?: return null
    val localIp = dhcpInfo.ipAddress.takeIf { it != 0 }?.let(::intToIpv4) ?: return null
    val ssid = wifiManager.connectionInfo?.ssid
        ?.removePrefix("\"")
        ?.removeSuffix("\"")
        ?.takeIf { !it.isNullOrBlank() && it != "<unknown ssid>" }
        ?: "未知 Wi-Fi"
    val subnetPrefix = gatewayIp.substringBeforeLast(".", "")
    if (subnetPrefix.isBlank()) return null
    return LanNetworkInfo(
        ssid = ssid,
        localIp = localIp,
        gatewayIp = gatewayIp,
        subnetPrefix = subnetPrefix
    )
}

private fun hasWifiScanPermission(context: Context): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasNearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.NEARBY_WIFI_DEVICES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return hasFineLocation && hasNearbyWifi
}

private fun hasWifiConnectPermission(context: Context): Boolean {
    return hasWifiScanPermission(context)
}

private fun missingWifiPermissions(context: Context): List<String> {
    return requiredWifiPermissions().filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredWifiPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

private fun scanMatchingSsids(context: Context, prefix: String): List<DeviceHotspot> {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return emptyList()
    return try {
        wifiManager.startScan()
        wifiManager.scanResults
            ?.mapNotNull { result ->
                result.sanitizedSsid()?.takeIf { it.startsWith(prefix) }?.let {
                    DeviceHotspot(ssid = it, level = result.level)
                }
            }
            ?.groupBy { it.ssid }
            ?.map { (_, hotspots) -> hotspots.maxBy { it.level } }
            ?.sortedByDescending { it.level }
            .orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }
}

private fun scanReachableDevices(
    subnetPrefix: String,
    port: Int,
    onDiscovered: ((LanDiscoveredDevice) -> Unit)? = null,
    onFinished: (List<LanDiscoveredDevice>) -> Unit
) {
    Thread {
        val foundDevices = mutableListOf<LanDiscoveredDevice>()
        val lock = Any()
        val executor = Executors.newFixedThreadPool(24)
        try {
            (1..254).forEach { host ->
                val candidateIp = "$subnetPrefix.$host"
                executor.execute {
                    runCatching {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(candidateIp, port), 120)
                        }
                        val device = LanDiscoveredDevice(ip = candidateIp)
                        synchronized(lock) {
                            foundDevices += device
                        }
                        Handler(Looper.getMainLooper()).post {
                            onDiscovered?.invoke(device)
                        }
                    }
                }
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(8, TimeUnit.SECONDS)
            Handler(Looper.getMainLooper()).post {
                onFinished(foundDevices.sortedWith(compareBy { it.ip.ipToSortableValue() }))
            }
        }
    }.start()
}

private fun String.ipToSortableValue(): Long {
    return split('.')
        .mapNotNull { it.toIntOrNull() }
        .fold(0L) { acc, value -> (acc shl 8) + value }
}

private fun ScanResult.sanitizedSsid(): String? {
    val value = SSID?.removePrefix("\"")?.removeSuffix("\"")?.trim().orEmpty()
    return value.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
}

private fun connectToK7ProHotspot(
    context: Context,
    ssid: String,
    password: String,
    onStatus: (String) -> Unit,
    onConnected: (String?) -> Unit,
    onError: (String) -> Unit
) {
    WifiApConnector.connect(
        context = context,
        ssid = ssid,
        password = password,
        onStatus = onStatus,
        onConnected = onConnected,
        onError = onError
    )
}

private fun startAirkissProvision(
    ssid: String,
    password: String,
    onStatus: (String) -> Unit,
    onReceived: (String) -> Unit,
    onFinished: () -> Unit
): AirkissSession {
    val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    val mainHandler = Handler(Looper.getMainLooper())
    val senderThread = Thread {
        try {
            mainHandler.post { onStatus("正在发送 Airkiss 配网信息...") }
            val encoder = AirKissEncoder(ssid, password)
            val payload = ByteArray(1500)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val targetAddress = InetAddress.getByName("255.255.255.255")
                repeat(5) {
                    for (packetLength in encoder.getEncodedData()) {
                        if (cancelled.get()) return@use
                        socket.send(DatagramPacket(payload, packetLength, targetAddress, 10000))
                        Thread.sleep(4L)
                    }
                }
            }
            if (!cancelled.get()) {
                mainHandler.post { onStatus("Airkiss 广播已发送，正在等待设备回包...") }
                startAirkissReceiver(cancelled, onReceived, onStatus, onFinished)
            } else {
                mainHandler.post(onFinished)
            }
        } catch (error: Exception) {
            Log.e("Airkiss", "send failed", error)
            if (!cancelled.get()) {
                mainHandler.post {
                    onStatus("Airkiss 发送失败：${error.message ?: "未知错误"}")
                    onFinished()
                }
            }
        }
    }.apply {
        name = "airkiss-send"
        start()
    }

    return AirkissSession {
        cancelled.set(true)
        runCatching { senderThread.interrupt() }
    }
}

private suspend fun runEsptouchProvision(
    context: Context,
    ssid: String,
    bssid: String,
    password: String,
    useBroadcast: Boolean,
    totalTimeoutMillis: Int,
    onTaskCreated: (EsptouchTask) -> Unit
): EsptouchRunResult {
    return withContext(Dispatchers.IO) {
        runCatching {
            val ssidBytes = TouchNetUtil.getOriginalSsidBytes(
                (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
            ) ?: ssid.encodeToByteArray()
            val task = EsptouchTask(
                ssidBytes,
                TouchNetUtil.parseBssid2bytes(bssid),
                password.encodeToByteArray(),
                context.applicationContext
            )
            withContext(Dispatchers.Main) {
                onTaskCreated(task)
            }
            task.setPackageBroadcast(useBroadcast)
            task.setWaitUdpTotalMillisecond(totalTimeoutMillis)
            EsptouchRunResult(results = task.executeForResults(1))
        }.getOrElse { error ->
            Log.e("Esptouch", "execute failed", error)
            EsptouchRunResult(
                results = null,
                errorMessage = error.message ?: error.javaClass.simpleName
            )
        }
    }
}

private fun nowTimeLabel(): String {
    val calendar = Calendar.getInstance()
    return "%02d:%02d:%02d".format(
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND)
    )
}

private fun startAirkissReceiver(
    cancelled: java.util.concurrent.atomic.AtomicBoolean,
    onReceived: (String) -> Unit,
    onStatus: (String) -> Unit,
    onFinished: () -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())
    Thread {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(24333)
            socket.soTimeout = 60_000
            val buffer = ByteArray(15_000)
            val packet = DatagramPacket(buffer, buffer.size)
            while (!cancelled.get()) {
                socket.receive(packet)
                val hex = HexStringUtil.byteArrayToHex(packet.data.copyOf(packet.length))
                if (hex.isNotBlank()) {
                    mainHandler.post {
                        onReceived(hex)
                        onFinished()
                    }
                    return@Thread
                }
            }
        } catch (error: Exception) {
            if (!cancelled.get()) {
                Log.e("Airkiss", "receive failed", error)
                mainHandler.post {
                    onStatus("Airkiss 等待回包失败：${error.message ?: "未知错误"}")
                    onFinished()
                }
            }
        } finally {
            runCatching { socket?.close() }
        }
    }.apply {
        name = "airkiss-recv"
        start()
    }
}

private fun intToIpv4(value: Int): String {
    return listOf(
        value and 0xFF,
        value shr 8 and 0xFF,
        value shr 16 and 0xFF,
        value shr 24 and 0xFF
    ).joinToString(".")
}

private fun exportLogs(context: Context, logs: List<String>) {
    val content = buildString {
        appendLine("Noo-Psyche 调试日志")
        appendLine()
        logs.forEach { appendLine(it) }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Noo-Psyche Debug Logs")
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "导出日志"))
}

private object WifiApConnector {
    private const val TAG = "WifiApConnector"
    private var currentCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect(
        context: Context,
        ssid: String,
        password: String,
        onStatus: (String) -> Unit,
        onConnected: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectModern(context, ssid, password, onStatus, onConnected, onError)
        } else {
            connectLegacy(context, ssid, password, onStatus, onConnected, onError)
        }
    }

    private fun connectModern(
        context: Context,
        ssid: String,
        password: String,
        onStatus: (String) -> Unit,
        onConnected: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            onError("未获取到系统网络服务。")
            return
        }
        currentCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()
        postToMain { onStatus("正在连接设备热点 $ssid") }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                postToMain { onStatus("已接入设备热点 $ssid") }
                mainHandler.postDelayed(
                    { postToMain { onConnected(readCurrentGatewayIp(context)) } },
                    800L
                )
            }

            override fun onUnavailable() {
                postToMain { onError("未能连接到设备热点 $ssid") }
            }

            override fun onLost(network: Network) {
                postToMain { onStatus("设备热点连接已断开") }
            }
        }
        currentCallback = callback
        try {
            connectivityManager.requestNetwork(request, callback)
        } catch (error: SecurityException) {
            Log.e(TAG, "requestNetwork failed for ssid=$ssid", error)
            postToMain { onError("缺少热点连接权限，无法连接设备热点。") }
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "requestNetwork invalid arguments for ssid=$ssid", error)
            postToMain { onError("设备热点连接参数无效。") }
        }
    }

    @Suppress("DEPRECATION")
    private fun connectLegacy(
        context: Context,
        ssid: String,
        password: String,
        onStatus: (String) -> Unit,
        onConnected: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            onError("未获取到 Wi-Fi 服务。")
            return
        }
        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }
        val networkId = wifiManager.addNetwork(config)
        if (networkId < 0) {
            onError("设备热点连接失败，请检查热点是否可用。")
            return
        }
        try {
            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
            postToMain { onStatus("正在连接设备热点 $ssid") }
            mainHandler.postDelayed(
                { postToMain { onConnected(readCurrentGatewayIp(context)) } },
                800L
            )
        } catch (error: SecurityException) {
            Log.e(TAG, "legacy connect failed for ssid=$ssid", error)
            postToMain { onError("缺少 Wi-Fi 控制权限，无法连接设备热点。") }
        }
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}

