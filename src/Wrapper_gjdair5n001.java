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
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;
import com.travelco.rdf.infocenter.InfoCenter;
/**
 * 诺达维亚航空
 * http://www.nordavia.ru/en/
 * @author Administrator
 *
 */

public class Wrapper_gjdair5n001 implements QunarCrawler {
	QFHttpClient httpClient = null;
	Map<String,String> map=null;
	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		BookingResult bookingResult = new BookingResult();
//		httpClient=new QFHttpClient(arg0, false);
//		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		String bookingUrlPre = "http://booking.nordavia.ru/en/indexformprocessing";
		map=getCity();
		String depCity="";
		String arrCity="";
		if(null!=map.get(arg0.getDep())){
			depCity=map.get(arg0.getDep());
		}else{
			depCity=InfoCenter.getCityFromAnyCode(arg0.getDep(), "en");
			if(depCity.length()==3){
				depCity=InfoCenter.getCityFromAnyCode(depCity, "en");
			}
		}
		if(null!=map.get(arg0.getArr())){
			arrCity=map.get(arg0.getArr());
		}else{
			arrCity=InfoCenter.getCityFromAnyCode(arg0.getArr(), "en");
			if(arrCity.length()==3){
				arrCity=InfoCenter.getCityFromAnyCode(arrCity, "en");
			}
		}
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("post");
		Map<String, String> paramap = new LinkedHashMap<String, String>();
		paramap.put("origin-city-name", depCity);
		paramap.put("destination-city-name",arrCity);
		paramap.put("there-date", arg0.getDepDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1"));
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
			httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			map=getCity();
			String depCity="";
			String arrCity="";
			if(null!=map.get(arg0.getDep())){
				depCity=map.get(arg0.getDep());
			}else{
				depCity=InfoCenter.getCityFromAnyCode(arg0.getDep(), "en");
				if(depCity.length()==3){
					depCity=InfoCenter.getCityFromAnyCode(depCity, "en");
				}
			}
			if(null!=map.get(arg0.getArr())){
				arrCity=map.get(arg0.getArr());
			}else{
				arrCity=InfoCenter.getCityFromAnyCode(arg0.getArr(), "en");
				if(arrCity.length()==3){
					arrCity=InfoCenter.getCityFromAnyCode(arrCity, "en");
				}
			}
			String postUrl="http://booking.nordavia.ru/en/indexformprocessing";
			
			post = new QFPostMethod(postUrl);
			NameValuePair[] pairs = new NameValuePair[]{
					new NameValuePair("origin-city-name", depCity),//origin
					new NameValuePair("destination-city-name", arrCity),//destination
					new NameValuePair("there-date", arg0.getDepDate().replaceAll("(....)-(..)-(..)", "$3.$2.$1")),
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
		List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
		//价格json
		String[] priceVariants=(StringUtils.substringBetween(arg0, "'card': {", "},").replace("\n", "").replace(" ", "")).split("\\),");
		//航班json
		String[] flightVariants=StringUtils.substringBetween(arg0, "var flightVariants = {", "};").replace("\n", "").replace("'", "").replace(" ", "").split("}},");
		//航班信息信息
		String flightHtml=StringUtils.substringBetween(arg0, "<tbody", "</table>").replace("\n", "").trim();
		
		//价格的KEY="78262425" Value="new Price('78262425', '5295.00', 'RUB', null, '782', false, true, '5N')"
		Map privceMap=new HashMap();
		for(int i=0;i<priceVariants.length;i++){
			String []priceKV=priceVariants[i].replace("'", "").split(":");
			privceMap.put(priceKV[0], priceKV[1].trim()+")");
		}
		Map flightMap=new HashMap();
		for(int j=0;j<flightVariants.length;j++){
			String privceKey=StringUtils.substringBefore(flightVariants[j], ":");
			String[] flightKeyStr=StringUtils.substringAfter(flightVariants[j], "{").split(",");
			for(int m=0;m<flightKeyStr.length;m++){
				String[] flightKey=flightKeyStr[m].split(":");
				if(null!=privceMap.get(privceKey)&&null==flightMap.get(flightKey[0])){
					flightMap.put(flightKey[0].trim(), privceMap.get(privceKey));
				}
			}
			
		}
		String [] flights=flightHtml.replace("<!-- Ð ÐµÐ¹ÑÑ ÑÑÐ´Ð° : ÐºÐ¾Ð½ÐµÑ-->", "").split("</tbody>");
		for(int i=0;i<flights.length;i++){
			if(null!=flights[i]&&!"".equals(flights[i].replace(" ", ""))){
				//判读是否中转
				if(!flights[i].contains("Route through city")){
					List<FlightSegement> segs = new ArrayList<FlightSegement>();
					OneWayFlightInfo baseFlight = new OneWayFlightInfo();
					//获取价格ID
					String id=StringUtils.substringBetween(flights[i],"id=\"","\"").replace("flight-tr-", "");
					String[] info=flightMap.get(id).toString().replace("'", "").replace("newPrice(", "").replace(")", "").split(",");
					String td[]=StringUtils.substringBetween(flights[i],"<td","</tr>").replace(" ", "").split("<td");
					List<String> flightNoList = new ArrayList<String>();
					String flightNo=StringUtils.substringBetween(td[1], "<acronymtitle=\"NORDAVIARA\">", "</td>").replace("</acronym>-", "").trim();
					String flightdeptime=StringUtils.substringBetween(td[2], ">", "</td>");
					String flightarrtime=StringUtils.substringBetween(td[4], ">", "</td>");
					flightNoList.add(flightNo);
					FlightSegement seg = new FlightSegement();
					seg.setFlightno(flightNo);
					seg.setDepDate(arg1.getDepDate());
					if(!td[4].contains("up-small")){
						seg.setArrDate(arg1.getDepDate());
					}else{
						String day=StringUtils.substringBetween(td[4], "up-small\">", "</span>").replace("+", "");
						Date depDate=Date.valueOf(arg1.getDepDate());
						Calendar cal = Calendar.getInstance();
						cal.setTime(Date.valueOf(arg1.getDepDate()));
						cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						String dateString = formatter.format(cal.getTime());
						seg.setArrDate(dateString);
					}
					seg.setCompany(flightNo.substring(0, 2));
					seg.setDepairport(arg1.getDep());
					seg.setArrairport(arg1.getArr());
					seg.setDeptime(flightdeptime);
					seg.setArrtime(flightarrtime);
					segs.add(seg);
					
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
					flightList.add(baseFlight);
				}else{
					List<FlightSegement> segs = new ArrayList<FlightSegement>();
					OneWayFlightInfo baseFlight = new OneWayFlightInfo();
					String Jointedflights=StringUtils.substringAfter(flights[i],"<input");
					String id=StringUtils.substringBetween(Jointedflights,"id=\"","\"");
					String[] info=flightMap.get(id).toString().replace("'", "").replace("newPrice(", "").replace(")", "").split(",");
					String jointedflightstr[]=StringUtils.substringAfter(Jointedflights, "</tr>").split("<tr>");
					List<String> flightNoList = new ArrayList<String>();
					for(int k=0;k<jointedflightstr.length;k++){
						if(null!=jointedflightstr[k]&&!"".equals(jointedflightstr[k])){
							FlightSegement seg = new FlightSegement();
							String jointedflightstd[]=jointedflightstr[k].split("<td");
							String flightNo=StringUtils.substringBetween(jointedflightstd[1], "\"NORDAVIA RA\">", "</td>").replace("</acronym>-", "").trim();
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
					flightList.add(baseFlight);
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
//		Map citymap = new HashMap();
//		String[] results_html = StringUtils.substringAfterLast(html, "{").replace("}", "").replace("\"", "").split(",");
//		for (int i = 0; i < results_html.length - 1; i++) {
//			String[] array= results_html[i].trim().split(":");
//			citymap.put(array[1].trim(), array[0].trim());
//		}
//		//{KRR=Krasnodar, AMV=Amderma, AAQ=Anapa, ARH=Arkhangelsk, MMK=Murmansk, NNM=Naryan-Mar, LED=St Petersburg, AER=Sochi, SCW=Syktyvkar, TOS=Tromso, KGD=Kaliningrad, MOW=Moscow, CSH=Solovetsky}
//		return citymap;
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
		searchParam.setDepDate("2014-09-07");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjdair5n001");
		searchParam.setToken("");
		Wrapper_gjdair5n001 gjdair5n001=new  Wrapper_gjdair5n001();
		String html = gjdair5n001.getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = gjdair5n001.process(html,searchParam);
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
