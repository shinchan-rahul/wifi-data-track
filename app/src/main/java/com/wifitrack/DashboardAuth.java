package com.wifitrack;
import android.content.*;import android.util.Base64;import java.security.*;import javax.crypto.*;import javax.crypto.spec.*;
public final class DashboardAuth {
 private final android.content.SharedPreferences prefs;
 public DashboardAuth(Context c){prefs=c.getSharedPreferences("dashboard-auth",Context.MODE_PRIVATE);}
 public boolean configured(){return prefs.contains("hash");}
 public void setPassword(String password)throws Exception {if(password.length()<12||password.length()>128)throw new IllegalArgumentException("Use 12–128 characters");byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);prefs.edit().putString("salt",enc(salt)).putString("hash",enc(derive(password,salt))).commit();}
 public boolean verify(String password){try{return password!=null&&password.length()<=128&&MessageDigest.isEqual(derive(password,Base64.decode(prefs.getString("salt",""),Base64.NO_WRAP)),Base64.decode(prefs.getString("hash",""),Base64.NO_WRAP));}catch(Exception e){return false;}}
 private static byte[] derive(String p,byte[] salt)throws Exception {PBEKeySpec s=new PBEKeySpec(p.toCharArray(),salt,120000,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(s).getEncoded();}finally{s.clearPassword();}}
 private static String enc(byte[] a){return Base64.encodeToString(a,Base64.NO_WRAP);}
}
