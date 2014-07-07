import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
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

/**
 * 
 *诺达维亚航空
 *http://www.nordavia.ru/en/
 */

public class Wrapper_gjsair5n001 implements QunarCrawler {
	QFHttpClient httpClient = null;
	Map<String,String> map=null;
	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		BookingResult bookingResult = new BookingResult();
		httpClient=new QFHttpClient(arg0, false);
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		String bookingUrlPre = "http://booking.nordavia.ru/en/indexformprocessing";
		Map citymap=getCity();
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("post");
		Map<String, String> paramap = new LinkedHashMap<String, String>();
		paramap.put("origin-city-name", map.get(arg0.getArr()).toString());
		paramap.put("destination-city-name",map.get(arg0.getDep()).toString());
		paramap.put("there-date", arg0.getDepDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1"));
		paramap.put("back-date", arg0.getRetDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1"));
		paramap.put("count-aaa", "1");
		paramap.put("count-rbg", "0");
		paramap.put("count-rmg", "0");
		paramap.put("pricetable", "Continue");
		bookingInfo.setInputs(paramap);		
		bookingResult.setData(bookingInfo);
		bookingResult.setRet(true);
		return bookingResult;
	}

	@Override
	public String getHtml(FlightSearchParam arg0) {
		QFGetMethod get = null;	
		QFPostMethod post=null;
		try{
			httpClient=new QFHttpClient(arg0, false);
			map=getCity();
			httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			String postUrl="http://booking.nordavia.ru/en/indexformprocessing";
			post = new QFPostMethod(postUrl);
			NameValuePair[] pairs = new NameValuePair[]{
					new NameValuePair("origin-city-name", map.get(arg0.getDep()).toString()),//origin
					new NameValuePair("destination-city-name", map.get(arg0.getArr()).toString()),//destination
					new NameValuePair("there-date", arg0.getDepDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1")),
					new NameValuePair("back-date", arg0.getRetDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1")),
					new NameValuePair("use-back", "1"),
					new NameValuePair("count-aaa", "1"),
					new NameValuePair("count-rbg", "0"),
					new NameValuePair("count-rmg", "0"),
					new NameValuePair("pricetable", "Continue"),
			};
			post.setRequestBody(pairs);
			post.setFollowRedirects(false);
			int status=httpClient.executeMethod(post);
			String getUrl="http://booking.nordavia.ru/en/pricetable";
			String cookies=StringUtils.join(httpClient.getState().getCookies(),";");
			get = new QFGetMethod(getUrl);
			get.setRequestHeader("Cookie",cookies);
			status=httpClient.executeMethod(get);
			return get.getResponseBodyAsString();
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
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(arg0)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}		
		//需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (arg0.contains("Today Flight is full, select an other day or check later for any seat released. ")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;			
		}
		try{
			List<RoundTripFlightInfo> flightList = new ArrayList<RoundTripFlightInfo>();
			//价格json
			String[] priceVariants=StringUtils.substringBetween(arg0, "'card': {", "},").replace("\n", "").replace(" ", "").split("\\),");
			//航班json
			String[] flightVariants=StringUtils.substringBetween(arg0, "var flightVariants = {", "};").replace("\n", "").replace("'", "").replace(" ", "").split("}},");
			
			//航班信息信息
			String flightHtml=StringUtils.substringBetween(arg0, "<tbody", "</table>").replace("\n", "").trim();
			//返回航班信息
			String retFlightHtml=StringUtils.substringBetween(StringUtils.substringAfterLast(arg0, "<div class=\"headline\">"), "<tbody", "</table>").replace("\n", "").replace(" ", "");
			
			Map privceMap=new HashMap();
			//价格的KEY="78262425" Value="new Price('78262425', '5295.00', 'RUB', null, '782', false, true, '5N')"
			for(int i=0;i<priceVariants.length;i++){
				String []priceKV=priceVariants[i].replace("'", "").split(":");
				privceMap.put(priceKV[0], priceKV[1].trim()+")");
			}
			Map flightMap=new HashMap();
			for(int i=0;i<flightVariants.length;i++){
				String privceKey=StringUtils.substringBefore(flightVariants[i], ":");
				String[] flightlistStr=StringUtils.substringAfter(flightVariants[i]+"}", "{").split("},");
				for(int m=0;m<flightlistStr.length;m++){
					String forwardkey=StringUtils.substringBefore(flightlistStr[m], ":");
					String[] backkeys=StringUtils.substringBetween(flightlistStr[m]+"}", ":{","}").split(",");
					for(int j=0;j<backkeys.length;j++){
						String backkey=StringUtils.substringBefore(backkeys[j], ":");
						if(null!=privceMap.get(privceKey)&&null==flightMap.get(forwardkey+"||"+backkey)){
							flightMap.put(forwardkey+"||"+backkey, privceMap.get(privceKey));
						}
					}
				}
			}
			
//			for(int j=0;j<flightVariants.length;j++){
//				String privceKey=StringUtils.substringBefore(flightVariants[j], ":");
//				String[] flightKeyStr=StringUtils.substringAfter(flightVariants[j]+"}", "{").split(",");
//				for(int m=0;m<flightKeyStr.length;m++){
//					String[] flightKey=flightKeyStr[m].split(":");
//					System.out.println(flightKey[0].trim());
//					
//				}
//				
//			}
			String [] flights=flightHtml.split("</tbody>");
			for(int i=0;i<flights.length;i++){
				if(null!=flights[i]&&!"".equals(flights[i])){
					
					List<FlightSegement> segs = new ArrayList<FlightSegement>();
					List<String> flightNoList = new ArrayList<String>();
//					String[] info=null;
					String id="";
					String flightNo="";
					//判读往航是否中转
					if(!flights[i].contains("Route through city")){
					//获取价格ID
					id=StringUtils.substringBetween(flights[i],"id=\"","\"").replace("flight-tr-", "");
					String td[]=StringUtils.substringBetween(flights[i],"<td","</tr>").replace(" ", "").split("<td");
					
					flightNo=StringUtils.substringBetween(td[1], "<acronymtitle=\"NORDAVIARA\">", "</td>").replace("</acronym>-", "").trim();
					String flightdeptime=StringUtils.substringBetween(td[2], ">", "</td>");
					String flightarrtime=StringUtils.substringBetween(td[4], ">", "</td>");
					flightNoList.add(flightNo);
					FlightSegement seg = new FlightSegement();
					seg.setFlightno(flightNo);
					seg.setDepDate(arg1.getDepDate());
					seg.setCompany(flightNo.substring(0, 2));
					seg.setDepairport(arg1.getDep());
					seg.setArrairport(arg1.getArr());
					seg.setDeptime(flightdeptime);
					seg.setArrtime(flightarrtime);
					segs.add(seg);
				}else{
					String Jointedflights=StringUtils.substringAfter(flights[i],"<input");
					id=StringUtils.substringBetween(Jointedflights,"id=\"","\"");
					String jointedflightstr[]=StringUtils.substringAfter(Jointedflights, "</tr>").split("<tr>");
					for(int k=0;k<jointedflightstr.length;k++){
						if(null!=jointedflightstr[k]&&!"".equals(jointedflightstr[k])){
							FlightSegement seg = new FlightSegement();
							String jointedflightstd[]=jointedflightstr[k].split("<td");
							flightNo=StringUtils.substringBetween(jointedflightstd[1], "\"NORDAVIA RA\">", "</td>").replace("</acronym>-", "").trim();
							flightNoList.add(flightNo);
							if(!jointedflightstd[2].contains("up-small")){
								String flightdeptime=StringUtils.substringBetween(jointedflightstd[2], ">", "</td>");
								seg.setDeptime(flightdeptime.trim());
								seg.setDepDate(arg1.getDepDate());
							}else{
								String flightdeptime=StringUtils.substringBetween(jointedflightstd[2], "</span>", "</td>").trim();
								seg.setDeptime(flightdeptime.trim());
								String day=StringUtils.substringBetween(jointedflightstd[2], "up-small\">", "</span>").replace("+", "");
								Date depDate=Date.valueOf(arg1.getDepDate());
								Calendar cal = Calendar.getInstance();
								cal.setTime(Date.valueOf(arg1.getDepDate()));
								cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
								String dateString = formatter.format(cal.getTime());
								seg.setDepDate(dateString);
							}
							if(!jointedflightstd[4].contains("up-small")){
								String flightarrtime=StringUtils.substringBetween(jointedflightstd[4], ">", "</td>");
								seg.setArrtime(flightarrtime.trim());
								seg.setArrDate(arg1.getDepDate());
							}else{
								String flightarrtime=StringUtils.substringBetween(jointedflightstd[4], "</span>", "</td>");
								seg.setArrtime(flightarrtime.trim());
								String day=StringUtils.substringBetween(jointedflightstd[4], "up-small\">", "</span>").replace("+", "");
								Date depDate=Date.valueOf(arg1.getDepDate());
								Calendar cal = Calendar.getInstance();
								cal.setTime(Date.valueOf(arg1.getDepDate()));
								cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
								String dateString = formatter.format(cal.getTime());
								seg.setArrDate(dateString);
							}
							seg.setFlightno(flightNo);
							String dep=StringUtils.substringBetween(jointedflightstd[3], ">", "</td>").trim();
							String arr=StringUtils.substringBetween(jointedflightstd[5], ">", "</td>").trim();
							Boolean depflag=false;
							Boolean arrflag=false;
							 for (Map.Entry<String, String> entry : map.entrySet()) {
								 if(dep.equals(entry.getValue())){
									 seg.setDepairport(entry.getKey());
									 depflag=true;
									
								 }
								 if(arr.equals(entry.getValue())){
									 seg.setArrairport(entry.getKey());
									 arrflag=true;
									
								 }
							 }
							 if(!depflag){
								 seg.setDepairport(arg1.getDep()); 
							 }
							 if(!arrflag){
								 seg.setArrairport(arg1.getArr());
							 }
							seg.setCompany(flightNo.substring(0, 2));
							segs.add(seg);
						}
					}
					}
					//返航
					String [] retflights=retFlightHtml.replace("<!--Ð ÐµÐ¹ÑÑÑÑÐ´Ð°:ÐºÐ¾Ð½ÐµÑ-->", "").split("</tbody>");
					for(int k=0;k<retflights.length;k++){
						if(null!=retflights[k]&&!"".equals(retflights[k])){
						RoundTripFlightInfo baseFlight = new RoundTripFlightInfo();
						//判读往航是否中转
						if(!retflights[k].contains("Route through city")){
						List<String> retflightNoList = new ArrayList<String>();
						List<FlightSegement> retsegs = new ArrayList<FlightSegement>();
						//获取价格ID
						String retid=StringUtils.substringBetween(retflights[k],"id=\"","\"").replace("flight-tr-", "");
						String[] info=flightMap.get(id+"||"+retid).toString().replace("'", "").replace("newPrice(", "").replace(")", "").split(",");
						String rettd[]=StringUtils.substringBetween(retflights[k],"<td","</tr>").replace(" ", "").split("<td");
						String retflightNo=StringUtils.substringBetween(rettd[1], "<acronymtitle=\"NORDAVIARA\">", "</td>").replace("</acronym>-", "").trim();
						String retflightdeptime=StringUtils.substringBetween(rettd[2], ">", "</td>");
						String retflightarrtime=StringUtils.substringBetween(rettd[4], ">", "</td>");
						retflightNoList.add(retflightNo);
						FlightSegement retseg = new FlightSegement();
						retseg.setFlightno(retflightNo);
						retseg.setDepDate(arg1.getRetDate());
						retseg.setCompany(retflightNo.substring(0, 2));
						retseg.setDepairport(arg1.getArr());
						retseg.setArrairport(arg1.getDep());
						retseg.setDeptime(retflightdeptime);
						retseg.setArrtime(retflightarrtime);
						retsegs.add(retseg);
						
						FlightDetail flightDetail = new FlightDetail();
						flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
						flightDetail.setDepcity(arg1.getDep());
						flightDetail.setArrcity(arg1.getArr());
						flightDetail.setFlightno(flightNoList);
						flightDetail.setWrapperid(arg1.getWrapperid());
						flightDetail.setMonetaryunit(info[2]);
						flightDetail.setPrice(Double.valueOf(info[1]));
						flightDetail.setTax(0);
						baseFlight.setDetail(flightDetail);
						baseFlight.setInfo(segs);
//						baseFlight.setOutboundPrice(Double.valueOf(info[1]));
						baseFlight.setRetdepdate(Date.valueOf(arg1.getRetDate()));
						baseFlight.setRetflightno(retflightNoList);
						baseFlight.setRetinfo(retsegs);
//						baseFlight.setReturnedPrice(Double.valueOf(info[1]));
						flightList.add(baseFlight);
						}else{
							String retJointedflights=StringUtils.substringAfter(flights[k],"<input");
							String retid=StringUtils.substringBetween(retJointedflights,"id=\"","\"");
							String[] info=flightMap.get(id+"||"+retid).toString().replace("'", "").replace("newPrice(", "").replace(")", "").split(",");
							String retjointedflightstr[]=StringUtils.substringAfter(retJointedflights, "</tr>").split("<tr>");
							List<String> retflightNoList = new ArrayList<String>();
							List<FlightSegement> retsegs = new ArrayList<FlightSegement>();
							String retjointedflightstd[]=retjointedflightstr[k].split("<td");
							String retflightNo=StringUtils.substringBetween(retjointedflightstd[1], "\"NORDAVIA RA\">", "</td>").replace("</acronym>-", "").trim();
							retflightNoList.add(retflightNo);
							FlightSegement retseg = new FlightSegement();
							if(!retjointedflightstd[2].contains("up-small")){
								String retflightdeptime=StringUtils.substringBetween(retjointedflightstd[2], ">", "</td>");
								retseg.setDeptime(retflightdeptime.trim());
								retseg.setDepDate(arg1.getRetDate());
							}else{
								String retflightdeptime=StringUtils.substringBetween(retjointedflightstd[2], "</span>", "</td>").trim();
								retseg.setDeptime(retflightdeptime.trim());
								String day=StringUtils.substringBetween(retjointedflightstd[2], "up-small\">", "</span>").replace("+", "");
								Calendar cal = Calendar.getInstance();
								cal.setTime(Date.valueOf(arg1.getRetDate()));
								cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
								String dateString = formatter.format(cal.getTime());
								retseg.setDepDate(dateString);
							}
							if(!retjointedflightstd[4].contains("up-small")){
								String retflightarrtime=StringUtils.substringBetween(retjointedflightstd[4], ">", "</td>");
								retseg.setArrtime(retflightarrtime.trim());
								retseg.setArrDate(arg1.getRetDate());
							}else{
								String retflightarrtime=StringUtils.substringBetween(retjointedflightstd[4], "</span>", "</td>");
								retseg.setArrtime(retflightarrtime.trim());
								String day=StringUtils.substringBetween(retjointedflightstd[4], "up-small\">", "</span>").replace("+", "");
								Calendar cal = Calendar.getInstance();
								cal.setTime(Date.valueOf(arg1.getRetDate()));
								cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
								String dateString = formatter.format(cal.getTime());
								retseg.setArrDate(dateString);
							}
							retseg.setFlightno(retflightNo);
							String dep=StringUtils.substringBetween(retjointedflightstd[3], ">", "</td>").trim();
							String arr=StringUtils.substringBetween(retjointedflightstd[5], ">", "</td>").trim();
							Boolean depflag=false;
							Boolean arrflag=false;
							 for (Map.Entry<String, String> entry : map.entrySet()) {
								 if(dep.equals(entry.getValue())){
									 retseg.setDepairport(entry.getKey());
									 depflag=true;
									
								 }
								 if(arr.equals(entry.getValue())){
									 retseg.setArrairport(entry.getKey());
									 arrflag=true;
									
								 }
							 }
							 if(!depflag){
								 retseg.setDepairport(arg1.getDep()); 
							 }
							 if(!arrflag){
								 retseg.setArrairport(arg1.getArr());
							 }
							retseg.setCompany(retflightNo.substring(0, 2));
							retsegs.add(retseg);
							FlightDetail flightDetail = new FlightDetail();
							flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
							flightDetail.setDepcity(arg1.getDep());
							flightDetail.setArrcity(arg1.getArr());
							flightDetail.setFlightno(flightNoList);
							flightDetail.setWrapperid(arg1.getWrapperid());
							flightDetail.setMonetaryunit(info[2]);
							flightDetail.setPrice(Double.valueOf(info[1]));
							flightDetail.setTax(0);
							
							baseFlight.setDetail(flightDetail);
							baseFlight.setInfo(segs);
//							baseFlight.setOutboundPrice(Double.valueOf(info[1]));
							baseFlight.setRetdepdate(Date.valueOf(arg1.getRetDate()));
							baseFlight.setRetflightno(retflightNoList);
							baseFlight.setRetinfo(retsegs);
//							baseFlight.setReturnedPrice(Double.valueOf(info[1]));
							flightList.add(baseFlight);
						}
						}
					}
					
				}
			}
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		}catch (Exception e) {
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	public String resquestCity() {
		QFGetMethod get = null;
		try {
			String getUrl = String
					.format("http://booking.nordavia.ru/en/json/dependence-cities?param=origin&type=json");
			get = new QFGetMethod(getUrl);
			get.getParams().setContentCharset("utf-8");
			int status = httpClient.executeMethod(get);
			return get.getResponseBodyAsString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != get) {
				get.releaseConnection();
			}
		}
		return "Exception";
	}
//	public Map getCity() {
//		String html = resquestCity();
//		map = new HashMap();
//		String[] results_html = StringUtils.substringAfterLast(html, "{").replace("}", "").replace("\"", "").split(",");
//		for (int i = 0; i < results_html.length - 1; i++) {
//			String[] array= results_html[i].trim().split(":");
//			map.put(array[1].trim(), array[0].trim());
//		}
//		return map;
//	}
	public Map getCity(){
		map = new HashMap();
		map.put("KRR", "Krasnodar");
		map.put("AMV", "Amderma");
		map.put("AAQ", "Anapa");
		map.put("ARH", "Arkhangelsk");
		map.put("MMK", "Murmansk");
		map.put("NNM", "Naryan-Mar");
		map.put("LED", "St Petersburg");
		map.put("AER", "Sochi");
		map.put("SCW", "Syktyvkar");
		map.put("TOS", "Tromso");
		map.put("KGD", "Kaliningrad");
		map.put("MOW", "Moscow");
		map.put("CSH", "Solovetsky");
		return map;
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("ARH");
		searchParam.setArr("MOW");
		searchParam.setDepDate("2014-07-18");
		searchParam.setRetDate("2014-07-22");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjsair5n001");
		searchParam.setToken("");
		Wrapper_gjsair5n001 gjsair5n001=new  Wrapper_gjsair5n001();
		String html = gjsair5n001.getHtml(searchParam);
//		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = gjsair5n001.process(html,searchParam);
		if(result.isRet() && result.getStatus().equals(Constants.SUCCESS))
		{
			List<RoundTripFlightInfo> flightList = (List<RoundTripFlightInfo>) result.getData();
			for (RoundTripFlightInfo in : flightList){
				System.out.println("------------" + in.getDetail());
				System.out.println("************" + in.getInfo().toString());
				System.out.println("++++++++++++" + in.getRetinfo().toString());
			}
		}
		else
		{
			System.out.println(result.getStatus());
		}		
	}
}
