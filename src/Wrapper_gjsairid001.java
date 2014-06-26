import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.RoundTripFlightInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;


public class Wrapper_gjsairid001 implements QunarCrawler{

	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "https://secure.batikair.com/BatikAirIBE/OnlineBooking.aspx";
		BookingResult bookingResult = new BookingResult();
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String[]	deptDate = formatter.parse(arg0.getDepDate()).toString().split(" ");
			String ddmm=deptDate[2]+deptDate[1];
			String[]	backDate = formatter.parse(arg0.getDepDate()).toString().split(" ");
			String backddmm=backDate[2]+backDate[1];
			BookingInfo bookingInfo = new BookingInfo();
			bookingInfo.setAction(bookingUrlPre);
			bookingInfo.setMethod("get");
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("trip_type", "return");
			map.put("persons.0", "1");
			map.put("persons.1", "0");
			map.put("persons.2", "0");
			map.put("depart",  arg0.getDep());
			map.put("dest.1", arg0.getArr());
			map.put("date.0",ddmm);
			map.put("date.1",backddmm);
			map.put("usercountry", "ID");
			map.put("carrier", "ID");
			map.put("date_flexibility", "fixed");
			bookingInfo.setInputs(map);		
			bookingResult.setData(bookingInfo);
			bookingResult.setRet(true);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return bookingResult;
	}

	@Override
	public String getHtml(FlightSearchParam arg0) {
		QFGetMethod get = null;	
		try {
		QFHttpClient httpClient = new QFHttpClient(arg0, false);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String[] todeptDate=formatter.parse(arg0.getDepDate()).toString().split(" ");
		String toddmm=todeptDate[2]+todeptDate[1];
		String[] backdeptDate=formatter.parse(arg0.getRetDate()).toString().split(" ");
		String backddmm=backdeptDate[2]+backdeptDate[1];
		
		String getUrl = String.format("https://secure.batikair.com/BatikAirIBE/onlinebooking.aspx?trip_type=return&persons.0=1&persons.1=0&persons.2=0&depart=%s&dest.1=%s&date.0=%s&date.1=%s&origin=EN&usercountry=ID&carrier=ID&date_flexibility=fixed", arg0.getDep(), arg0.getArr(),toddmm,backddmm);
		Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);   
		Protocol.registerProtocol("https", myhttps);   
		get = new QFGetMethod(getUrl);
		get.setRequestHeader("Referer", "http://www.batikair.com/");
		get.setFollowRedirects(false);
		int status=httpClient.executeMethod(get);
		Cookie[] cookies = httpClient.getState().getCookies(); 
		if(cookies !=null && cookies.length>0){

            String cook=cookies[0].getValue();
            for (int i = 1; i < cookies.length; i++) {
                cook += "; " + cookies[i].getName() + "=" + cookies[i].getValue();
            }
            get.setRequestHeader("Cookie",cook);
		}
		get = new QFGetMethod("https://secure.batikair.com/BatikAirIBE/OnlineBooking.aspx");
		get.setRequestHeader("Referer", getUrl);
		
		status=httpClient.executeMethod(get);
		return get.getResponseBodyAsString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if (null != get){
				get.releaseConnection();
			}
		}
		return "Exception";	
	}

	@Override
	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(arg0)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}
		if (arg0.contains("no flights")) {
			result.setRet(true);
			result.setStatus(Constants.NO_RESULT);
			return result;			
		}
		List<RoundTripFlightInfo> flightList = new ArrayList<RoundTripFlightInfo>();
		String tohtml=StringUtils.substringBetween(arg0,"<table id=\"tblOutFlightBlocks\"", "<td colspan=\"7\">");
		String backhtml=StringUtils.substringBetween(arg0,"<div id=\"pnlRetFlights\">", "<td colspan=\"7\">");
		backhtml=backhtml.substring(backhtml.indexOf("<table id=\"tblInFlightBlocks\""), backhtml.length());
		if(null!=tohtml){
			String toflightsstr=tohtml.substring(tohtml.indexOf("</tr>")+5, tohtml.lastIndexOf("<tr>"));
			String[] toflights=toflightsstr.split("<tr id=\"flightRowOutbound");
			int m=0;
			for(int i=0;i<toflights.length;i++){
				if(!toflights[i].equals("")){
				RoundTripFlightInfo baseFlight = new RoundTripFlightInfo();
				FlightDetail toflightDetail = new FlightDetail();
				List<FlightSegement> tosegs = new ArrayList<FlightSegement>();
				String[] toinfo=toflights[i].split("<td");
				int t1=1;
				if(toflights[i].contains("rowspan")){
					m=Integer.parseInt(StringUtils.substringBetween(toflights[i], "rowspan=\"", "\""))-1;
					t1=Integer.parseInt(StringUtils.substringBetween(toflights[i], "rowspan=\"", "\""));
				}
				String toflightNumbe=toinfo[1].substring(toinfo[1].indexOf("</div>"),toinfo[1].indexOf("</div><div")).replace("</div>", "").replace(" ","").trim();
				String toflightdeptimeweek=toinfo[3].substring(toinfo[3].indexOf("<br />"), toinfo[3].indexOf("</td>")).replace("<br />", "");
				String toflightarrtimeweek=toinfo[4].substring(toinfo[4].indexOf("<br />"), toinfo[4].indexOf("</td>")).replace("<br />", "");
				String  todepairport=StringUtils.substringBetween(toinfo[3],"(", ")");
				String  toarrairport=StringUtils.substringBetween(toinfo[4],"(", ")");
				String toflightdeptime=toflightdeptimeweek.substring(toflightdeptimeweek.indexOf(" "), toflightdeptimeweek.length()).trim();
				String toflightarrtime=toflightarrtimeweek.substring(toflightarrtimeweek.indexOf(" "),toflightarrtimeweek.length()).trim();
//				String topromoTotal="0";
				String topromoPrice="0";
				Double topromotax=0d;
				String tounit="";
				if(toinfo.length>5){
					t1=t1*10000;
					if(!toinfo[5].contains("Sold Out")&&!toinfo[5].contains("N/A")){
						String[] unitPrice=StringUtils.substringBetween(toinfo[5],"Base Fare:","Total").split(" ");
						tounit=unitPrice[0].trim();
//						topromoTotal=StringUtils.substringBetween(toinfo[5],"<br />","</label>");
						topromoPrice=unitPrice[1].trim();
						topromotax=Double.valueOf(StringUtils.substringBetween(toinfo[5],"Fees:","\"><input").replace(tounit, "").replace(",", "").trim())+t1;
						
					}else if(!toinfo[6].contains("Sold Out")&&!toinfo[6].contains("N/A")){
						String[] unitPrice=StringUtils.substringBetween(toinfo[6],"Base Fare:","Total").split(" ");
						tounit=unitPrice[0].trim();
//						topromoTotal=StringUtils.substringBetween(toinfo[6],"<br />","</label>");
						topromoPrice=unitPrice[1].trim();
						topromotax=Double.valueOf(StringUtils.substringBetween(toinfo[6],"Fees:","\"><input").replace(tounit, "").replace(",", "").trim())+t1;
					}else if(!toinfo[7].contains("Sold Out")&&!toinfo[7].contains("N/A")){
						String[] unitPrice=StringUtils.substringBetween(toinfo[7],"Base Fare:","Total").split(" ");
						tounit=unitPrice[0].trim();
//						topromoTotal=StringUtils.substringBetween(toinfo[7],"<br />","</label>");
						topromoPrice=unitPrice[1].trim();
						topromotax=Double.valueOf(StringUtils.substringBetween(toinfo[7],"Fees:","\"><input").replace(tounit, "").replace(",", "").trim())+t1;
					}else{
						continue;
					}
					
					FlightSegement toseg = new FlightSegement();
					toseg.setFlightno(toflightNumbe);
					toseg.setDepDate(arg1.getDepDate());
					toseg.setDepairport(todepairport);
					toseg.setArrairport(toarrairport);
					toseg.setDeptime(toflightdeptime);
					toseg.setArrtime(toflightarrtime);
					tosegs.add(toseg);
					
					List<String> toflightNoList = new ArrayList<String>();
					toflightNoList.add(toflightNumbe);
					
					toflightDetail.setFlightno(toflightNoList);
					toflightDetail.setDepcity(arg1.getDep());
					toflightDetail.setArrcity(arg1.getArr());
					toflightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
					toflightDetail.setMonetaryunit(tounit);
					toflightDetail.setPrice(Math.round(Double.parseDouble(topromoPrice.replace(",", ""))));
					toflightDetail.setTax((Math.round(topromotax)));
					toflightDetail.setWrapperid(arg1.getWrapperid());
					baseFlight.setDetail(toflightDetail);
					baseFlight.setOutboundPrice(Math.round(Double.parseDouble(topromoPrice.replace(",", ""))));
					baseFlight.setInfo(tosegs);
					flightList.add(baseFlight);
					}else{
						FlightSegement toseg = new FlightSegement();
						toseg.setFlightno(toflightNumbe);
						toseg.setDepairport(todepairport);
						toseg.setArrairport(toarrairport);
						toseg.setDeptime(toflightdeptime);
						toseg.setArrtime(toflightarrtime);
						List<String> toflightNoList = new ArrayList<String>();
						toflightNoList.add(toflightNumbe);
						baseFlight=flightList.get(flightList.size()-1);
						baseFlight.getInfo().add(toseg);
						baseFlight.getDetail().getFlightno().addAll(toflightNoList);
						m--;
					}
				boolean retflag=false;
				if(m==0){//m为0时去航班中转结束
					String backflightsstr=backhtml.substring(backhtml.indexOf("</tr>")+5, backhtml.lastIndexOf("<tr>"));
					String[] backflights=backflightsstr.split("<tr");
					int k=0;//k为0时第一次返航，
					Double goprice=null;
					Double gotax=null;
					for(int j=0;j<backflights.length;j++){
						if(!backflights[j].equals("")){
						List<FlightSegement> backsegs = new ArrayList<FlightSegement>();
						String[] backinfo=backflights[j].split("<td");
						String backflightNumbe=backinfo[1].substring(backinfo[1].indexOf("</div>"),backinfo[1].indexOf("</div><div")).replace("</div>", "").replace(" ","").trim();
						String backflightdeptimeweek=backinfo[3].substring(backinfo[3].indexOf("<br />"), backinfo[3].indexOf("</td>")).replace("<br />", "");
						String backflightarrtimeweek=backinfo[4].substring(backinfo[4].indexOf("<br />"), backinfo[4].indexOf("</td>")).replace("<br />", "");
						String  backdepairport=StringUtils.substringBetween(backinfo[3],"(", ")");
						String  backarrairport=StringUtils.substringBetween(backinfo[4],"(", ")");
						String backflightdeptime=backflightdeptimeweek.substring(backflightdeptimeweek.indexOf(" "), backflightdeptimeweek.length()).trim();
						String backflightarrtime=backflightarrtimeweek.substring(backflightarrtimeweek.indexOf(" "),backflightarrtimeweek.length()).trim();
						int t2=1;//中转航班数
						if(backflights[j].contains("rowspan")){
								t2=Integer.parseInt(StringUtils.substringBetween(backflights[j], "rowspan=\"", "\""));
						}
						String backpromoTotal="0";
						String backpromoPrice="0";
						Double backpromotax=0d;
						String backunit="";
						if(backinfo.length>5){
							t2=t2*10000;
						if(!backinfo[5].contains("Sold Out")&&!backinfo[5].contains("N/A")){
							String[] backunitPrice=StringUtils.substringBetween(backinfo[5],"Base Fare:","Total").split(" ");
							backunit=backunitPrice[0].trim();
//							backpromoTotal=StringUtils.substringBetween(backinfo[5],"<br />","</label>");
							backpromoPrice=backunitPrice[1].trim();
							backpromotax=Double.parseDouble(StringUtils.substringBetween(backinfo[5],"Fees:","\"><input").replace(backunit, "").replace(",", "").trim())+t2;
							
						}else if(!backinfo[6].contains("Sold Out")&&!backinfo[6].contains("N/A")){
							String[] backunitPrice=StringUtils.substringBetween(backinfo[6],"Base Fare:","Total").split(" ");
							backunit=backunitPrice[0].trim();
//							backpromoTotal=StringUtils.substringBetween(backinfo[6],"<br />","</label>");
							backpromoPrice=backunitPrice[1].trim();
							backpromotax=Double.parseDouble(StringUtils.substringBetween(backinfo[6],"Fees:","\"><input").replace(backunit, "").replace(",", "").trim())+t2;
						}else if(!backinfo[7].contains("Sold Out")&&!backinfo[7].contains("N/A")){
							String[] backunitPrice=StringUtils.substringBetween(backinfo[7],"Base Fare:","Total").split(" ");
							backunit=backunitPrice[0].trim();
//							backpromoTotal=StringUtils.substringBetween(backinfo[7],"<br />","</label>");
							backpromoPrice=backunitPrice[1].trim();
							backpromotax=Double.parseDouble(StringUtils.substringBetween(backinfo[6],"Fees:","\"><input").replace(backunit, "").replace(",", "").trim())+t2;
						}else{
							continue;
						}
						retflag=true;
						FlightSegement backseg = new FlightSegement();
						backseg.setFlightno(backflightNumbe);
						backseg.setDepDate(arg1.getRetDate());
						backseg.setDepairport(backdepairport);
						backseg.setArrairport(backarrairport);
						backseg.setDeptime(backflightdeptime);
						backseg.setArrtime(backflightarrtime);
						backsegs.add(backseg);
						
						List<String> flightNoList = new ArrayList<String>();
						flightNoList.add(backflightNumbe);
						if(k==0){
							baseFlight=flightList.get(flightList.size()-1);
							goprice=baseFlight.getDetail().getPrice();
							gotax=baseFlight.getDetail().getTax();
							Long sumPrice=Math.round(goprice+Double.parseDouble(backpromoPrice.replace(",", "")));
							Long sumTax=Math.round(gotax+backpromotax);
							baseFlight.getDetail().setPrice(sumPrice);
							baseFlight.getDetail().setTax(sumTax);
							baseFlight.setRetflightno(flightNoList);
							baseFlight.setRetdepdate(Date.valueOf(arg1.getRetDate()));
							baseFlight.setRetinfo(backsegs);
							baseFlight.setReturnedPrice(Math.round(Double.parseDouble(backpromoPrice.replace(",", ""))));
							
						}else{
							Long sumPrice=Math.round(goprice+Double.parseDouble(backpromoPrice.replace(",", "")));
							Long sumTax=Math.round(gotax+backpromotax);
							baseFlight=flightList.get(flightList.size()-1);
							RoundTripFlightInfo backFB=new RoundTripFlightInfo();
							FlightDetail detail=new FlightDetail();
							detail.setArrcity(baseFlight.getDetail().getArrcity());
							detail.setDepcity(baseFlight.getDetail().getDepcity());
							detail.setDepdate(baseFlight.getDetail().getDepdate());
							backFB.setRetflightno(flightNoList);
							detail.setMonetaryunit(baseFlight.getDetail().getMonetaryunit());
							detail.setWrapperid(baseFlight.getDetail().getWrapperid());
							detail.setPrice(sumPrice);
							detail.setTax(sumTax);
							detail.setFlightno(baseFlight.getDetail().getFlightno());
							backFB.setDetail(detail);
							backFB.setInfo(baseFlight.getInfo());
							
							backFB.setRetdepdate(Date.valueOf(arg1.getRetDate()));
							backFB.setRetinfo(backsegs);
							backFB.setReturnedPrice(Math.round(Double.parseDouble(backpromoPrice.replace(",", ""))));
							flightList.add(backFB);
						}
						k+=1;
						}else{
							FlightSegement backseg = new FlightSegement();
							backseg.setFlightno(backflightNumbe);
							backseg.setDepDate(arg1.getRetDate());
							backseg.setDepairport(backdepairport);
							backseg.setArrairport(backarrairport);
							backseg.setDeptime(backflightdeptime);
							backseg.setArrtime(backflightarrtime);
							List<String> flightNoList = new ArrayList<String>();
							flightNoList.add(backflightNumbe);
							baseFlight=flightList.get(flightList.size()-1);
							baseFlight.getRetinfo().add(backseg);
							baseFlight.getRetflightno().addAll(flightNoList);
						}
					}
					}
				}
			}
			}
		}
		result.setRet(true);
		result.setStatus(Constants.SUCCESS);
		result.setData(flightList);
		return result;
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("CGK");//SUB
		searchParam.setArr("SUB");//SOQ
		searchParam.setDepDate("2014-06-27");
		searchParam.setRetDate("2014-06-28");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjsairid001");
		searchParam.setToken("");
		String html = new  Wrapper_gjsairid001().getHtml(searchParam);
		ProcessResultInfo result = new ProcessResultInfo();
		result = new  Wrapper_gjsairid001().process(html,searchParam);
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
