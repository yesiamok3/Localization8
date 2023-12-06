package com.iot8.localization8;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.math3.analysis.solvers.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    boolean isPermitted = false;
    boolean isWifiScan = false;
    boolean doneWifiScan = true;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    TextView ap1;
    TextView ap2;
    TextView ap3;

    WifiManager wifiManager;
    List<ScanResult> scanResultList;
    ArrayList<String> arrayList;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                getWifiInfo();
        }
    };


    private void getWifiInfo() {
        if(!doneWifiScan) { // wifiScan을 한 경우에만 getScanResult를 사용하도록 flag 변수 구현
            scanResultList = wifiManager.getScanResults();
            Set<ScanResult> scanResultSet = new HashSet<>(scanResultList);

            String str;
            //중복되지 않게 해당 num번째 배열에 AP정보들을 집어넣는다.
            for (int i = 1; i < scanResultList.size(); i++) {
                ScanResult result = scanResultList.get(i);
                // 화면의 TextView에 SSID와 BSSID를 이어붙여서 텍스트로 표시
                str = result.SSID;
                System.out.println(str + result.level);
                double level = calculateDistance(result.level, -50, 2.0);
                if (result.SSID.equals("swcse601")) {
                    ap1.setText(String.valueOf(level));
                }
                if (result.SSID.equals("Smart-CAU_2.4G")) {
                    ap2.setText(String.valueOf(level));
                }
                if (result.SSID.equals("옥재")) {
                    ap3.setText(String.valueOf(level));
                }
            }

            doneWifiScan = true;
        }
    }

    public static double calculateDistance(int rssi, int referenceSignalStrength, double pathLossExponent) {
        return Math.pow(10.0, ((rssi - referenceSignalStrength) / (-10.0 * pathLossExponent)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //초기 셋팅 과정
        requestRuntimePermission();

        ap1 = (TextView)findViewById(R.id.ap1);
        ap2 = (TextView)findViewById(R.id.ap2);
        ap3 = (TextView)findViewById(R.id.ap3);

        arrayList = new ArrayList<>();
        arrayList.add("AP1");
        arrayList.add("AP2");
        arrayList.add("AP3");

        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // wifi scan 결과 수신을 위한 BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // wifi scan 결과 수신용 BroadcastReceiver 등록 해제
        unregisterReceiver(mReceiver);
    }

    public void onClick(View view) {
        if(view.getId() == R.id.start) {
            // Start wifi scan 버튼이 눌렸을 때
            doneWifiScan = false;
            Toast.makeText(this, "WiFi scan start!!", Toast.LENGTH_LONG).show();

            if(isPermitted) {
                // wifi 스캔 시작
                wifiManager.startScan();
                isWifiScan = true;
            } else {
                Toast.makeText(getApplicationContext(),
                        "Location access 권한이 없습니다..", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestRuntimePermission() {
        //*******************************************************************
        // Runtime permission check
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            // ACCESS_FINE_LOCATION 권한이 있는 것
            isPermitted = true;
        }
        //*********************************************************************
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // ACCESS_FINE_LOCATION 권한을 얻음
                    isPermitted = true;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 location 요청 작업을 수행할 수 없다
                    // 적절히 대처한다
                    isPermitted = false;

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}