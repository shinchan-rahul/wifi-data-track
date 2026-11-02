package com.wifitrack;
import fi.iki.elonen.NanoHTTPD;import android.content.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.SecureRandom;import java.util.*;import org.json.*;
public final class DashboardServer extends NanoHTTPD {
 private final Context context;private final UsageStore store;private final DashboardAuth auth;
 private final Map<String,Long> sessions=new HashMap<>();private int failures;private long blockedUntil;
 public DashboardServer(Context c,UsageStore s){super("0.0.0.0",8080);context=c;store=s;auth=new DashboardAuth(c);}
 private Response response(Response.Status status,String mime,String text){Response r=newFixedLengthResponse(status,mime,text);r.addHeader("Cache-Control","no-store");r.addHeader("X-Content-Type-Options","nosniff");r.addHeader("X-Frame-Options","DENY");r.addHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'");return r;}
 private Response json(Response.Status status,JSONObject data){return response(status,"application/json",data.toString());}
 private Response error(Response.Status s,String msg){try{return json(s,new JSONObject().put("error",msg));}catch(JSONException e){return response(s,"text/plain",msg);}}
 @Override public synchronized Response serve(IHTTPSession request){try{
  String uri=request.getUri();String host=request.getHeaders().getOrDefault("host","");
  if(!validHost(host))return error(Response.Status.FORBIDDEN,"Invalid Host header");
  if(request.getMethod()==Method.GET&&(uri.equals("/")||uri.equals("/app.js")||uri.equals("/style.css"))){String name=uri.equals("/")?"index.html":uri.substring(1);String mime=name.endsWith("js")?"text/javascript":name.endsWith("css")?"text/css":"text/html";try(InputStream in=context.getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return response(Response.Status.OK,mime,out.toString("UTF-8"));}}
  if(uri.equals("/api/login")&&request.getMethod()==Method.POST){
   long now=System.currentTimeMillis();if(now<blockedUntil)return error(Response.Status.TOO_MANY_REQUESTS,"Too many attempts. Wait one minute.");
   if(!auth.configured())return error(Response.Status.FORBIDDEN,"Set dashboard password in the Android app first.");
   String origin=request.getHeaders().get("origin");if(origin!=null&&!origin.equals("http://"+host))return error(Response.Status.FORBIDDEN,"Invalid origin");
   int length=Integer.parseInt(request.getHeaders().getOrDefault("content-length","0"));if(length<1||length>2048||request.getHeaders().containsKey("transfer-encoding"))return error(Response.Status.BAD_REQUEST,"Invalid body length");
   Map<String,String> body=new HashMap<>();request.parseBody(body);JSONObject payload=new JSONObject(body.getOrDefault("postData","{}"));
   if(!auth.verify(payload.optString("password",null))){if(++failures>=5){blockedUntil=now+60000;failures=0;}return error(Response.Status.UNAUTHORIZED,"Wrong dashboard password");}
   failures=0;sessions.entrySet().removeIf(e->e.getValue()<now);if(sessions.size()>=32)sessions.clear();byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE);sessions.put(token,now+3600000);return json(Response.Status.OK,new JSONObject().put("token",token));
  }
  if(!uri.startsWith("/api/"))return error(Response.Status.NOT_FOUND,"Not found");
  String token=request.getHeaders().getOrDefault("authorization","").replaceFirst("^Bearer ","");Long expiry=sessions.get(token);if(expiry==null||expiry<System.currentTimeMillis())return error(Response.Status.UNAUTHORIZED,"Log in to dashboard");
  if(uri.equals("/api/logout")&&request.getMethod()==Method.POST){sessions.remove(token);return json(Response.Status.OK,new JSONObject().put("ok",true));}
  if(uri.equals("/api/report")&&request.getMethod()==Method.GET){JSONObject out=store.report();android.content.SharedPreferences p=context.getSharedPreferences("recorder",Context.MODE_PRIVATE);out.put("status",p.getString("status","Waiting for first reading"));out.put("statusAt",p.getLong("statusAt",0));return json(Response.Status.OK,out);}
  return error(Response.Status.NOT_FOUND,"Not found");
 }catch(Exception e){return error(Response.Status.BAD_REQUEST,"Request could not be processed");}}
 private boolean validHost(String host){String h=host.replaceFirst(":8080$","");if(h.equals("localhost")||h.equals("127.0.0.1"))return true;if(!h.matches("[0-9.]+"))return false;try{java.net.InetAddress ip=java.net.InetAddress.getByName(h);return ip.isSiteLocalAddress()&&java.net.NetworkInterface.getByInetAddress(ip)!=null;}catch(Exception e){return false;}}
}
