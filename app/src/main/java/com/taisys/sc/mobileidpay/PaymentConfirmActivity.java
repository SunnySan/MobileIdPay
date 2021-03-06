package com.taisys.sc.mobileidpay;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;
import com.taisys.oti.Card;

import org.json.JSONObject;

public class PaymentConfirmActivity extends AppCompatActivity {
    private static final String TAG = PaymentConfirmActivity.class.getSimpleName();
    private static final int PUBLIC_KEY_FILE = 0x02A1;
    private static final int PRIVATE_KEY_FILE = 0x03A1;

    private static final int REQUEST_CODE = 1000;

    private Card mCard = new Card();
    private ProgressDialog pg = null;
    private Context myContext = null;

    private String goodsId = "";
    private TextView txtName, txtDescription, txtPrice;
    private ImageView imgGoodsImage;
    private Goods goods = null;

    private String sTransactionID = "";
    private String sPublicKey = "";
    private String sRsaSignature = "";
    private String sLocation = "";

    private FusedLocationProviderClient mFusedLocationClient = null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                        getMyLocation();
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    }
                }
            }

        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_confirm);

        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){
        }

        txtName = findViewById(R.id.pcGoodsName);
        txtPrice = findViewById(R.id.pcGoodsPrice);
        txtDescription = findViewById(R.id.pcGoodsDescription);
        imgGoodsImage = findViewById(R.id.pcGoodsImage);

        String barcode = getIntent().getStringExtra("code");

        // close the activity in case of empty barcode
        if (TextUtils.isEmpty(barcode)) {
            Toast.makeText(getApplicationContext(), getString(R.string.msgBarcodeIsEmpty), Toast.LENGTH_LONG).show();
            finish();
        }

        sTransactionID = utility.generateRequestId();
        myContext = this;
        setOnClickListener();
        // search the barcode
        searchBarcode(barcode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mCard!=null){
            mCard.CloseSEService();
        }
        super.onDestroy();
    }

    private void showWaiting(final String title, final String msg) {
        disWaiting();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pg = new ProgressDialog(myContext);
                // }
                pg.setIndeterminate(true);
                pg.setCancelable(false);
                pg.setCanceledOnTouchOutside(false);
                pg.setTitle(title);
                pg.setMessage(msg);
                pg.show();
            }
        });
    }

    private void disWaiting() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pg != null && pg.isShowing()) {
                    pg.dismiss();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    private void setOnClickListener(){
        Button b1 = (Button) findViewById(R.id.btnPcConfirm);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //顯示Progress對話視窗
                // showWaiting(getString(R.string.pleaseWait), getString(R.string.msgCheckCardAvailability));
                showWaiting(getString(R.string.pleaseWait), getString(R.string.msgCreatingSignature));
                getMyLocation();
            }
        });

        Button b2 = (Button) findViewById(R.id.btnPcCancel);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                //sendAuthenticationResultToServer(false);
            }
        });
    }

    /**
     * Searches the barcode by making http call
     * Request was made using Volley network library but the library is
     * not suggested in production, consider using Retrofit
     */
    private void searchBarcode(String barcode) {
        Log.i(TAG, "Sunny: search goods");
        // making volley's json request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                barcode, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Sunny: Goods response: " + response.toString());

                        // check for success status
                        if (response.toString().indexOf("00000")>0) {
                            // received movie response
                            showGoodsInfo(response);
                        } else {    //error
                            // no movie found
                            showNoGoods();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
                showNoGoods();
            }
        });

        MyApplication.getInstance().addToRequestQueue(jsonObjReq);
    }

    private void showNoGoods(){
        Toast.makeText(getApplicationContext(), getString(R.string.msgUnableToFindGoods), Toast.LENGTH_SHORT).show();
    }

    /**
     * Rendering movie details on the ticket
     */
    private void showGoodsInfo(JSONObject response) {
        try {

            // converting json to movie object
            goods = new Gson().fromJson(response.toString(), Goods.class);

            if (goods != null) {
                goodsId = goods.getId();
                txtName.setText(goods.getName());
                txtPrice.setText("$" + goods.getPrice());
                txtDescription.setText(goods.getDescription());
                Log.d(TAG, "goodsId: " + goodsId);
                Log.d(TAG, "image url: " + goods.getPicture());
                Picasso.with(this).load(goods.getPicture()).placeholder(R.mipmap.ic_launcher).into(imgGoodsImage);
            } else {
                // movie not found
                showNoGoods();
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JSON Exception: " + e.getMessage());
            showNoGoods();
            Toast.makeText(getApplicationContext(), "Error occurred. Check your LogCat for full report", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // exception
            //showNoGoods();
            Toast.makeText(getApplicationContext(), "Error occurred. Check your LogCat for full report", Toast.LENGTH_SHORT).show();
        }
    }

    private class Goods {
        String id;
        String name;
        String description;
        String picture;
        String price;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getPicture() {
            return picture;
        }

        public String getPrice() {
            return price;
        }

    }

    private void getMyLocation(){
        try{
            if ( Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission( myContext, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission( myContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PaymentConfirmActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                utility.showMessage(myContext, getString(R.string.msgPleaseGrantLocationPermission));
                return  ;
            }else{
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(myContext);
            }
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                String x = String.valueOf(location.getLongitude());
                                String y = String.valueOf(location.getLatitude());
                                Log.d(TAG, "Sunny: Latitude=" + y);
                                Log.d(TAG, "Sunny: Longitude=" + x);
                                x = utility.byte2Hex(x.getBytes());
                                y = utility.byte2Hex(y.getBytes());
                                sLocation = "ff" + y + "ff" + x;
                                openSimCard();
                            }
                        }
                    });
        }catch (Exception e) {
            Toast.makeText(getApplicationContext(), getString(R.string.msgUnableToGetYourLocation), Toast.LENGTH_SHORT).show();
            openSimCard();
        }
    }

    private void openSimCard(){
        mCard.OpenSEService(myContext, "A000000018506373697A672D63617264",
                new Card.SCSupported() {

                    @Override
                    public void isSupported(boolean success) {
                        if (success) {
                            //手機支援OTI
                            getCardInfo();
                        } else {
                            disWaiting();
                            utility.showMessage(myContext, getString(R.string.msgDoesntSupportOti));
                        }
                    }
                });
    }

    private void getCardInfo(){
        //顯示Progress對話視窗
        //utility.showToast(myContext, getString(R.string.msgReadCardInfo));
        //showWaiting(getString(R.string.pleaseWait), getString(R.string.msgReadCardInfo));
        Log.d(TAG, "Get Card Info");
        String res[] = mCard.GetCardInfo();
        String iccid = "";
        String s = "";
        int i = 0;
        int j = 0;

        //disWaiting();
        if (res != null && res[0].equals(Card.RES_OK)) {
            /*
                            res[1] 的結構如下：
                            假?拿到回复信息?：040001C3D908123456789012345601010412000100
                            其中   040001C3D9           LV?构 04?度，0001C3D9文件系?剩余空?大小，0x0001C3D9 = 115673 byte；
                            081234567890123456   LV?构 08?度，081234567890123456?卡?；
                            0101                 LV?构 01?度，01卡片版本?；
                            0412000100           LV?构 04?度，12000100 Cos版本?；
                         */
            s = res[1].substring(0, 2);
            i = Integer.parseInt(s);
            s = res[1].substring((i+1)*2, (i+1)*2 + 2);
            //utility.showMessage(myContext, s);
            j = Integer.parseInt(s);
            iccid = res[1].substring((i+1)*2+2, (i+1)*2+2 + j*2);
            //utility.showMessage(myContext, s);
            //i = s.length();
            //utility.showMessage(myContext, String.valueOf(i));
            utility.setMySetting(myContext, "iccid", iccid);
            verifyPinCode();
        } else {
            disWaiting();
            utility.showMessage(myContext, getString(R.string.msgUnableToGetIccid));
        }

    }

    private void verifyPinCode(){
        Log.d(TAG, "Verify PIN code");
        EditText editTextPinCode = (EditText) findViewById(R.id.pcEnterPinCode);
        String pinCode = editTextPinCode.getText().toString();
        if (pinCode.length()==0){
            disWaiting();
            utility.showMessage(myContext,getString(R.string.msgPleaseEnterPinCode));
            return;
        }
        //utility.showToast(myContext, getString(R.string.msgVerifyPinCode));
        //showWaiting(getString(R.string.pleaseWait), getString(R.string.msgVerifyPinCode));
        int pinId = 0x1;
        pinCode = utility.byte2Hex(pinCode.getBytes());
        String res = mCard.VerifyPIN(pinId, pinCode);
        //String  res = Card.RES_OK;
        /*
        if (mCard!=null){
            mCard.CloseSEService();
        }
        */
        if (res != null && res.equals(Card.RES_OK)) {
            Log.d(TAG, "PIN verification passed");
            generateRSASignature();
        } else {
            disWaiting();
            Log.d(TAG, "PIN code compared failed, user enter PIN= " + pinCode + ", response= " + res);
            //utility.showMessage(myContext, pinCode);
            utility.showMessage(myContext, getString(R.string.msgPinCodeIsIncorrect));
        }
    }

    //讀出PublicKey並產生 RSA 簽章
    private void generateRSASignature(){
        String res[] = null;

        //讀出 public key
        res = mCard.ReadFile(PUBLIC_KEY_FILE, 0x0, 264);
        if (res != null && res[0].equals(Card.RES_OK)) {
            sPublicKey = res[1];
            Log.d(TAG, "Sunny, existing public key=" + sPublicKey);
        }else if (res[0]!=null && res[0].equals("-15")){    //key不存在，建立key
            Log.d(TAG, "Sunny: no public key found, try to generate key pair...");
            String resString = "";
            //先執行CreateFile
            resString = mCard.CreateFile(PUBLIC_KEY_FILE, (byte)0x02, 0x0, (byte)0x0, (byte)0x0, (byte)0x0);
            if (resString == null || !resString.equals(Card.RES_OK)) {
                disWaiting();
                if (mCard!=null){
                    mCard.CloseSEService();
                }
                utility.showMessage(myContext, getString(R.string.msgUnableToCreatePublicKeyFile) + "error code=" + res[0]);
                Log.e(TAG, "Sunny: Create public key file fail!");
                return;
            }
            Log.i(TAG, "Sunny: Create public key file OK!");
            resString = mCard.CreateFile(PRIVATE_KEY_FILE, (byte)0x03, 0x0, (byte)0x0, (byte)0x0, (byte)0x0);
            if (resString == null || !resString.equals(Card.RES_OK)) {
                disWaiting();
                if (mCard!=null){
                    mCard.CloseSEService();
                }
                utility.showMessage(myContext, getString(R.string.msgUnableToCreatePrivateKeyFile) + "error code=" + res[0]);
                Log.e(TAG, "Sunny: Create public key file fail!");
                return;
            }
            Log.i(TAG, "Sunny: Create private key file OK!");

            //產生 RSA key pair
            resString = mCard.GenRSAKeyPair(Card.RSA_1024_BITS, PUBLIC_KEY_FILE, PRIVATE_KEY_FILE);
            if (resString != null && resString.equals(Card.RES_OK)) {
                Log.d(TAG, "Sunny: Generate key pair OK!");
                //重新讀出 PublicKey
                res = mCard.ReadFile(PUBLIC_KEY_FILE, 0x0, 264);
                if (res != null && res[0].equals(Card.RES_OK)) {
                    sPublicKey = res[1];
                    Log.d(TAG, "Sunny, new generated public key=" + sPublicKey);
                } else {
                    disWaiting();
                    if (mCard!=null){
                        mCard.CloseSEService();
                    }
                    utility.showMessage(myContext, getString(R.string.msgFailToReadPublicKey) + "error code=" + res[0]);
                    Log.e(TAG, "Sunny, unable to read new generated public key:" + res[0]);
                    return;
                }

            } else {
                disWaiting();
                if (mCard!=null){
                    mCard.CloseSEService();
                }
                Log.e(TAG, "Sunny, Gen key pair Failed! error code=" + resString);
                utility.showMessage(myContext, getString(R.string.msgUnableToGenerateRsaKeyPair) + ", error code=" + resString);
                return;
            }
        } else {
            disWaiting();
            if (mCard!=null){
                mCard.CloseSEService();
            }
            utility.showMessage(myContext, getString(R.string.msgFailToReadPublicKey) + "error code=" + res[0]);
            Log.e(TAG, "Sunny, failed to read public key:" + res[0]);
            return;
        }

        res = mCard.RSAPriKeyCalc(sTransactionID + sLocation, false, PRIVATE_KEY_FILE);
        if (mCard!=null){
            mCard.CloseSEService();
        }
        if (res != null && res[0].equals(Card.RES_OK)) {
            Log.d(TAG, "RSA signature successfully, signature hash=" + res[1]);
            sRsaSignature = res[1];
            sendAuthenticationResultToServer(true);
        }else{
            Log.d(TAG, "RSA signature failed！ error code=" + res[0]);
            disWaiting();
            utility.showMessage(myContext, getString(R.string.msgPinCodeIsIncorrect));
        }

    }

    private void sendAuthenticationResultToServer(boolean bPass){
        String iccid = utility.getMySetting(myContext, "iccid");
        if (iccid==null || iccid.length()==0){
            iccid = utility.generateRequestId();
            utility.setMySetting(myContext, "iccid", iccid);
        }

        String sResult = "";
        if (bPass) sResult="00000"; else sResult="99999";

        // 建立請求物件，設定網址
        String url = "http://cms.gslssd.com/MobileIdPayServer/ajaxAddNewTransaction.jsp?";
        url += "iccid=" + iccid;
        url += "&goodsId=" + goodsId;
        url += "&transactionId=" + sTransactionID + sLocation;
        url += "&publicKey=" + sPublicKey;
        url += "&signature=" + sRsaSignature;


        //資料都有了，將資料送給 server
        //showWaiting(getString(R.string.pleaseWait), getString(R.string.msgSendRegistrationRequest));
        Log.i(TAG, "Sunny: send signature to server");
        // making volley's json request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        disWaiting();
                        Log.i(TAG, "Sunny: server response: " + response.toString());

                        try{
                            String sResultCode = response.getString("resultCode");
                            String sResultText = response.getString("resultText");
                            if (sResultCode==null || sResultCode.length()<1 || !sResultCode.equals("00000")){
                                if (sResultText==null || sResultText.length()<1){
                                    utility.showMessage(myContext, getString(R.string.msgProcessFailed));
                                }else{
                                    utility.showMessage(myContext, sResultText);
                                }
                            }else{
                                AlertDialog.Builder dialog = new AlertDialog.Builder(myContext);
                                dialog.setTitle(R.string.msgSystemInfo)
                                        .setMessage(getString(R.string.msgProcessSucceeded))
                                        .setIcon(R.drawable.ic_launcher)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                finish();
                                            }
                                        })
                                        .show();
                            }
                        }catch (Exception e){
                            utility.showMessage(myContext, getString(R.string.msgUnableToParseServerResponseData));
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                disWaiting();
                Log.e(TAG, "Error: " + error.getMessage());
                utility.showMessage(myContext, getString(R.string.msgProcessFailed) + ": " + error.getMessage());
            }
        });

        MyApplication.getInstance().addToRequestQueue(jsonObjReq);
    }


}
