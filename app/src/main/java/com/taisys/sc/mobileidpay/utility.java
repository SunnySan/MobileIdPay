package com.taisys.sc.mobileidpay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class utility {
    public static String getMySetting(Context context, String keyName){
        // 建立SharedPreferences物件
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String valueString = sharedPreferences.getString(keyName, "");
        return valueString;
    }

    public static void setMySetting(Context context, String keyName, String value){
        // 建立SharedPreferences物件
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(keyName, value);
        editor.apply();
    }

    public static void showToast(Context context, String msg) {
        if (msg==null || msg.length()==0) return;
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static void showMessage(Context context, String msg){
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.msgSystemInfo)
                .setMessage(msg)
                .setIcon(R.drawable.ic_launcher)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //取得 byte array 每個 byte 的 16 進位碼
    public static String byte2Hex(byte[] b) {
        String result = "";
        for (int i=0 ; i<b.length ; i++)
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        return result;
    }

    //將 16 進位碼的字串轉為 byte array
    public static byte[] hex2Byte(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i=0 ; i<bytes.length ; i++)
            bytes[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
        return bytes;
    }

    /*********************************************************************************************************************/

//產生20碼的RequestId
    public static String generateRequestId(){
        //以【日期+時間+四位數隨機數】作為送給BSC API的 RequestId，例如【20110816-102153-6221】
        //java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss");
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        java.util.Date currentTime = new java.util.Date();//得到當前系統時間
        String txtRandom = String.valueOf(Math.round(Math.random()*10000));
        txtRandom = MakesUpZero(txtRandom, 4);	//不足4碼的話，將前面補0
        //String txtRequestId = formatter.format(currentTime) + "-" + txtRandom; //將日期時間格式化，加上一個隨機數，作為RequestId，格式是yyyyMMdd-HHmmss-xxxx
        String txtRequestId = formatter.format(currentTime) + txtRandom; //將日期時間格式化，加上一個隨機數，作為RequestId，格式是yyyyMMdd-HHmmss-xxxx

        return txtRequestId;
    }

/*********************************************************************************************************************/

/*********************************************************************************************************************/

    /**
     * 數字不足部份補零回傳
     * @param str 字串
     * @param lenSize 字串數字最大長度,不足的部份補零
     * @return 回傳補零後字串數字
     */
    public static String MakesUpZero(String str, int lenSize) {
        String zero = "0000000000";
        String returnValue = zero;

        returnValue = zero + str;

        return returnValue.substring(returnValue.length() - lenSize);

    }

/*********************************************************************************************************************/

}
