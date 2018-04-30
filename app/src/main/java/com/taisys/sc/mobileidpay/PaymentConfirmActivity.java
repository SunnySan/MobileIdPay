package com.taisys.sc.mobileidpay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

public class PaymentConfirmActivity extends AppCompatActivity {
    private static final String TAG = PaymentConfirmActivity.class.getSimpleName();

    private String goodsId = "";
    private TextView txtName, txtDescription, txtPrice;
    private ImageView imgGoodsImage;
    private Goods goods = null;

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

    /**
     * Searches the barcode by making http call
     * Request was made using Volley network library but the library is
     * not suggested in production, consider using Retrofit
     */
    private void searchBarcode(String barcode) {
        Log.e(TAG, "Sunny: search goods");
        // making volley's json request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                barcode, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "Sunny: Goods response: " + response.toString());

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
                String s = "";
                s = getString(R.string.txtGoodsName);
                txtName.setText(s + "\n" + goods.getName());
                s = getString(R.string.txtGoodsPrice);
                txtPrice.setText(s + "\n" + "$" + goods.getPrice());
                s = getString(R.string.txtGoodsDescription);
                txtDescription.setText(s + "\n" + goods.getDescription());
                /*
                txtDirector.setText(goods.getDescription());
                txtDuration.setText(goods.getPrice());
                */
                //GlideApp.with(this).load(goods.getPicture()).into(imgGoodsImage);
                Log.e(TAG, "image url: " + goods.getPicture());
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

}
