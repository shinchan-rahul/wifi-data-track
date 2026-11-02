package com.wifitrack;
import android.webkit.CookieManager;import javax.net.ssl.HttpsURLConnection;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;
public final class RouterClient {
 public static final String BASE="https://192.168.1.1";
 public UsageMath.Sample read()throws Exception {
  URL url=new URL(BASE+"/statistics_status_web_app.cgi");HttpsURLConnection c=(HttpsURLConnection)url.openConnection();
  RouterTrust.configure(c);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Referer",BASE+"/web_whw/");String cookie=CookieManager.getInstance().getCookie(url.toString());if(cookie!=null)c.setRequestProperty("Cookie",cookie);
  try{if(c.getResponseCode()!=200)throw new IOException("Router HTTP "+c.getResponseCode()+"; open in-app router login again.");
   byte[] bytes;try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>1048576)throw new IOException("Router response too large");}bytes=out.toByteArray();}if(bytes.length>1048576)throw new IOException("Router response too large");
   try{return RouterParser.parse(new String(bytes,StandardCharsets.UTF_8),System.currentTimeMillis());}catch(Exception e){throw new IOException("Router did not return valid WAN1 counters. Log in again; additional router authentication may be required.");}
  }finally{c.disconnect();}
 }
}
