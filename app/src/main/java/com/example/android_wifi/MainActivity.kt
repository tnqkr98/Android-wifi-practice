package com.example.android_wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

// android 8.0 - 8.1 : getScanResults 에 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE 중 하나의 권한 필요
// android 9.0 : startScan() 을 위해선 ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION 중 하나, CHANGE_WIFI_STATE 필요
// android 10 : ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE 필수 (+ 설정 - 위치 활성화)
// startScan 사용제한 : 포그라운드 앱 - 2분 간격 4회.  백그라운드앱 - 30분 간격 1회
// wifi Direct : 직접 연결

class MainActivity : AppCompatActivity() {

    lateinit var wifiManager:WifiManager
    val wifiScanReceiver = object:BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
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
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiScan()
    }

    private fun wifiScan(){
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if(!success){
            Log.d("res","scanFailure1")
            scanFailure()
        }
    }

    private fun scanSuccess(){
        val results = wifiManager.scanResults
        for(res in results){
            Log.d("res","results + $res")
        }
        // 새 스캔 결과 목록
    }

    private fun scanFailure() {
        Log.d("res","scanFailure")
        val results = wifiManager.scanResults
        // 이전 스캔 결과 목록
    }
}