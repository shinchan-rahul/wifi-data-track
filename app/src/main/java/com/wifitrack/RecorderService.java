package com.wifitrack;
import android.app.*;import android.content.*;import android.os.*;import java.util.concurrent.*;
public final class RecorderService extends Service {
 private ScheduledExecutorService executor;private DashboardServer server;private UsageStore store;
 private PowerManager.WakeLock wake;
 @Override public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("recorder","Usage recorder",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
  startForeground(10,new Notification.Builder(this,"recorder").setContentTitle("Wi-Fi usage recorder is running").setContentText("Router readings every 2 minutes · dashboard port 8080").setSmallIcon(android.R.drawable.stat_notify_sync).setContentIntent(open).build());
  if(!new DashboardAuth(this).configured()){status("Set a dashboard password before starting.");stopSelf();return;}
  RouterTrust.load(this);store=new UsageStore(this);server=new DashboardServer(this,store);
  try{server.start(10000,false);}catch(Exception e){status("Dashboard could not start; port 8080 may be busy.");stopSelf();return;}
  wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"WifiTrack:recorder");wake.acquire();
  executor=Executors.newSingleThreadScheduledExecutor();executor.scheduleWithFixedDelay(()->{try{store.add(new RouterClient().read());status("Reading saved");}catch(javax.net.ssl.SSLException e){status("Router TLS certificate not trusted or hostname mismatch. See README certificate setup.");}catch(Exception e){status(e.getMessage()==null?"Collection failed":e.getMessage());}},0,120,TimeUnit.SECONDS);
 }
 private void status(String text){getSharedPreferences("recorder",MODE_PRIVATE).edit().putString("status",text).putLong("statusAt",System.currentTimeMillis()).apply();}
 @Override public int onStartCommand(Intent i,int flags,int id){return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){if(executor!=null)executor.shutdownNow();if(server!=null)server.stop();if(wake!=null&&wake.isHeld())wake.release();if(store!=null)store.close();status("Recorder stopped; open app and tap Start to resume.");super.onDestroy();}
}
