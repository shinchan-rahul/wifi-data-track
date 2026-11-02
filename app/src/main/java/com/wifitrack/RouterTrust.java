package com.wifitrack;
import android.content.*;import android.util.Base64;import java.security.*;import java.security.cert.*;import javax.net.ssl.*;
/** Optional explicit trust in one user-imported certificate, only for the fixed router. */
public final class RouterTrust {
 private static volatile byte[] pinned;
 public static void load(Context c){String p=c.getSharedPreferences("router-trust",Context.MODE_PRIVATE).getString("der",null);pinned=p==null?null:Base64.decode(p,Base64.NO_WRAP);}
 public static void save(Context c,X509Certificate cert)throws Exception {byte[] der=cert.getEncoded();c.getSharedPreferences("router-trust",Context.MODE_PRIVATE).edit().putString("der",Base64.encodeToString(der,Base64.NO_WRAP)).commit();pinned=der;}
 public static String fingerprint(X509Certificate c)throws Exception {byte[] hash=MessageDigest.getInstance("SHA-256").digest(c.getEncoded());StringBuilder s=new StringBuilder();for(byte b:hash){if(s.length()>0)s.append(':');s.append(String.format("%02X",b&255));}return s.toString();}
 public static boolean matches(byte[] der){byte[] p=pinned;return p!=null&&der!=null&&MessageDigest.isEqual(p,der);}
 public static boolean pinned(){return pinned!=null;}
 public static void configure(HttpsURLConnection c)throws Exception {if(!"192.168.1.1".equals(c.getURL().getHost())||!"https".equals(c.getURL().getProtocol()))throw new SecurityException("Unexpected router URL");if(!pinned())return;
  SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(null,new TrustManager[]{new X509TrustManager(){public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}public void checkClientTrusted(X509Certificate[] chain,String type)throws CertificateException{throw new CertificateException("Client trust unsupported");}public void checkServerTrusted(X509Certificate[] chain,String type)throws CertificateException{if(chain==null||chain.length==0||!matches(chain[0].getEncoded()))throw new CertificateException("Router certificate changed");}}},new SecureRandom());
  c.setSSLSocketFactory(ctx.getSocketFactory());c.setHostnameVerifier((host,session)->{try{return "192.168.1.1".equals(host)&&matches(session.getPeerCertificates()[0].getEncoded());}catch(Exception e){return false;}});
 }
 private RouterTrust(){}
}
