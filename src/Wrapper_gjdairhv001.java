import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
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
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;

/**
 * 荷兰泛航航空单航机票抓取
 * @author fangbin
 *
 */
public class Wrapper_gjdairhv001 implements QunarCrawler{

	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "http://www.transavia.com/hv/main/nav/processflightqry";
		BookingResult bookingResult = new BookingResult();
		String[] depDate=arg0.getDepDate().split("-");
		String fromMonth=depDate[0]+"-"+depDate[1];
		String fromDay=depDate[2];
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("post");
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("lang", "en");
		map.put("country", "EU");
		map.put("single", "true");
		map.put("adults", "1");
		map.put("children", "0");
		map.put("infants", "0");
		map.put("from", arg0.getDep());
		map.put("ojTo", "");
		map.put("fromDay", fromDay);
		map.put("fromMonth", fromMonth);
		map.put("to", arg0.getArr());
		map.put("ojFrom", "");
		map.put("trip", "single");
		map.put("jsform", "true");
		
		bookingInfo.setInputs(map);		
		bookingResult.setData(bookingInfo);
		bookingResult.setRet(true);
		return bookingResult;
	}

	@Override
	public String getHtml(FlightSearchParam arg0) {
		QFPostMethod post=null;
		try {	
		QFHttpClient httpClient = new QFHttpClient(arg0, false);
			post=new QFPostMethod("http://www.transavia.com/hv/main/nav/processflightqry");
			String[] depDate=arg0.getDepDate().split("-");
			String fromMonth=depDate[0]+"-"+depDate[1];
			String fromDay=depDate[2];
			NameValuePair[] pairs = new NameValuePair[]{
				     new NameValuePair("adults", "1"),
				     new NameValuePair("single", "true"),
				     new NameValuePair("from", arg0.getDep()),
				     new NameValuePair("to", arg0.getArr()),
				     new NameValuePair("fromMonth", fromMonth),
				     new NameValuePair("fromDay", fromDay),
				     new NameValuePair("trip", "single"),
				     new NameValuePair("children", "0"),
				     new NameValuePair("lang", "en"),
				     new NameValuePair("country", "EU"),
				     new NameValuePair("ie6view", "on"),
				     new NameValuePair("infants", "0"),
				     new NameValuePair("jsform", "true"),
				   };
			post.setRequestBody(pairs);
		    int status = httpClient.executeMethod(post);
		    return post.getResponseBodyAsString();
		} catch (Exception e) {			
			e.printStackTrace();
		} finally{
			if (null != post){
				post.releaseConnection();
			}
		}
		return "Exception";
	}

	
	@Override
	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String html = arg0;
		
		/* ProcessResultInfo中，
		 * ret为true时，status可以为：SUCCESS(抓取到机票价格)|NO_RESULT(无结果，没有可卖的机票)
		 * ret为false时，status可以为:CONNECTION_FAIL|INVALID_DATE|INVALID_AIRLINE|PARSING_FAIL|PARAM_ERROR
		 */
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(html)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}		
		//需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (html.contains("Today Flight is full, select an other day or check later for any seat released. ")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;			
		}
		try {	
		//抽近一周的航班列表区域
		String ht=StringUtils.substringBetween(html,"<div id=\"hc\">", "<div class=\"clear\">");
		if(null!=ht){
			String[] baseinfo=ht.substring(ht.lastIndexOf("</div>")+6, ht.length()).split(">");
			String theToCarrierCode="";
			for(int i=0 ;i<baseinfo.length;i++){
//				if(baseinfo[i].contains("theToDeparture")){
//					theToDeparture=StringUtils.substringBetween(baseinfo[i],"value=\"", "\"");
//				}
//				if(baseinfo[i].contains("theToArrival")){
//					theToArrival=StringUtils.substringBetween(baseinfo[i],"value=\"", "\"");
//				}
				if(baseinfo[i].contains("theToCarrierCode")){
					theToCarrierCode=StringUtils.substringBetween(baseinfo[i],"value=\"", "\"");
					break;
				}
			}
			List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
			//获取每天的航班信息
			String []flightday=ht.split("<div class=\"flightday\">");
			for(int i=0 ;i<flightday.length;i++){
				//获取查询当天的航班信息
				if(flightday[i].contains("checked")){
					String sub2=StringUtils.substringBetween(flightday[i],"<li class=\"selec", "</ul>");
					if(null!=sub2){
						List<FlightSegement> segs = new ArrayList<FlightSegement>();
						OneWayFlightInfo baseFlight = new OneWayFlightInfo();
						FlightDetail flightDetail = new FlightDetail();
						//获取当天的航班信息列表
						String []sp2=sub2.split("<li class=\"\">");
						for(int j=0;j<sp2.length;j++){
							String flightdate=StringUtils.substringBetween(sp2[j],"name=\"flightdate\" value=\"","\"");
							String flightdeptime=StringUtils.substringBetween(sp2[j],"name=\"flightdeptime\" value=\"","\"");
							String flightarrtime=StringUtils.substringBetween(sp2[j],"name=\"flightarrtime\" value=\"","\"");
							String number=StringUtils.substringBetween(sp2[j],"<label for=\""+flightdate+"|","\"");
							String price=StringUtils.substringBetween(sp2[j],"<strong>","</strong>");
							
							
							List<String> flightNoList = new ArrayList<String>();
							flightNoList.add(theToCarrierCode+number);
							
							FlightSegement seg = new FlightSegement();
							seg.setFlightno(theToCarrierCode+number);
							seg.setDepDate(flightdate);
							seg.setDepairport(arg1.getDep());
							seg.setArrairport(arg1.getArr());
							seg.setDeptime(flightdeptime);
							seg.setArrtime(flightarrtime);
							segs.add(seg);
							
							flightDetail.setMonetaryunit("EUR");
							flightDetail.setPrice(Math.round(Double.parseDouble(price)-Double.parseDouble("21.35")));
							flightDetail.setDepcity(arg1.getDep());
							flightDetail.setArrcity(arg1.getArr());
							flightDetail.setWrapperid(arg1.getWrapperid());
							flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
							flightDetail.setFlightno(flightNoList);
							flightDetail.setTax(Math.round(Double.parseDouble("22.35")));
							baseFlight.setDetail(flightDetail);
							baseFlight.setInfo(segs);
							flightList.add(baseFlight);
						}
					}else{
						result.setRet(false);
						result.setStatus(Constants.CONNECTION_FAIL);
						return result;
					}
					break;
				}
				
			}
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		}else{
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
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
		searchParam.setDep("AMS");
		searchParam.setArr("ATH");
		searchParam.setDepDate("2014-06-27");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjdairhv001");
		searchParam.setToken("");
		String html = new  Wrapper_gjdairhv001().getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = new  Wrapper_gjdairhv001().process(html,searchParam);
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
