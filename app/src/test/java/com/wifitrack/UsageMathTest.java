package com.wifitrack;
import org.junit.Test;import static org.junit.Assert.*;import java.time.Instant;
public class UsageMathTest {
 private long t(String s){return Instant.parse(s).toEpochMilli();}
 @Test public void normalDifference(){var a=new UsageMath.Sample(t("2026-11-02T10:00:00Z"),1000,100);var b=new UsageMath.Sample(a.at()+120000,1600,150);var n=UsageMath.between(a,b);assertEquals(600,n.rx());assertEquals(50,n.tx());assertEquals("RECORDED",n.quality());}
 @Test public void resetExcluded(){var n=UsageMath.between(new UsageMath.Sample(1000,900,900),new UsageMath.Sample(2000,1,1));assertEquals("RESET_EXCLUDED",n.quality());assertEquals(0,n.rx());}
 @Test public void midnightNotInvented(){var a=new UsageMath.Sample(t("2026-11-02T18:29:00Z"),100,10);var b=new UsageMath.Sample(t("2026-11-02T18:31:00Z"),400,40);var n=UsageMath.between(a,b);assertEquals("UNALLOCATED_CROSS_DAY",n.quality());assertEquals("",n.date());assertEquals(300,n.rx());}
 @Test public void longGapFlagged(){assertEquals("GAP_TOTAL",UsageMath.between(new UsageMath.Sample(1000,1,1),new UsageMath.Sample(601000,100,100)).quality());}
 @Test(expected=IllegalArgumentException.class)public void clockReversal(){UsageMath.between(new UsageMath.Sample(2000,1,1),new UsageMath.Sample(1000,2,2));}
 @Test(expected=IllegalArgumentException.class)public void negativeCounterRejected(){new UsageMath.Sample(1000,-1,0);}
}
