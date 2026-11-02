package com.wifitrack;
import java.time.*;
/** Counter differences only; never turn a cumulative baseline into today's usage. */
public final class UsageMath {
 public static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
 public record Sample(long at,long rx,long tx) { public Sample {if(at<0||rx<0||tx<0)throw new IllegalArgumentException("Negative counter/time");} }
 public record Interval(long start,long end,long rx,long tx,String quality,String date) {}
 public static String date(long at){return Instant.ofEpochMilli(at).atZone(ZONE).toLocalDate().toString();}
 public static Interval between(Sample a,Sample b){
  if(b.at<=a.at)throw new IllegalArgumentException("Reading time must increase");
  if(b.rx<a.rx||b.tx<a.tx)return new Interval(a.at,b.at,0,0,"RESET_EXCLUDED",date(b.at));
  boolean cross=!date(a.at).equals(date(b.at));
  String q=cross?"UNALLOCATED_CROSS_DAY":b.at-a.at>300000?"GAP_TOTAL":"RECORDED";
  return new Interval(a.at,b.at,b.rx-a.rx,b.tx-a.tx,q,cross?"":date(b.at));
 }
 private UsageMath(){}
}
