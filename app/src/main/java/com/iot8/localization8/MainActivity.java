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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean isPermitted = false;
    boolean isWifiScan = false;
    boolean doneWifiScan = true;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    TextView ap1;
    TextView ap2;
    TextView ap3;
    TextView currentPosition;

    WifiManager wifiManager;
    List<ScanResult> scanResultList;
    ArrayList<String> arrayList;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                try {
                    getWifiInfo();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };

    // TODO: 비콘의 위치와 거리를 설정
    double[][] points = {{1.0, 1.0}, {10.0, 1.0}, {5.0, 10.0}};
    double[] distance = {0.0, 0.0, 0.0};


    private void getWifiInfo() throws InterruptedException {
        if(!doneWifiScan) { // wifiScan을 한 경우에만 getScanResult를 사용하도록 flag 변수 구현
            scanResultList = wifiManager.getScanResults();

            String str;
            //중복되지 않게 해당 num번째 배열에 AP정보들을 집어넣는다.
            for (int i = 1; i < scanResultList.size(); i++) {
                ScanResult result = scanResultList.get(i);
                // 화면의 TextView에 SSID와 BSSID를 이어붙여서 텍스트로 표시
                str = result.SSID;
                System.out.println(str + result.level);
                // TODO: 1m 기준일 때 신호세기, 얼마나 방해를 많이 받는지 설정
                double level = calculateDistance(result.level, -50, 3.5);
                if (result.SSID.equals("U+Net0DB8")) {
                    ap1.setText(String.valueOf(level) + ' ' + result.level);
                    distance[0] = level;
                }
                if (result.SSID.equals("SK_WiFiGIGA5CB8_2.4G")) {
                    ap2.setText(String.valueOf(level) + ' ' + result.level);
                    distance[1] = level;
                }
                if (result.SSID.equals("yesiamok")) {
                    ap3.setText(String.valueOf(level) + ' ' + result.level);
                    distance[2] = level;
                }
            }

            doneWifiScan = true;

            if (ap1.getText().toString().isEmpty() || ap2.getText().toString().isEmpty() || ap3.getText().toString().isEmpty()) {
                doneWifiScan = false;
                Thread.sleep(3000);
                scanWiFi();
            } else {
                try {
                    double[] result = trilaterate(points[0], distance[0], points[1], distance[1], points[2], distance[2]);
                    String resultText = "x: " + result[0] + " y: " + result[1];
                    currentPosition.setText(resultText);
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public static double calculateDistance(int rssi, int referenceSignalStrength, double pathLossExponent) {
        return Math.pow(10.0, ((rssi - referenceSignalStrength) / (-10.0 * pathLossExponent)));
    }

    public static double[] trilaterate(double[] point1, double distance1, double[] point2, double distance2, double[] point3, double distance3) {
        // Check for a degenerate case: all three points are collinear
        if (areCollinear(point1, point2, point3)) {
            throw new IllegalArgumentException("Trilateration is not possible. The three points are collinear.");
        }

        // Use an iterative optimization method to find the unknown point
        double[] unknownPoint = new double[]{0, 0}; // Initial guess

        for (int i = 0; i < 100; i++) {
            double[] residuals = computeResiduals(unknownPoint, point1, distance1, point2, distance2, point3, distance3);
            double[][] jacobian = computeJacobian(unknownPoint, point1, distance1, point2, distance2, point3, distance3);

            try {
                double[] delta = solveLinearSystem(jacobian, residuals);
                unknownPoint[0] -= delta[0];
                unknownPoint[1] -= delta[1];

                // Check for convergence
                if (Math.abs(delta[0]) < 1e-6 && Math.abs(delta[1]) < 1e-6) {
                    break;
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Singular matrix during optimization. Trilateration may not converge.");
            }
        }

        return unknownPoint;
    }

    private static boolean areCollinear(double[] point1, double[] point2, double[] point3) {
        // Check if the three points are collinear
        double area = 0.5 * (point1[0] * (point2[1] - point3[1]) + point2[0] * (point3[1] - point1[1]) + point3[0] * (point1[1] - point2[1]));
        return Math.abs(area) < 1e-10;
    }

    private static double[][] computeJacobian(double[] point, double[] point1, double distance1, double[] point2, double distance2, double[] point3, double distance3) {
        double[][] jacobian = new double[3][2];

        // Numerical differentiation to compute the Jacobian
        double epsilon = 1e-6;

        for (int i = 0; i < 2; i++) {
            double[] perturbedPoint = point.clone();
            perturbedPoint[i] += epsilon;

            double[] perturbedResiduals = computeResiduals(perturbedPoint, point1, distance1, point2, distance2, point3, distance3);

            for (int j = 0; j < 3; j++) {
                jacobian[j][i] = (perturbedResiduals[j] - computeResiduals(point, point1, distance1, point2, distance2, point3, distance3)[j]) / epsilon;
            }
        }

        return jacobian;
    }

    private static double[] solveLinearSystem(double[][] A, double[] b) {
        // Solve the linear system using Gauss elimination or another method
        double det = A[0][0] * A[1][1] - A[0][1] * A[1][0];

        if (Math.abs(det) < 1e-10) {
            throw new IllegalArgumentException("Singular matrix, cannot solve the system");
        }

        double[] solution = new double[2];
        solution[0] = (A[1][1] * b[0] - A[0][1] * b[1]) / det;
        solution[1] = (-A[1][0] * b[0] + A[0][0] * b[1]) / det;

        return solution;
    }

    private static double[] computeResiduals(double[] point, double[] point1, double distance1, double[] point2, double distance2, double[] point3, double distance3) {
        double[] residuals = new double[3];

        residuals[0] = Math.sqrt(Math.pow(point[0] - point1[0], 2) + Math.pow(point[1] - point1[1], 2)) - distance1;
        residuals[1] = Math.sqrt(Math.pow(point[0] - point2[0], 2) + Math.pow(point[1] - point2[1], 2)) - distance2;
        residuals[2] = Math.sqrt(Math.pow(point[0] - point3[0], 2) + Math.pow(point[1] - point3[1], 2)) - distance3;

        return residuals;
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
        currentPosition = (TextView)findViewById(R.id.currentPosition);

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
            scanWiFi();
        }
    }

    public void scanWiFi() {
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