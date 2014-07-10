import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.RoundTripFlightInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;


public class Wrapper_gjsairxn001 implements QunarCrawler {
	public  QFHttpClient httpClient=null;
	public String cookies="";
	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "https://booking.xpressair.co.id/EAStart.aspx?New=1&Language=en&CurrencyOverride=IDR";
		BookingResult bookingResult = new BookingResult();
		
		try {
			Map<String,String> maps=this.getAddress();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String[] date=arg0.getDepDate().split("-");
			String deptdate=date[1]+"/"+date[2]+"/"+date[0];
			String[] arrdate=arg0.getDepDate().split("-");
			String backdate=arrdate[1]+"/"+arrdate[2]+"/"+arrdate[0];
			BookingInfo bookingInfo = new BookingInfo();
			bookingInfo.setAction(bookingUrlPre);
			bookingInfo.setMethod("post");
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("OutboundDate", deptdate);
			map.put("HomeboundDate", backdate);
			map.put("HoursFromGMT", "8");
		    map.put("MinutesFromGMT", "0");
		    map.put("rblDirection", "1");
		    map.put("ctl00$ContentPlaceHolder1$Curr", "IDR");
		    map.put("ctl00$ContentPlaceHolder1$Origin", maps.get(arg0.getDep()).toString());
		    map.put("ctl00$ContentPlaceHolder1$Destination", maps.get(arg0.getArr()).toString());
			bookingInfo.setInputs(map);		
			bookingResult.setData(bookingInfo);
			bookingResult.setRet(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return bookingResult;
	}

	@Override
	public String getHtml(FlightSearchParam arg0) {
		QFGetMethod get = null;	
		QFPostMethod post=null;
		try {
			httpClient = new QFHttpClient(arg0, false);
			httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			//获取“__VIEWSTATE”
			get = new QFGetMethod("https://booking.xpressair.co.id/EAStart.aspx?New=1&Language=en&CurrencyOverride=IDR");
			httpClient.executeMethod(get);
			Thread.sleep(3000);
			String getViewStatehtml=get.getResponseBodyAsString();
			String __VIEWSTATE="";
			String _TSM_HiddenField_="";
			String ClientState="";
			if(getViewStatehtml.contains("__VIEWSTATE")){
				String tsmtr=StringUtils.substringBetween(getViewStatehtml, "id=\"_TSM_HiddenField_\"", "/>");
				_TSM_HiddenField_=tsmtr.substring(tsmtr.indexOf("value=\"")+7, tsmtr.lastIndexOf("\""));
				String vsstr=StringUtils.substringBetween(getViewStatehtml, "id=\"__VIEWSTATE\"", "/>");
				__VIEWSTATE=vsstr.substring(vsstr.indexOf("value=\"")+7, vsstr.lastIndexOf("\""));
				ClientState= "-"+(Integer.parseInt(StringUtils.substringBetween(getViewStatehtml, "~","\""))+1);

			}
			String[] dedate=arg0.getDepDate().split("-");
			String deptdate=dedate[1]+"/"+dedate[2]+"/"+dedate[0];
			String[] arrdate=arg0.getDepDate().split("-");
			String backdate=arrdate[1]+"/"+arrdate[2]+"/"+arrdate[0];
			String getUrl = "https://booking.xpressair.co.id/EAStart.aspx?New=1&Language=en&CurrencyOverride=IDR";
			post = new QFPostMethod(getUrl);
		cookies=StringUtils.join(httpClient.getState().getCookies(),";");
		 post.setRequestHeader("Cookie",cookies);
//		Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);   
//		Protocol.registerProtocol("https", myhttps);   
		//通过“__VIEWSTATE”再次请求
		Map map=this.getAddress();
		NameValuePair[] pairs = new NameValuePair[]{
				new NameValuePair("_TSM_HiddenField_", _TSM_HiddenField_),
			     new NameValuePair("__VIEWSTATE", __VIEWSTATE),
			     new NameValuePair("HoursFromGMT", "8"),
			     new NameValuePair("MinutesFromGMT", "0"),
			     new NameValuePair("rblDirection", "0"),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Curr", "IDR"),
			     new NameValuePair("OutboundDate", deptdate),
			     new NameValuePair("HomeboundDate", backdate),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Adults", "1"),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Children", "0"),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Infants", "0"),
			     new NameValuePair("ctl00$ContentPlaceHolder1$search", "SEARCH"),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Origin", map.get(arg0.getDep()).toString()),
			     new NameValuePair("ctl00$ContentPlaceHolder1$Destination", map.get(arg0.getArr()).toString()),
			     new NameValuePair("ctl00$ContentPlaceHolder1$NoBot$NoBot_NoBotExtender_ClientState", ClientState),
			   };
		post.setRequestBody(pairs);
		int status=httpClient.executeMethod(post);
		return post.getResponseBodyAsString();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if (null != post){
				post.releaseConnection();
			}
			if (null != get){
				get.releaseConnection();
			}
		}
		return "Exception";	
	}
	
	@Override
	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String tablehtml=StringUtils.substringBetween(arg0, "<table class=\"colored\">", "</tbody>");
		String rethtml=StringUtils.substringBetween(arg0, "id=\"ctl00_ContentPlaceHolder1_HomeBoundFlights\"", "<div class=\"clear\">");
		String rettr=rethtml.substring(rethtml.indexOf("</tr>"), rethtml.lastIndexOf("<tr>"));
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(tablehtml)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;
		}		
		//需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (tablehtml.contains("Sorry, no departures available for the outbound flight. It is possible that the timetable for this period is unavailable.")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;
		}
		String _TSM_HiddenField_="";
		String __VIEWSTATE="";
		String clientState="";
		NameValuePair[] pairsnumber = null;
		Map map=this.getAddress();
		String[] date=arg1.getDepDate().split("-");
		String deptdate=date[1]+"/"+date[2]+"/"+date[0];
		try{
		if(null!=tablehtml){
			List<RoundTripFlightInfo> flightList = new ArrayList<RoundTripFlightInfo>();
			String[] trhtml=tablehtml.substring(tablehtml.indexOf("</tr>")+5, tablehtml.length()).replace("\r\n", "").trim().split("<tr");
			boolean flag=false;
			for(int i=0;i<trhtml.length;i++){
				List<FlightSegement> segs = new ArrayList<FlightSegement>();
				RoundTripFlightInfo baseFlight = new RoundTripFlightInfo();
				FlightDetail flightDetail = new FlightDetail();
				if(null!=trhtml[i]&&!"".equals(trhtml[i])){
					String filghtNumber="";
					String[] td=trhtml[i].split("<td");
					if(td[1].contains(arg1.getDepDate())){
						flag=true;
						String depttime=StringUtils.substringBetween(td[3], "\">", "</span>");
						String arrtime=StringUtils.substringBetween(td[4], "\">", "</span>");
						String price="0";
						String index="";
						if(!td[5].contains("Full")&&!td[5].contains("N/A")){
							String str=StringUtils.substringBetween(td[5], "<label", "</label>");
							price=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
							index="1";
						}else if(!td[6].contains("Full")&&!td[6].contains("N/A")){
							String str=StringUtils.substringBetween(td[6], "<label", "</label>");
							price=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
							index="2";
						}else if(!td[7].contains("Full")&&!td[7].contains("N/A")){
							String str=StringUtils.substringBetween(td[7], "<label", "</label>");
							price=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
							index="3";
						}else{
							result.setRet(false);
							result.setStatus(Constants.NO_RESULT);
							return result; 
						}
						String []retstr=rettr.split("<tr");
						List<FlightSegement> retsegs = new ArrayList<FlightSegement>();
						for(int m=0;m<retstr.length;m++){
							if(retstr[m].contains(arg1.getRetDate())){
								String[] rettd=retstr[m].split("<td");
								String retdeptime=StringUtils.substringBetween(rettd[3], "\">", "</span>");
								String retarrtime=StringUtils.substringBetween(rettd[4], "\">", "</span>");
								String retprice="0";
								int retindex=0;
								if(!rettd[5].contains("Full")&&!rettd[5].contains("N/A")){
									String str=StringUtils.substringBetween(rettd[5], "<label", "</label>");
									retprice=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
									retindex=1;
								}else if(!rettd[6].contains("Full")&&!rettd[6].contains("N/A")){
									String str=StringUtils.substringBetween(rettd[6], "<label", "</label>");
									retprice=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
									retindex=2;
								}else if(!rettd[7].contains("Full")&&!rettd[7].contains("N/A")){
									String str=StringUtils.substringBetween(rettd[7], "<label", "</label>");
									retprice=str.substring(str.lastIndexOf("\">")+2, str.length()).replace(",", "").trim();
									retindex=3;
								}
								//为获取航班号的方法getFlightNumber(FlightSearchParam arg,NameValuePair[] pairs)准备参数－－－－－－－start
								String tsmtr=StringUtils.substringBetween(arg0, "id=\"_TSM_HiddenField_\"", "/>");
								_TSM_HiddenField_=tsmtr.substring(tsmtr.indexOf("value=\"")+7, tsmtr.lastIndexOf("\""));
								String vsstr=StringUtils.substringBetween(arg0, "id=\"__VIEWSTATE\"", "/>");
								__VIEWSTATE=vsstr.substring(vsstr.indexOf("value=\"")+7, vsstr.lastIndexOf("\""));
								clientState= "-"+(Integer.parseInt(StringUtils.substringBetween(arg0, "~","\""))+1);
								String LowestCommonClass=StringUtils.substringBetween(td[1], "LowestCommonClass\"", "/>");
								String rbFlightname=StringUtils.substringBetween(td[4+Integer.parseInt(index)], "name=\"", "\"");
								String rbFlightvalue=StringUtils.substringBetween(td[4+Integer.parseInt(index)], "value=\"", "\"");
								String btnContinue=StringUtils.substringBetween(StringUtils.substringBetween(arg0, "btnContinue\"", "/>"),"value=\"","\"");
								String outboundDate=StringUtils.substringBetween(StringUtils.substringBetween(arg0, "id=\"OutboundDate\"", "/>"),"value=\"","\"");
								String hfFlightNumbername="";
								String LowestCommonClassName="";
								String LowestCommonClassValue="";
								String hfCompleteETDname="";
								String hfCompleteETDvalue="";
								String hfCompleteETAname="";
								String hfCompleteETAvalue="";
								String hfDatename="";
								String hfDatevalue="";
								String hfAdultFarename="";
								String hfAdultFarevalue="";
								String hfChildFarename="";
								String hfChildFarevalue="";
								String hfInfantFarename="";
								String hfInfantFarevalue="";
								String hfAdultNetFarename="";
								String hfAdultNetFarevalue="";
								String hfChildNetFarename="";
								String hfChildNetFarevalue="";
								String hfInfantNetFarename="";
								String hfInfantNetFarevalue="";
								String hfCommission1_name="";
								String hfCommission1_value="";
								String hfCommission2_name="";
								String hfCommission2_value="";
								String hfAdultTaxname="";
								String hfAdultTaxvalue="";
								String hfAdultVATname="";
								String hfAdultVATvalue="";
								String hfChildTaxname="";
								String hfChildTaxvalue="";
								String hfChildVATname="";
								String hfChildVATvalue="";
								String hfInfantTaxname="";
								String hfInfantTaxvalue="";
								String hfInfantVATname="";
								String hfInfantVATvalue="";
								
								String[] inputs=td[1].split("<input ");
								for(int j=0;j<inputs.length;j++){
									if(inputs[j].contains("FlightNumber")){
										hfFlightNumbername=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										filghtNumber=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("LowestCommonClass")){
										LowestCommonClassName=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										LowestCommonClassValue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfCompleteETD")){
										hfCompleteETDname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfCompleteETDvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfCompleteETA")){
										hfCompleteETAname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfCompleteETAvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfDate")){
										hfDatename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfDatevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfAdultFare"+index)){
										hfAdultFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfAdultFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfChildFare"+index)){
										hfChildFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfChildFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfInfantFare"+index)){
										hfInfantFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfInfantFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfAdultNetFare"+index)){
										hfAdultNetFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfAdultNetFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfChildNetFare"+index)){
										hfChildNetFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfChildNetFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfInfantNetFare"+index)){
										hfInfantNetFarename=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfInfantNetFarevalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfCommission1_"+index)){
										hfCommission1_name=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfCommission1_value=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfCommission2_"+index)){
										hfCommission2_name=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfCommission2_value=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfAdultTax"+index)){
										hfAdultTaxname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfAdultTaxvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfAdultVAT"+index)){
										hfAdultVATname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfAdultVATvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfChildTax"+index)){
										hfChildTaxname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfChildTaxvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfChildVAT"+index)){
										hfChildVATname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfChildVATvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfInfantTax"+index)){
										hfInfantTaxname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfInfantTaxvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									if(inputs[j].contains("hfInfantVAT"+index)){
										hfInfantVATname=StringUtils.substringBetween(inputs[j], "name=\"", "\"");
										hfInfantVATvalue=StringUtils.substringBetween(inputs[j], "value=\"", "\"");
										continue;
									}
									
									
								}
								String[] retinput=rettd[1].split("<input ");
								String rethfFlightNumbername="";
								String rethfFlightNumbervalue="";
								for(int k=0;k<retinput.length;k++){
									if(retinput[k].contains("FlightNumber")){
										rethfFlightNumbername=StringUtils.substringBetween(retinput[k], "name=\"", "\"");
										rethfFlightNumbervalue=StringUtils.substringBetween(retinput[k], "value=\"", "\"");
										break;
									}
								}
								pairsnumber=new NameValuePair[]{
										new NameValuePair("_TSM_HiddenField_", _TSM_HiddenField_),
									    new NameValuePair("__VIEWSTATE", __VIEWSTATE),	
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfOutboundFlight", filghtNumber),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfHomeboundFlight", rethfFlightNumbervalue),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfOutboundClass", index),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfHomeboundClass",retindex+""),	
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfOutboundETA", hfCompleteETAvalue),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfOutboundDate", arg1.getDepDate()),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfHomeboundDate", arg1.getRetDate()),
									    new NameValuePair("ctl00$ContentPlaceHolder1$hfOutboundLowestCommonClass", LowestCommonClassValue),
									    new NameValuePair("HoursFromGMT", "8"),
									    new NameValuePair("MinutesFromGMT", "0"),
									    new NameValuePair("TotalFareCopy", price),
									    new NameValuePair("rblDirection", "1"),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Curr", "IDR"),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Origin",map.get(arg1.getDep()).toString()),
									    new NameValuePair("OutboundDate", outboundDate),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Destination", map.get(arg1.getArr()).toString()),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Adults", "1"),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Children", "0"),
									    new NameValuePair("ctl00$ContentPlaceHolder1$Infants", "0"),
									    new NameValuePair("ctl00$ContentPlaceHolder1$NoBot$NoBot_NoBotExtender_ClientState",clientState ),
									    new NameValuePair(hfFlightNumbername, filghtNumber),
									    new NameValuePair(rethfFlightNumbername, rethfFlightNumbervalue),
									    new NameValuePair(hfDatename, hfDatevalue),
									    new NameValuePair(hfCompleteETDname, hfCompleteETDvalue),
										new NameValuePair(hfCompleteETAname, hfCompleteETAvalue),
									    new NameValuePair("ctl00$ContentPlaceHolder1$btnContinue",btnContinue ),
//									    new NameValuePair(hfAdultFarename, hfAdultFarevalue),
//									    new NameValuePair(hfChildFarename, hfChildFarevalue),
//									    new NameValuePair(hfInfantFarename, hfInfantFarevalue),
//									    new NameValuePair(hfAdultNetFarename, hfAdultNetFarevalue),
//									    new NameValuePair(hfChildNetFarename, hfChildNetFarevalue),
//									    new NameValuePair(hfInfantNetFarename,hfInfantNetFarevalue),
									    new NameValuePair(hfCommission1_name,hfCommission1_value),
									    new NameValuePair(hfCommission2_name,hfCommission2_value),
//									    new NameValuePair(hfChildVATname,hfChildVATvalue),
//									    new NameValuePair(hfAdultTaxname,hfAdultTaxvalue),
//									    new NameValuePair(hfAdultVATname,hfAdultVATvalue),
//									    new NameValuePair(hfChildTaxname,hfChildTaxvalue),
//									    new NameValuePair(hfInfantTaxname,hfInfantTaxvalue),
//									    new NameValuePair(hfInfantVATname,hfInfantVATvalue),
									    
									    new NameValuePair(LowestCommonClassName, LowestCommonClassValue),
									    
									    
									    new NameValuePair(rbFlightname, rbFlightvalue),
								};
								//为获取航班号的方法getFlightNumber(FlightSearchParam arg,NameValuePair[] pairs)准备参数－－－－－－－end
								
								String[] retflightNo=this.getFlightNumber(arg1,pairsnumber);
								List<String> flightNoList = new ArrayList<String>();
								FlightSegement seg = new FlightSegement();
								if(null!=retflightNo){
									flightNoList.add(retflightNo[0]);
									seg.setFlightno(retflightNo[0]);
								}
								seg.setDepDate(arg1.getDepDate());
								seg.setDepairport(arg1.getDep());
								seg.setArrairport(arg1.getArr());
								seg.setDeptime(depttime);
								seg.setArrtime(arrtime);
								seg.setCompany("XN");
								segs.add(seg);
								
								flightDetail.setMonetaryunit("IDR");
								flightDetail.setPrice(Double.parseDouble(price)+Double.parseDouble(retprice));
								flightDetail.setDepcity(arg1.getDep());
								flightDetail.setArrcity(arg1.getArr());
								flightDetail.setWrapperid(arg1.getWrapperid());
								flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
								flightDetail.setFlightno(flightNoList);
								flightDetail.setTax(0);
								if(retindex!=0){
								List<String> retflightNoList = new ArrayList<String>();
								FlightSegement retseg = new FlightSegement();
								if(null!=retflightNo&&retflightNo.length>1){
									retflightNoList.add(retflightNo[1]);
									retseg.setFlightno(retflightNo[1]);
								}
								retseg.setDepDate(arg1.getRetDate());
								retseg.setDepairport(arg1.getArr());
								retseg.setArrairport(arg1.getDep());
								retseg.setDeptime(retdeptime);
								retseg.setArrtime(retarrtime);
								retseg.setCompany("XN");
								retsegs.add(retseg);
								baseFlight.setRetflightno(retflightNoList);
								baseFlight.setRetinfo(retsegs);
								baseFlight.setOutboundPrice(Double.parseDouble(retprice));
								}
								baseFlight.setDetail(flightDetail);
								baseFlight.setInfo(segs);
								baseFlight.setRetdepdate(Date.valueOf(arg1.getRetDate()));
								flightList.add(baseFlight);
							}
						}
						
						
					}
				}
			}
			if(flag){
				result.setRet(true);
				result.setStatus(Constants.SUCCESS);
				result.setData(flightList);
				return result;
			}else{
				result.setRet(false);
				result.setStatus(Constants.NO_RESULT);
				return result;
			}
			
		}else{
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;
		}
		}catch (Exception e) {
			System.out.println(e);
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	/**
	 * 获取航班号
	 * @return
	*/
	public String[] getFlightNumber(FlightSearchParam arg,NameValuePair[] pairs){
		QFPostMethod post=null;
		QFGetMethod get = null;	
		String []flightNo=null;
		try {
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		String getUrl = "https://booking.xpressair.co.id/EAStart.aspx?New=1&Language=en&CurrencyOverride=IDR";
		post = new QFPostMethod(getUrl);
		post.setRequestBody(pairs);
		post.setFollowRedirects(false);
		cookies=StringUtils.join(httpClient.getState().getCookies(),";");
		httpClient.getState().clearCookies();
		post.setRequestHeader("Cookie",cookies);
		int status=httpClient.executeMethod(post);
		get=new QFGetMethod("https://booking.xpressair.co.id/EA/PassengersEA.aspx?Guid=&AddMode=");
//		cookies=StringUtils.join(httpClient.getState().getCookies(),";");
//		httpClient.getState().clearCookies();
		get.setRequestHeader("Cookie",cookies);
		get.setRequestHeader("Referer", "https://booking.xpressair.co.id/EAStart.aspx?New=1&Language=en&CurrencyOverride=IDR");
		status=httpClient.executeMethod(get);
		String html= get.getResponseBodyAsString();
		System.out.println(html);
		String tbody=StringUtils.substringBetween(html, "table class=\"colored step2\" style=\"margin-bottom:15px\">", "</table>");
		
		//获取航班号
		if(null!=tbody){
			String[] tr=tbody.substring(tbody.indexOf("</tr>")+5, tbody.lastIndexOf("<tr>")-4).replace("\r\n", "").trim().split("<tr>");
			flightNo=new String[tr.length];
			int k=0;
			for(int i=0;i<tr.length;i++){
				if(null!=tr[i]&&!"".equals(tr[i])){
					String[] td=tr[i].split("<td");
					if(td!=null){
						flightNo[k]=StringUtils.substring(td[2], td[2].indexOf(">")+1, td[2].indexOf("</td>"));
						k++;
					}
				}
			}
		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if (null != post){
				post.releaseConnection();
			}
			if (null != get){
				get.releaseConnection();
			}
		}
		return flightNo;
	} 
	/**
	 * 三字码转数字码
	 * @return
	 */
	public Map<String,String> getAddress(){
		Map<String,String> map=new HashMap<String,String>();
		map.put("AMQ", "172");
		map.put("BDO", "197");
		map.put("BTH", "194");
		map.put("FKQ", "169");
		map.put("CGK", "141");
		map.put("DJJ", "158");
		map.put("JHB", "224");
		map.put("KNG", "164");
		map.put("KCH", "203");
		map.put("LAH", "174");
		map.put("LUW", "166");
		map.put("UPG", "142");
		map.put("MLG", "200");
		map.put("MDC", "159");
		map.put("MKW", "157");
		map.put("MNA", "170");
		map.put("PDG", "205");
		map.put("PLM", "206");
		map.put("PLW", "167");
		map.put("PNK", "195");
		map.put("SOQ", "143");
		map.put("SUB", "156");
		map.put("TTE", "163");
		map.put("TLI", "196");
		map.put("JOG", "162");
		return map;
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("BDO");
//		searchParam.setArr("JOG");
		searchParam.setArr("PDG");
		searchParam.setDepDate("2014-06-27");
		searchParam.setRetDate("2014-06-28");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjsairxn001");
		searchParam.setToken("");
		Wrapper_gjsairxn001 gjsairxn001 =new  Wrapper_gjsairxn001();
		String html = gjsairxn001.getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = gjsairxn001.process(html,searchParam);
		if(result.isRet() && result.getStatus().equals(Constants.SUCCESS))
		{
			List<RoundTripFlightInfo> flightList = (List<RoundTripFlightInfo>) result.getData();
			for (RoundTripFlightInfo in : flightList){
				System.out.println("------------" + in.getDetail());
				System.out.println("************" + in.getInfo().toString());
				System.out.println("++++++++++++" + in.getRetinfo().toString());
				System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			}
		}
		else
		{
			System.out.println(result.getStatus());
		}
	}
}
