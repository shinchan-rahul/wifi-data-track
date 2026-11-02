package com.wifitrack;
import org.json.*;
public final class RouterParser {
 public static UsageMath.Sample parse(String body,long at)throws JSONException {
  JSONArray wan=new JSONObject(body).getJSONArray("WAN");
  for(int i=0;i<wan.length();i++){JSONObject w=wan.getJSONObject(i);
   if("1_TR069_INTERNET_R_VID_0".equals(w.optString("WanName")))
    return new UsageMath.Sample(at,counter(w,"EthernetBytesReceived"),counter(w,"EthernetBytesSent"));
  }
  throw new JSONException("Expected WAN1 service not present; login may have expired.");
 }
 private static long counter(JSONObject w,String key)throws JSONException {Object v=w.get(key);String s=v.toString();if(!s.matches("[0-9]{1,18}"))throw new JSONException("Invalid counter: "+key);return Long.parseLong(s);}
}
