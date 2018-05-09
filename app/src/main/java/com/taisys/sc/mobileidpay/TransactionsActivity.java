package com.taisys.sc.mobileidpay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionsActivity extends AppCompatActivity {
    private static final String TAG = TransactionsActivity.class.getSimpleName();

    private ProgressDialog pg = null;
    private Context myContext = null;

    private ListView lv;
    private List<Map<String, Object>> data;

    private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    private Map<String, Object> map;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    startActivity(new Intent(TransactionsActivity.this, MainActivity.class));
                    finish();
                    return true;
                case R.id.navigation_myid:
                    startActivity(new Intent(TransactionsActivity.this, MyIdActivity.class));
                    finish();
                    return true;
                case R.id.navigation_transactions:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.transactionNavigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_transactions);

        myContext = this;
        lv = (ListView)findViewById(R.id.lvTransactionList);
        getData();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(TransactionsActivity.this, MainActivity.class));
        this.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(TransactionsActivity.this, MainActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
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

    private List<Map<String, Object>> getData()
    {
        String iccid = utility.getMySetting(myContext, "iccid");
        if (iccid==null || iccid.length()==0){
            utility.showMessage(myContext,getString(R.string.msgUnableToGetIccid));
            return null;
        }

        //資料都有了，將資料送給 server
        showWaiting(getString(R.string.pleaseWait), getString(R.string.msgDataUpdateInProgress));

        // 建立請求物件，設定網址
        String url = "http://cms.gslssd.com/MobileIdPayServer/ajaxGetTransactionHistory.jsp?";
        url += "iccid=" + iccid;

        // making volley's json request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        disWaiting();
                        Log.d(TAG, "Sunny: server response: " + response.toString());

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
                                JSONArray jaRecords = response.getJSONArray("records");
                                int iSize = jaRecords.length();
                                for (int i = 0; i < iSize; i++)
                                {
                                    JSONObject jItem = jaRecords.getJSONObject(i);
                                    map = new HashMap<String, Object>();
                                    map.put("img", jItem.getString("Goods_Picture_URL"));
                                    map.put("title", jItem.getString("Goods_Name"));
                                    map.put("info", "$" + jItem.getString("Goods_Price") + ", " + jItem.getString("Create_Date"));
                                    list.add(map);
                                }
                                data = list;
                                MyAdapter adapter = new MyAdapter(myContext);
                                lv.setAdapter(adapter);
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
        return null;
    }

    static class ViewHolder
    {
        public ImageView img;
        public TextView title;
        public TextView info;
    }

    public class MyAdapter extends BaseAdapter
    {
        private LayoutInflater mInflater = null;
        private MyAdapter(Context context)
        {
            //根据context上下文加载布局，这里的是Demo17Activity本身，即this
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            //How many items are in the data set represented by this Adapter.
            //在此适配器中所代表的数据集中的条目数
            return data.size();
        }
        @Override
        public Object getItem(int position) {
            // Get the data item associated with the specified position in the data set.
            //获取数据集中与指定索引对应的数据项
            return data.get(position);
        }
        @Override
        public long getItemId(int position) {
            //Get the row id associated with the specified position in the list.
            //获取在列表中与指定索引对应的行id
            return position;
        }

        //Get a View that displays the data at the specified position in the data set.
        //获取一个在数据集中指定索引的视图来显示数据
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            //如果缓存convertView为空，则需要创建View
            if(convertView == null)
            {
                holder = new ViewHolder();
                //根据自定义的Item布局加载布局
                convertView = mInflater.inflate(R.layout.report_list_item, null);
                holder.img = (ImageView)convertView.findViewById(R.id.imgReportListItemImage);
                holder.title = (TextView)convertView.findViewById(R.id.textReportListItemTitle);
                holder.info = (TextView)convertView.findViewById(R.id.textReportListItemInfo);
                //将设置好的布局保存到缓存中，并将其设置在Tag里，以便后面方便取出Tag
                convertView.setTag(holder);
            }else
            {
                holder = (ViewHolder)convertView.getTag();
            }
            String img_url = (String) data.get(position).get("img");
            if (!img_url.equals("")){
                Picasso.with(myContext).load(img_url).into(holder.img);
            } else {
                //todo - implement a default image in case img_url is indeed empty
                Picasso.with(myContext).load(R.mipmap.ic_launcher).into(holder.img);
            }
            //holder.img.setImageResource((Integer) data.get(position).get("img"));
            holder.title.setText((String)data.get(position).get("title"));
            holder.info.setText((String)data.get(position).get("info"));

            return convertView;
        }
    }


}
