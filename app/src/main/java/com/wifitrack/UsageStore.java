package com.wifitrack;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;
public final class UsageStore extends SQLiteOpenHelper {
 public UsageStore(Context c){super(c,"usage.db",null,1);}
 @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE samples(at INTEGER PRIMARY KEY,rx INTEGER NOT NULL,tx INTEGER NOT NULL)");db.execSQL("CREATE TABLE intervals(start INTEGER,end INTEGER PRIMARY KEY,rx INTEGER,tx INTEGER,quality TEXT,day TEXT)");}
 @Override public void onUpgrade(SQLiteDatabase db,int a,int b){throw new IllegalStateException("Migration required");}
 public synchronized void add(UsageMath.Sample s){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
  UsageMath.Sample last=null;try(Cursor c=db.rawQuery("SELECT at,rx,tx FROM samples ORDER BY at DESC LIMIT 1",null)){if(c.moveToFirst())last=new UsageMath.Sample(c.getLong(0),c.getLong(1),c.getLong(2));}
  if(last!=null){UsageMath.Interval n=UsageMath.between(last,s);ContentValues v=new ContentValues();v.put("start",n.start());v.put("end",n.end());v.put("rx",n.rx());v.put("tx",n.tx());v.put("quality",n.quality());v.put("day",n.date());db.insertOrThrow("intervals",null,v);}
  ContentValues v=new ContentValues();v.put("at",s.at());v.put("rx",s.rx());v.put("tx",s.tx());db.insertOrThrow("samples",null,v);db.setTransactionSuccessful();
 }finally{db.endTransaction();}}
 public synchronized JSONObject report()throws JSONException {JSONObject out=new JSONObject();JSONArray days=new JSONArray(),unallocated=new JSONArray();SQLiteDatabase db=getReadableDatabase();
  try(Cursor c=db.rawQuery("SELECT day,SUM(rx),SUM(tx),SUM(CASE WHEN quality='GAP_TOTAL' THEN 1 ELSE 0 END),SUM(CASE WHEN quality='RESET_EXCLUDED' THEN 1 ELSE 0 END),COUNT(*) FROM intervals WHERE day<>'' GROUP BY day ORDER BY day DESC",null)){while(c.moveToNext())days.put(new JSONObject().put("date",c.getString(0)).put("rx",c.getLong(1)).put("tx",c.getLong(2)).put("gaps",c.getInt(3)).put("resets",c.getInt(4)).put("intervals",c.getInt(5)));}
  try(Cursor c=db.rawQuery("SELECT start,end,rx,tx FROM intervals WHERE day='' ORDER BY end DESC",null)){while(c.moveToNext())unallocated.put(new JSONObject().put("start",c.getLong(0)).put("end",c.getLong(1)).put("rx",c.getLong(2)).put("tx",c.getLong(3)));}
  try(Cursor c=db.rawQuery("SELECT MIN(at),MAX(at),COUNT(*) FROM samples",null)){if(c.moveToFirst()){out.put("first",c.isNull(0)?0:c.getLong(0));out.put("last",c.isNull(1)?0:c.getLong(1));out.put("samples",c.getLong(2));}}
  return out.put("days",days).put("unallocated",unallocated).put("timezone","Asia/Kolkata").put("perDeviceSupported",false);
 }
}
