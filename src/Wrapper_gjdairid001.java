import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;

public class Wrapper_gjdairid001  implements QunarCrawler{

	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		
		String bookingUrlPre = "https://secure.batikair.com/BatikAirIBE/onlinebooking.aspx";
		BookingResult bookingResult = new BookingResult();
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			String[]	deptDate = formatter.parse(arg0.getDepDate()).toString().split(" ");
			String ddmm=deptDate[2]+deptDate[1];
			BookingInfo bookingInfo = new BookingInfo();
			bookingInfo.setAction(bookingUrlPre);
			bookingInfo.setMethod("get");
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("trip_type", "one+way");
			map.put("persons.0", "1");
			map.put("persons.1", "0");
			map.put("persons.2", "0");
			map.put("depart",  arg0.getDep());
			map.put("dest.1", arg0.getArr());
			map.put("date.0",ddmm);
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
//		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String[] deptDate=formatter.parse(arg0.getDepDate()).toString().split(" ");
		String ddmm=deptDate[2]+deptDate[1];
		
		String getUrl = String.format("https://secure.batikair.com/BatikAirIBE/onlinebooking.aspx?trip_type=one+way&persons.0=1&persons.1=0&persons.2=0&depart=%s&dest.1=%s&date.0=%s&origin=EN&usercountry=ID&carrier=ID&date_flexibility=fixed", arg0.getDep(), arg0.getArr(),ddmm);
//		Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);   
//		Protocol.registerProtocol("https", myhttps);   
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
//            cookies[0].setValue(cook);
//            HttpState state = new HttpState();
//            state.addCookie(cookies[0]);
//            httpClient.setState(state);
            get.setRequestHeader("Cookie",cook);
		}
		get = new QFGetMethod("https://secure.batikair.com/BatikAirIBE/OnlineBooking.aspx");
		get.setRequestHeader("Referer", getUrl);
		
		status=httpClient.executeMethod(get);
//		CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
//		 HttpState state = new HttpState();
//		 String cookie=httpClient.getState().getCookies()[0].getValue();
//		 httpClient.getState().getCookies()[0].setValue(cookie);
//         state.addCookie(httpClient.getState().getCookies()[0]);
//         httpClient.setState(state);
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
		List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
		try {	
		String ht=StringUtils.substringBetween(arg0,"<table id=\"tblOutFlightBlocks\"", "<td colspan=\"7\">");
		if(null!=ht){
			
			String flightsstr=ht.substring(ht.indexOf("</tr>")+5, ht.lastIndexOf("<tr>"));
			String[] flights=flightsstr.split("<tr id=\"flightRowOutbound");
			for(int i=0;i<flights.length;i++){
				if(!flights[i].equals("")){
				List<FlightSegement> segs = new ArrayList<FlightSegement>();
				OneWayFlightInfo baseFlight = new OneWayFlightInfo();
				FlightDetail flightDetail = new FlightDetail();
				String[] info=flights[i].split("<td");
				String flightNumbe=info[1].substring(info[1].indexOf("</div>"),info[1].indexOf("</div><div")).replace("</div>", "").replace(" ","").trim();
				String flightdeptimeweek=info[3].substring(info[3].indexOf("<br />"), info[3].indexOf("</td>")).replace("<br />", "");
				String flightarrtimeweek=info[4].substring(info[4].indexOf("<br />"), info[4].indexOf("</td>")).replace("<br />", "");
				String  depairport=StringUtils.substringBetween(info[3],"(", ")");
				String  arrairport=StringUtils.substringBetween(info[4],"(", ")");
				String flightdeptime=flightdeptimeweek.substring(flightdeptimeweek.indexOf(" "), flightdeptimeweek.length()).trim();
				String flightarrtime=flightarrtimeweek.substring(flightarrtimeweek.indexOf(" "),flightarrtimeweek.length()).trim();
				String promoTotal="0";
				String promoPrice="0";
				String promotax="0";
				String unit="";
				if(info.length>5){
				if(!info[5].contains("Sold Out")&&!info[5].contains("N/A")){
					String[] unitPrice=StringUtils.substringBetween(info[5],"Base Fare:","Total").split(" ");
					unit=unitPrice[0].trim();
					promoTotal=StringUtils.substringBetween(info[5],"<br />","</label>");
					promoPrice=unitPrice[1].trim();
					promotax=StringUtils.substringBetween(info[5],"Fees:","\"><input").replace(unit, "").trim();
					
				}else if(!info[6].contains("Sold Out")&&!info[6].contains("N/A")){
					String[] unitPrice=StringUtils.substringBetween(info[6],"Base Fare:","Total").split(" ");
					unit=unitPrice[0].trim();
					promoTotal=StringUtils.substringBetween(info[6],"<br />","</label>");
					promoPrice=unitPrice[1].trim();
					promotax=StringUtils.substringBetween(info[6],"Fees:","\"><input").replace(unit, "").trim();
				}else if(!info[7].contains("Sold Out")&&!info[7].contains("N/A")){
					String[] unitPrice=StringUtils.substringBetween(info[7],"Base Fare:","Total").split(" ");
					unit=unitPrice[0].trim();
					promoTotal=StringUtils.substringBetween(info[7],"<br />","</label>");
					promoPrice=unitPrice[1].trim();
					promotax=StringUtils.substringBetween(info[7],"Fees:","\"><input").replace(unit, "").trim();
				}
				
				FlightSegement seg = new FlightSegement();
				seg.setFlightno(flightNumbe);
//				seg.setDepDate(arg1.getDepDate());
				seg.setDepairport(depairport);
				seg.setArrairport(arrairport);
				seg.setDeptime(flightdeptime);
				seg.setArrtime(flightarrtime);
				segs.add(seg);
				
				
				List<String> flightNoList = new ArrayList<String>();
				flightNoList.add(flightNumbe);
				
				flightDetail.setFlightno(flightNoList);
				flightDetail.setDepcity(arg1.getDep());
				flightDetail.setArrcity(arg1.getArr());
				flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
				flightDetail.setMonetaryunit(unit);
				flightDetail.setPrice(Math.round(Double.parseDouble(promoPrice.replace(",", ""))));
				flightDetail.setTax(Math.round(Double.parseDouble(promotax.replace(",", ""))));
				flightDetail.setWrapperid(arg1.getWrapperid());
				baseFlight.setDetail(flightDetail);
				baseFlight.setInfo(segs);
				flightList.add(baseFlight);
				}else{
					FlightSegement seg = new FlightSegement();
					seg.setFlightno(flightNumbe);
//					seg.setDepDate(arg1.getDep());
					seg.setDepairport(depairport);
					seg.setArrairport(arrairport);
					seg.setDeptime(flightdeptime);
					seg.setArrtime(flightarrtime);
					segs.add(seg);
					List<String> flightNoList = new ArrayList<String>();
					flightNoList.add(flightNumbe);
					baseFlight=flightList.get(flightList.size()-1);
					baseFlight.getInfo().add(seg);
					baseFlight.getDetail().getFlightno().addAll(flightNoList);
				}
			}
			}
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		}else{
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			result.setData(flightList);
			return result;
		}
		} catch(Exception e){
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("AMQ");
		searchParam.setArr("BPN");
		searchParam.setDepDate("2014-06-18");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjdairid001");
		searchParam.setToken("");
		String html = new  Wrapper_gjdairid001 ().getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = new  Wrapper_gjdairid001().process(html,searchParam);
		if(result.isRet() && result.getStatus().equals(Constants.SUCCESS))
		{
			List<OneWayFlightInfo> flightList = (List<OneWayFlightInfo>) result.getData();
			for (OneWayFlightInfo in : flightList){
				System.out.println("------------" + in.getDetail());
				System.out.println("************" + in.getInfo().toString());
			}
		}
		else
		{
			System.out.println(result.getStatus());
		}		
	}
}