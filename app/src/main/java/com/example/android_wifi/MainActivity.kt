package com.example.android_wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import okhttp3.OkHttpClient
import java.net.Socket
import java.net.URL

// android 8.0 - 8.1 : getScanResults 에 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE 중 하나의 권한 필요
// android 9.0 : startScan() 을 위해선 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 중 하나, CHANGE_WIFI_STATE 필요
// android 10 : ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE 필수 (+ 설정 - 위치 활성화)
// startScan 사용제한 : 포그라운드 앱 - 2분 간격 4회.  백그라운드앱 - 30분 간격 1회
// wifi Direct : 직접 연결

// android 10 부터 보안 이슈로 wifi 기능 on/off 를 programmatically 하게 다룰 수 없게 됨 (9 이하는 가능)
// android p2p : wifi 를 매개로 두 기기가 p2p 연결   A <----wifi(AP)-----> B

// 최초 연결을 보장하기위해선 반드시 모든 네트워크가 끊기거나(또는 셀룰러 상태) 적어도 와이파이는 연결이 안된 상태에서 Suggestion을 실행하면, 먹히고 연결됨. (이후 부터는 자동으로 다시 연결)
// Suggestion 만 으로도 Connect 가 포함되는것. (즉 최초는 Suggestion). 그런데. 지금 네트워크가 특정 와이파이에 잘 연결되어있다면, suggestion 은 안먹힘.
// 문제는 앱이 최초 연결에 성공했다하더라도, 다른 와이파이에 연결된 것을 끊고 내 와이파이를 연결하는건 죽어도 안됨(무한로딩 or 연결된척;)
// 최초 이후는 ConnectivityMananger

class MainActivity : AppCompatActivity() {

    lateinit var wifiManager:WifiManager
    lateinit var connectivityManager:ConnectivityManager
    private val wifiScanReceiver = object:BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success:Boolean? = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,false)
            if(success!!){
                scanSuccess()
            }
            else{
                scanFailure()
                Log.d("res","scanFailure2")
            }
        }

        override fun peekService(myContext: Context?, service: Intent?): IBinder {
            return super.peekService(myContext, service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Settings.System.canWrite(applicationContext)


        requestPermissions(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION),101)

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val btn = findViewById<Button>(R.id.button)
        btn.setOnClickListener {
            wifiScan()
        }

        val btn2 = findViewById<Button>(R.id.button2)

        // Suggestion api 29 이상 부터
        btn2.setOnClickListener {

            // ONE R XUFKFW.OSC, BSSID : c0:84:7d:f3:a3:e6, capabilities : [WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS][WPS], 88888888
            // 제안(사용자 승인) -> 연결 ?
            val suggestion2 = WifiNetworkSuggestion.Builder()
                .setSsid("THETAYN12100323.OSC")
                .setWpa2Passphrase("12100323")
                .setIsAppInteractionRequired(true)
                .build()

            val suggestionList = listOf(suggestion2)

            wifiManager.removeNetworkSuggestions(suggestionList)

            //connectWifi()

            val suggestionReceiver = object : BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d("cupix network","wifi suggestion receive")
                    if(!intent?.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)){
                        return
                    }
                    //connectWifi()
                }
            }
            registerReceiver(suggestionReceiver,IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION))


            val status = wifiManager.addNetworkSuggestions(suggestionList)
            if(status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS){
                //error
                Log.d("cupix network","wifi suggestion error")
            }
            Log.d("cupix network","suggestion : success")
        }


        val btn3 = findViewById<Button>(R.id.button3)
        btn3.setOnClickListener {
            connectWifi()
        }

        val webView = findViewById<WebView>(R.id.web_view)
        webView.webViewClient = WebViewClient()
        val setting = webView.settings
        setting.javaScriptEnabled = true
        webView.loadUrl("https://naver.com")

        val btn4 = findViewById<Button>(R.id.button4)
        btn4.setOnClickListener {
            webView.loadUrl("https://naver.com")
        }
    }

    private fun wifiScan(){
        val success = wifiManager.startScan()
        if(!success){
            Log.d("res","scanFailure1")
            scanFailure()
        }
    }

    private fun scanSuccess(){
        val results = wifiManager.scanResults
        for(res in results){
            Log.d("res","SSID : ${res.SSID}, BSSID : ${res.BSSID}, capabilities : ${res.capabilities}")
        }
        // 새 스캔 결과 목록
    }

    private fun scanFailure() {
        Log.d("res","scanFailure")
        val results = wifiManager.scanResults
        // 이전 스캔 결과 목록
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun connectWifi(){
        Log.d("cupix network","${wifiManager.wifiState}")
        Log.d("cupix network","${wifiManager.isWifiEnabled}")

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid("THETAYN12100323.OSC")
            //.setBssid(MacAddress.fromString("C0:84:7D:F3:A3:E6"))
            .setWpa2Passphrase("12100323")
            //.setSsid("Cupix")
            //.setBssid(MacAddress.fromString("5a:86:94:40:5e:94"))
            //.setWpa2Passphrase("ScanRoom3D")
            .build()


        // [WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS][WPS]
        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        connectivityManager.requestNetwork(wifiRequest,object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("wifi network", "onAvailable $network")
                //var netInfo = connectivityManager.activeNetwork

                //var client = OkHttpClient()

                //val success = connectivityManager.bindProcessToNetwork(network)
                //network.bindSocket(Socket("http://192.168.1.1",80))
                //Log.d("cupix network","success : $success")
                //network.openConnection(URL("http://192.168.1.1:80"))
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                //Log.d("wifi network", "onLosing")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("wifi network", "onLost")
                connectivityManager.unregisterNetworkCallback(this)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                //Log.d("wifi network", "onUnavailable")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                //Log.d("wifi network", "onCapabilitiesChanged , network : $network, networkCapabilities : $networkCapabilities")
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                //Log.d("wifi network", "onLinkPropertiesChanged network : $network, linkProperties : $linkProperties")
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                super.onBlockedStatusChanged(network, blocked)
                //Log.d("wifi network", "onBlockedStatusChanged")
            }
        })
    }
}