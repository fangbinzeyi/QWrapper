import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
/**
 * 中国东方航空
 * http://tw.ceair.com/
 */

public class Wrapper_gjsairmu003 implements QunarCrawler {
	QFHttpClient httpClient =null;
	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "http://tw.ceair.com/muovc/front/reservation/flight-search!doFlightSearch.shtml";
		BookingResult bookingResult = new BookingResult();
		
		try {
			BookingInfo bookingInfo = new BookingInfo();
			bookingInfo.setAction(bookingUrlPre);
			bookingInfo.setMethod("get");
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("cond.tripType", "RT");
			map.put("cond.depCode",  arg0.getDep());
//			map.put("cond.depCode_reveal",  "東京成田");
			map.put("cond.arrCode", arg0.getArr());
//			map.put("cond.arrCode_reveal", "北京首都機場");
			map.put("cond.routeType", "3");
			map.put("cond.cabinRank", "ECONOMY");
			map.put("depDate", arg0.getDepDate());
			map.put("depRtDate", arg0.getRetDate());
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
		try {
			httpClient = new QFHttpClient(arg0, false);
			httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			String getUrl = String.format("http://tw.ceair.com/muovc/front/reservation/flight-search!doFlightSearch.shtml?cond.tripType=RT&cond.depCode=%s&cond.arrCode=%s&cond.routeType=3&depDate=%s&depRtDate=%s&cond.cabinRank=ECONOMY", arg0.getDep(),arg0.getArr(),arg0.getDepDate(),arg0.getRetDate());
			get = new QFGetMethod(getUrl);
			int status=httpClient.executeMethod(get);
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
		String deptable= StringUtils.substringBetween(arg0, "<div class=\"routing\">","</table>");
		deptable=StringUtils.substringAfterLast(deptable, "</thead>").replace("\r\n","").trim();
		String rettable=StringUtils.substringAfterLast(arg0, "<div class=\"flight_table rt_back\"").replace("\r\n","").trim();
		List<RoundTripFlightInfo> flightList = new ArrayList<RoundTripFlightInfo>();
		try {
			if(null!=deptable&&!"".equals(deptable)){
				Map cityMap=getCity();
				String deptbody[]=deptable.split("<tbody>");
				for(int i=0;i<deptbody.length;i++){
					if(null!=deptbody[i]&&!"".equals(deptbody[i])){
						String [] deptr=deptbody[i].substring(0, deptbody[i].indexOf("<tr class=\"detail\">")-19).trim().split("<tr class=\"booking\">");
						List<FlightSegement> tosegs = new ArrayList<FlightSegement>();
						List<String> flightNoList = new ArrayList<String>();
						Double prices=0d;
						Double taxs=0d;
						String monetaryunit="";
						for(int j=0;j<deptr.length;j++){
							if(null!=deptr[j]&&!"".equals(deptr[j])){
								String [] deptd=deptr[j].trim().split("<td");
								String[] depdatetime=null;
								String[] arrdatetime=null;
								String depflightNo="";
								FlightSegement seg = new FlightSegement();
								if(j==1){
									deptd=deptr[j].trim().split("<td");
									String depDateTimeStr=StringUtils.substringBetween(deptd[2], "/>", "</td>").trim();
									String arrDateTimeStr=StringUtils.substringBetween(deptd[3], "/>", "</td>").trim();
									depdatetime=depDateTimeStr.split(" ");
									arrdatetime=arrDateTimeStr.split(" ");
									//获取起飞城市
									String depairport=StringUtils.substringBetween(deptd[5],">","</td>").replace("\n", "").trim();
									seg.setDepairport(cityMap.get(depairport).toString());
									//获取到达城市
									String arrairport=StringUtils.substringBetween(deptd[6],">","</td>").replace("\n", "").trim();
									seg.setArrairport(cityMap.get(arrairport).toString());
									depflightNo=StringUtils.substringBetween(deptd[4], "/>", "</td>").trim();
									String depprice="0";
									String deptax="0";
									if(!deptd[7].contains("<span> - </span>")){
										if("".equals(monetaryunit)){
											monetaryunit=StringUtils.substringBetween(deptd[7],"/>","<span");
										}
										String[] span=deptd[7].split("<span");
										depprice=StringUtils.substringBetween(span[2],"\">","</span>");
										if(null!=depprice&&!"".equals(depprice)){
											prices+=Double.parseDouble(depprice);
										}
										deptax=StringUtils.substringBetween(span[3],"\">","</span>");
										if(null!=deptax&&!"".equals(deptax)){
											taxs+=Double.parseDouble(deptax);
										}
										
									}else if(!deptd[8].contains("<span> - </span>")){
										if("".equals(monetaryunit)){
											monetaryunit=StringUtils.substringBetween(deptd[8],"/>","<span");
										}
										String[] span=deptd[8].split("<span");
										depprice=StringUtils.substringBetween(span[2],"\">","</span>");
										deptax=StringUtils.substringBetween(span[3],"\">","</span>");
										if(null!=depprice&&!"".equals(depprice)){
											prices+=Double.parseDouble(depprice.replace(",", ""));
										}
										if(null!=deptax&&!"".equals(deptax)){
											taxs+=Double.parseDouble(deptax.replace(",", ""));
										}
									}else{
										if(j==0){
											result.setRet(false);
											result.setStatus(Constants.PARSING_FAIL);
											return result;
										}
									}
								}else{
									//获取起飞城市
									String depairport=StringUtils.substringBetween(deptd[4],">","</td>").replace("\n", "").trim();
									seg.setDepairport(cityMap.get(depairport).toString());
									//获取到达城市
									String arrairport=StringUtils.substringBetween(deptd[5],">","</td>").replace("\n", "").trim();
									seg.setArrairport(cityMap.get(arrairport).toString());
									String depDateTimeStr=StringUtils.substringBetween(deptd[1], "/>", "</td>").trim();
									depdatetime=depDateTimeStr.split(" ");
									String arrDateTimeStr=StringUtils.substringBetween(deptd[2], "/>", "</td>").trim();
									arrdatetime=arrDateTimeStr.split(" ");
									depflightNo=StringUtils.substringBetween(deptd[3], "/>", "</td>").trim();
								}
								
								flightNoList.add(depflightNo);
								seg.setFlightno(depflightNo);
								String depdate=arg1.getDepDate().substring(0, 4)+"年"+depdatetime[1];
								String arrdate=arg1.getDepDate().substring(0, 4)+"年"+arrdatetime[1];
								seg.setDepDate(this.strFormat(depdate,"yyyy年MM月dd日","yyyy-MM-dd"));
								seg.setArrDate(this.strFormat(arrdate,"yyyy年MM月dd日","yyyy-MM-dd"));
								seg.setDeptime(depdatetime[0].trim());
								seg.setArrtime(arrdatetime[0].trim());
								seg.setCompany(depflightNo.substring(0, 2));
//								seg.setArrDate(arrdatetime[1].trim());
								tosegs.add(seg);
								
							}
						}
						//返回
						String[] retdiv=rettable.split("<div class=\"flight_table rt_back\"");
						for(int m=0;m<retdiv.length;m++){
							if(null!=retdiv[m]&&!"".equals(retdiv[m])){
								
								String retbody[]=StringUtils.substringAfterLast(retdiv[m], "</thead>").replace("\r\n","").trim().split("<tbody>");
								for(int z=0;z<retbody.length;z++){
								if(null!=retbody[z]&&!"".equals(retbody[z])){
								List<FlightSegement> retsegs = new ArrayList<FlightSegement>();
								List<String> retflightNoList = new ArrayList<String>();
								String [] rettr=retbody[z].substring(0, retbody[z].indexOf("<tr class=\"detail\">")-19).trim().split("<tr class=\"booking\">");
								Double retprices=0d;
								Double rettaxs=0d;
								for(int k=0;k<rettr.length;k++){
									if(null!=rettr[k]&&!"".equals(rettr[k])){
										String [] rettd=rettr[k].trim().split("<td");
										String[] retdatetime=null;
										String[] retarrdatetime=null;
										String retflightNo="";
										FlightSegement retseg = new FlightSegement();
										if(k==1){
											String retDateTimeStr=StringUtils.substringBetween(rettd[2], "/>", "</td>").trim();
											retdatetime=retDateTimeStr.split(" ");
											String retarrDateTimeStr=StringUtils.substringBetween(rettd[3], "/>", "</td>").trim();
											retarrdatetime=retarrDateTimeStr.split(" ");
											retflightNo=StringUtils.substringBetween(rettd[4], "/>", "</td>").trim();
											//获取起飞城市
											String depairport=StringUtils.substringBetween(rettd[5],">","</td>").replace("\n", "").trim();
											retseg.setDepairport(cityMap.get(depairport).toString());
											//获取到达城市
											String arrairport=StringUtils.substringBetween(rettd[6],">","</td>").replace("\n", "").trim();
											retseg.setArrairport(cityMap.get(arrairport).toString());
											String retprice="0";
											String rettax="0";
											
											if(!rettd[7].contains("<span> - </span>")){
												String[] span=rettd[7].split("<span");
												retprice=StringUtils.substringBetween(span[2],"\">","</span>");
												if(null!=retprice&&!"".equals(retprice)){
													retprices+=Double.parseDouble(retprice);
												}
												rettax=StringUtils.substringBetween(span[3],"\">","</span>");
												if(null!=rettax&&!"".equals(rettax)){
													rettax+=Double.parseDouble(rettax);
												}
												
											}else if(!rettd[8].contains("<span> - </span>")){
												String[] span=rettd[8].split("<span");
												retprice=StringUtils.substringBetween(span[2],"\">","</span>");
												rettax=StringUtils.substringBetween(span[3],"\">","</span>");
												if(null!=retprice&&!"".equals(retprice)){
													retprices+=Double.parseDouble(retprice.replace(",", ""));
												}
												if(null!=rettax&&!"".equals(rettax)){
													rettax+=Double.parseDouble(rettax.replace(",", ""));
												}
											}
										}else{
											String retDateTimeStr=StringUtils.substringBetween(rettd[1], "/>", "</td>").trim();
											retdatetime=retDateTimeStr.split(" ");
											String retarrDateTimeStr=StringUtils.substringBetween(rettd[2], "/>", "</td>").trim();
											retarrdatetime=retarrDateTimeStr.split(" ");
											retflightNo=StringUtils.substringBetween(rettd[3], "/>", "</td>").trim();
											//获取起飞城市
											String depairport=StringUtils.substringBetween(rettd[4],">","</td>").replace("\n", "").trim();
											retseg.setDepairport(cityMap.get(depairport).toString());
											//获取到达城市
											String arrairport=StringUtils.substringBetween(rettd[5],">","</td>").replace("\n", "").trim();
											retseg.setArrairport(cityMap.get(arrairport).toString());
										}
										retflightNoList.add(retflightNo);
										retseg.setFlightno(retflightNo);
										String depdate=arg1.getRetDate().substring(0, 4)+"年"+retdatetime[1];
										String arrdate=arg1.getRetDate().substring(0, 4)+"年"+retarrdatetime[1];
										retseg.setDepDate(this.strFormat(depdate,"yyyy年MM月dd日","yyyy-MM-dd"));
										retseg.setArrDate(this.strFormat(arrdate,"yyyy年MM月dd日","yyyy-MM-dd"));
										retseg.setDepDate(arg1.getRetDate());
										retseg.setDeptime(retdatetime[0].trim());
										retseg.setArrtime(retarrdatetime[0].trim());
										retseg.setCompany(retflightNo.substring(0, 2));
//										seg.setArrDate(arrdatetime[1].trim());
										retsegs.add(retseg);
										
										
									}
								}
								RoundTripFlightInfo baseFlight = new RoundTripFlightInfo();
								FlightDetail toflightDetail = new FlightDetail();
								toflightDetail.setArrcity(arg1.getArr());
								toflightDetail.setDepcity(arg1.getDep());
								toflightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
								toflightDetail.setFlightno(flightNoList);
								toflightDetail.setMonetaryunit("TWD");
								toflightDetail.setWrapperid(arg1.getWrapperid());
								toflightDetail.setPrice(retprices+prices);
								toflightDetail.setTax(rettaxs+taxs);
								baseFlight.setDetail(toflightDetail);
								baseFlight.setInfo(tosegs);
								baseFlight.setOutboundPrice(prices);
								baseFlight.setRetdepdate(Date.valueOf(arg1.getRetDate()));
								baseFlight.setRetflightno(retflightNoList);
								baseFlight.setRetinfo(retsegs);
								baseFlight.setReturnedPrice(retprices);
								flightList.add(baseFlight);
								}
								}	
								break;
							}
							
						}
					}
				}
				result.setRet(true);
				result.setStatus(Constants.SUCCESS);
				result.setData(flightList);	
				return result;
			}else{
				result.setRet(false);
				result.setStatus(Constants.NO_RESULT);
				return result;	
			}
		} catch(Exception e){
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}

	/**
	 * 二次请求获取机场三字码
	 * 
	 * @param radio_id
	 * @return
	 */
	public String resquestCity() {
		QFGetMethod get = null;
		try {
			String getUrl = String
					.format("http://ca.ceair.com/muovc/resource/zh_TW/js/city.js");
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

	public Map getCity() {

		String html = resquestCity();
		Map map = new HashMap();
		String[] results_html = StringUtils
				.substringAfter(html, "var _cityData =").replace("+\n", "")
				.split(";\"+");
		for (int i = 0; i < results_html.length - 1; i++) {
			String[] array_str = results_html[i].trim().split("\\|");
			String[] array = array_str[0].split(":");
			map.put(array[1], array[0].replace("\"", ""));
		}
		return map;
	}
	
	public String strFormat(String datestr,String sFormat,String newFormat){
		SimpleDateFormat formatter = new SimpleDateFormat(sFormat);
		SimpleDateFormat newformatter = new SimpleDateFormat(newFormat);
		String dateString ="";
		try {
			java.util.Date theDate = formatter.parse(datestr);
			dateString = newformatter.format(theDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateString;
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("NRT");
		searchParam.setArr("PEK");
		searchParam.setDepDate("2014-08-16");
		searchParam.setRetDate("2014-08-23");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjsairmu003");
		searchParam.setToken("");
		Wrapper_gjsairmu003 gjsairmu003=new Wrapper_gjsairmu003();
		String html = gjsairmu003.getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = gjsairmu003.process(html,searchParam);
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
