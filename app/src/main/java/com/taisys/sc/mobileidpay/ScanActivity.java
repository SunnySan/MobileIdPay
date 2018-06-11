package com.taisys.sc.mobileidpay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.List;

import info.androidhive.barcode.BarcodeReader;

public class ScanActivity extends AppCompatActivity implements BarcodeReader.BarcodeReaderListener {

    BarcodeReader barcodeReader;
    private Context myContext = null;
    private String sMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){
        }

        myContext = this;

        // get the barcode reader instance
        barcodeReader = (BarcodeReader) getSupportFragmentManager().findFragmentById(R.id.barcode_scanner);
    }

    @Override
    public void onScanned(Barcode barcode) {
        String s = barcode.displayValue;

        sMessage = s;
        runOnUiThread(new Runnable() {
            public void run() {
                final Toast toast = Toast.makeText(myContext, "scanned value: " + sMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        if (s.indexOf("ajaxGetMyGoods")<1) return;
        barcodeReader.pauseScanning();
        // playing barcode reader beep sound
        barcodeReader.playBeep();
        Log.d("Sunny", "Sunny code:" + barcode.displayValue);

        // ticket details activity by passing barcode
        Intent intent = new Intent(ScanActivity.this, PaymentConfirmActivity.class);
        intent.putExtra("code", barcode.displayValue);
        startActivity(intent);
        finish();
    }

    @Override
    public void onScannedMultiple(List<Barcode> list) {
        runOnUiThread(new Runnable() {
            public void run() {
                final Toast toast = Toast.makeText(myContext, "multiple list scanned", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {
        runOnUiThread(new Runnable() {
            public void run() {
                final Toast toast = Toast.makeText(myContext, "bitmap scanned", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    @Override
    public void onScanError(String s) {
        sMessage = s;
        runOnUiThread(new Runnable() {
            public void run() {
                final Toast toast = Toast.makeText(myContext, "Error occurred while scanning: " + sMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @Override
    public void onCameraPermissionDenied() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
