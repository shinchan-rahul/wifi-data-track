package com.wifitrack;
import org.junit.Test;import static org.junit.Assert.*;
public class RouterParserTest {
 @Test public void selectsWan1WithoutDoubleCounting()throws Exception {var s=RouterParser.parse("{\"WAN\":[{\"WanName\":\"2_INTERNET_R_VID_1999\",\"EthernetBytesReceived\":999,\"EthernetBytesSent\":999},{\"WanName\":\"1_TR069_INTERNET_R_VID_0\",\"EthernetBytesReceived\":2010043585,\"EthernetBytesSent\":140102959}]}",1000);assertEquals(2010043585L,s.rx());assertEquals(140102959L,s.tx());}
 @Test(expected=org.json.JSONException.class)public void missingWanRejected()throws Exception{RouterParser.parse("{\"WAN\":[]}",1000);}
 @Test(expected=org.json.JSONException.class)public void malformedCounterRejected()throws Exception{RouterParser.parse("{\"WAN\":[{\"WanName\":\"1_TR069_INTERNET_R_VID_0\",\"EthernetBytesReceived\":1.5,\"EthernetBytesSent\":5}]}",1000);}
}
