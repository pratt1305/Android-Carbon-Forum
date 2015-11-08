package com.lincanbin.carbonforum.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.lincanbin.carbonforum.LoginActivity;
import com.lincanbin.carbonforum.application.CarbonForumApplication;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpUtil {
    private static String charset = "utf-8";
    private Integer connectTimeout = null;
    private Integer socketTimeout = null;
    private String proxyHost = null;
    private Integer proxyPort = null;

    // get方法访问服务器，返回json对象
    public static JSONObject getRequest(Context context, String url, Boolean enableSession, Boolean loginRequired) {
        try {
            URL localURL = new URL(url);

            URLConnection connection = localURL.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

            httpURLConnection.setRequestProperty("Accept-Charset", charset);
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String cookie = getCookie(context);
            if(enableSession && cookie != null){
                httpURLConnection.setRequestProperty("Cookie", cookie);
            }
            InputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader reader = null;
            StringBuilder resultBuffer = new StringBuilder();
            String tempLine = null;

            switch (httpURLConnection.getResponseCode()){
                case 200:
                case 301:
                case 302:
                case 404:
                    break;
                case 401:
                    context.getSharedPreferences("UserInfo",Activity.MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                    break;
                case 500:
                    Log.v("Post Result","Code 500");
                    return null;
                default:
                    throw new Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
            }
            if(enableSession) {
                saveCookie(context, httpURLConnection);
            }
            try {
                inputStream = httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);

                while ((tempLine = reader.readLine()) != null) {
                    resultBuffer.append(tempLine);
                }

                String getResult = resultBuffer.toString();
                Log.v("Get URL : ", url);
                Log.v("Get Result",getResult);
                return JSONUtil.json2Object(getResult);
            }  catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (reader != null) {
                    reader.close();
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // post方法访问服务器，返回json对象
    public static JSONObject postRequest(Context context, String url, Map<String, String> parameterMap, Boolean enableSession, Boolean loginRequired) {
        try{
            if(loginRequired){
                if(parameterMap==null) {
                    parameterMap = new HashMap<>();
                }
                //SharedPreferences mySharedPreferences= context.getSharedPreferences("UserInfo", Activity.MODE_PRIVATE);
                parameterMap.put("AuthUserID", CarbonForumApplication.userInfo.getString("UserID", ""));
                parameterMap.put("AuthUserExpirationTime", CarbonForumApplication.userInfo.getString("UserExpirationTime", ""));
                parameterMap.put("AuthUserCode", CarbonForumApplication.userInfo.getString("UserCode", ""));
            }
           /* Translate parameter map to parameter date string */
            StringBuilder parameterBuffer = new StringBuilder();
            if (parameterMap != null) {
                Iterator iterator = parameterMap.keySet().iterator();
                String key = null;
                String value = null;
                while (iterator.hasNext()) {
                    key = (String) iterator.next();
                    if (parameterMap.get(key) != null) {
                        value = URLEncoder.encode(parameterMap.get(key), "UTF-8");
                    } else {
                        value = "";
                    }

                    parameterBuffer.append(key).append("=").append(value);
                    if (iterator.hasNext()) {
                        parameterBuffer.append("&");
                    }
                }
            }

            Log.v("POST URL : ", url);
            Log.v("POST parameter : ", parameterBuffer.toString());

            URL localURL = new URL(url);

            URLConnection connection = localURL.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Accept-Charset", charset);
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(parameterBuffer.length()));

            String cookie = getCookie(context);
            if(enableSession && cookie != null){
                httpURLConnection.setRequestProperty("Cookie", cookie);
            }
            OutputStream outputStream = null;
            OutputStreamWriter outputStreamWriter = null;
            InputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader reader = null;
            StringBuilder resultBuffer = new StringBuilder();
            String tempLine = null;

            try {
                outputStream = httpURLConnection.getOutputStream();
                outputStreamWriter = new OutputStreamWriter(outputStream);

                outputStreamWriter.write(parameterBuffer.toString());
                outputStreamWriter.flush();
                switch (httpURLConnection.getResponseCode()){
                    case 200:
                    case 301:
                    case 302:
                    case 404:
                        break;
                    case 401:
                        CarbonForumApplication.userInfo.edit().clear().apply();
                        Intent intent = new Intent(context, LoginActivity.class);
                        context.startActivity(intent);
                        break;
                    case 500:
                        Log.v("Post Result","Code 500");
                        return null;
                    default:
                        throw new Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
                }
                if(enableSession) {
                    saveCookie(context, httpURLConnection);
                }
                inputStream = httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);

                while ((tempLine = reader.readLine()) != null) {
                    resultBuffer.append(tempLine);
                }

                String postResult = resultBuffer.toString();
                Log.v("Post Result",postResult);
                JSONTokener jsonParser = new JSONTokener(postResult);
                return (JSONObject) jsonParser.nextValue();
            } catch (Exception e) {
                Log.v("No Network", "No Network");
                e.printStackTrace();
                return null;
            } finally {
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

                if (reader != null) {
                    reader.close();
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //获取之前保存的Cookie
    public static String getCookie(Context context){
        SharedPreferences mySharedPreferences= context.getSharedPreferences("Session",
                Activity.MODE_PRIVATE);
        try{
            return  mySharedPreferences.getString("Cookie", "");
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }

    }
    //保存Cookie
    public static Boolean saveCookie(Context context, URLConnection connection){
        //获取Cookie
        String headerName=null;
        for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
            if (headerName.equals("Set-Cookie")) {
                String cookie = connection.getHeaderField(i);
                //将Cookie保存起来
                SharedPreferences mySharedPreferences = context.getSharedPreferences("Session",
                        Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putString("Cookie", cookie);
                editor.apply();
                return true;
            }
        }
        return false;
    }

    private URLConnection openConnection(URL localURL) throws IOException {
        URLConnection connection;
        if (proxyHost != null && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            connection = localURL.openConnection(proxy);
        } else {
            connection = localURL.openConnection();
        }
        return connection;
    }

    private void renderRequest(URLConnection connection) {

        if (connectTimeout != null) {
            connection.setConnectTimeout(connectTimeout);
        }

        if (socketTimeout != null) {
            connection.setReadTimeout(socketTimeout);
        }

    }

    /*
     * Getter & Setter
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        HttpUtil.charset = charset;
    }
}