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
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.RoundTripFlightInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;

/**
 * 荷兰泛航航空往返机票抓取
 * @author fangbin
 *
 */
public class Wrapper_gjsairhv001 implements QunarCrawler {

	@Override
	public BookingResult getBookingInfo(FlightSearchParam flightsearchparam) {
		String bookingUrlPre = "http://www.transavia.com/hv/main/nav/processflightqry";
		BookingResult bookingResult = new BookingResult();
		String[] depDate=flightsearchparam.getDepDate().split("-");
		String fromMonth=depDate[0]+"-"+depDate[1];
		String fromDay=depDate[2];
		String[] reDate=flightsearchparam.getRetDate().split("-");
		String toMonth=reDate[0]+"-"+reDate[1];
		String toDay=reDate[2];
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("post");
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("lang", "en");
		map.put("country", "EU");
		map.put("adults", "1");
		map.put("children", "0");
		map.put("infants", "0");
		map.put("from", flightsearchparam.getDep());
		map.put("ojTo", "");
		map.put("toDay", toDay);
		map.put("toMonth", toMonth);
		map.put("fromDay", fromDay);
		map.put("fromMonth", fromMonth);
		map.put("to", flightsearchparam.getArr());
		map.put("ojFrom", "");
		map.put("trip", "retour");
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
			String[] toDate=arg0.getRetDate().split("-");
			String toMonth=toDate[0]+"-"+toDate[1];
			String toDay=toDate[2];
			NameValuePair[] pairs = new NameValuePair[]{
				     new NameValuePair("adults", "1"),
				     new NameValuePair("from", arg0.getDep()),
				     new NameValuePair("to", arg0.getArr()),
				     new NameValuePair("fromMonth", fromMonth),
				     new NameValuePair("fromDay", fromDay),
				     new NameValuePair("toMonth", toMonth),
				     new NameValuePair("toDay", toDay),
				     new NameValuePair("trip", "retour"),
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
	public ProcessResultInfo process(String s, FlightSearchParam flightsearchparam) {
		String html = s;
		ProcessResultInfo result = new ProcessResultInfo();
		try {
			String theToDeparture="",theToArrival="",theToCarrierCode="",theFromDeparture="",theFromArrival="",theFromCarrierCode="";
			//往
			String hc=StringUtils.substringBetween(html,"<div id=\"hc\">", "<div id=\"backfl\"");
			//返
			String tc=StringUtils.substringBetween(html,"<div id=\"tc\">", "<div class=\"clear\">");
			List<RoundTripFlightInfo> flightList = new ArrayList<RoundTripFlightInfo>();
			if(null!=tc){
				//基本信息
				String[] basicinfo=tc.substring(tc.lastIndexOf("</div>")+6, tc.length()).split(">");
				for(int i=0 ;i<basicinfo.length;i++){
					if(basicinfo[i].contains("theToDeparture")){
						theToDeparture=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
					if(basicinfo[i].contains("theToArrival")){
						theToArrival=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
					if(basicinfo[i].contains("theToCarrierCode")){
						theToCarrierCode=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
					if(basicinfo[i].contains("theFromDeparture")){
						theFromDeparture=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
					if(basicinfo[i].contains("theFromArrival")){
						theFromArrival=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
					if(basicinfo[i].contains("theFromCarrierCode")){
						theFromCarrierCode=StringUtils.substringBetween(basicinfo[i],"value=\"", "\"");
					}
				}

				
				String[] toflight=hc.split("<div class=\"flightday\">");
				for(int i=0 ;i<toflight.length;i++){
					if(toflight[i].contains("checked")){
						String flightItemStr=StringUtils.substringBetween(toflight[i],"<ul><li class=\"", "</ul>");
						if(null!=flightItemStr){
							//获取当天的航班信息列表
							String []flightItems=flightItemStr.split("<li class=\"");
							for(int j=0;j<flightItems.length;j++){
								RoundTripFlightInfo baseFlight = new RoundTripFlightInfo();
								List<FlightSegement> segs = new ArrayList<FlightSegement>();
								FlightDetail flightDetail = new FlightDetail();
								
								String flightdate=StringUtils.substringBetween(flightItems[j],"name=\"flightdate\" value=\"","\"");
								String flightdeptime=StringUtils.substringBetween(flightItems[j],"name=\"flightdeptime\" value=\"","\"");
								String flightarrtime=StringUtils.substringBetween(flightItems[j],"name=\"flightarrtime\" value=\"","\"");
								String number=StringUtils.substringBetween(flightItems[j],"<label for=\""+flightdate+"|","\"");
								String price=StringUtils.substringBetween(flightItems[j],"<strong>","</strong>");
								
								List<String> flightNoList = new ArrayList<String>();
								flightNoList.add(theToCarrierCode+number);
								
								FlightSegement seg = new FlightSegement();
								seg.setFlightno(theToCarrierCode+number);
								seg.setDepDate(flightdate);
								seg.setDepairport(flightsearchparam.getDep());
								seg.setArrairport(flightsearchparam.getArr());
								seg.setDeptime(flightdeptime);
								seg.setArrtime(flightarrtime);
								seg.setCompany(theToCarrierCode);
								segs.add(seg);
								
								flightDetail.setMonetaryunit("EUR");
								flightDetail.setDepcity(flightsearchparam.getDep());
								flightDetail.setArrcity(flightsearchparam.getArr());
								flightDetail.setWrapperid(flightsearchparam.getWrapperid());
								flightDetail.setDepdate(Date.valueOf(flightsearchparam.getDepDate()));
								flightDetail.setFlightno(flightNoList);
								if(null!=tc){
									String[] backflight=tc.split("<div class=\"flightday\">");
									for(int k=0 ;k<backflight.length;k++){
										if(backflight[k].contains("checked")){
											String retflightItemStr=StringUtils.substringBetween(backflight[k],"<ul><li class=\"", "</ul>");
											if(null!=retflightItemStr){
												List<FlightSegement> retsegs = new ArrayList<FlightSegement>();
												//获取当天的航班信息列表
												String []retflightItems=retflightItemStr.split("<li class=\"");
												for(int m=0;m<retflightItems.length;m++){
													String retflightdate=StringUtils.substringBetween(retflightItems[m],"name=\"flightdate\" value=\"","\"");
													String retflightdeptime=StringUtils.substringBetween(retflightItems[m],"name=\"flightdeptime\" value=\"","\"");
													String retflightarrtime=StringUtils.substringBetween(retflightItems[m],"name=\"flightarrtime\" value=\"","\"");
													String retnumber=StringUtils.substringBetween(retflightItems[m],"<label for=\""+retflightdate+"|","\"");
													String retprice=StringUtils.substringBetween(retflightItems[m],"<strong>","</strong>");
													
													List<String> retflightNoList = new ArrayList<String>();
													retflightNoList.add(theFromCarrierCode+retnumber);
													
													FlightSegement retseg = new FlightSegement();
													retseg.setFlightno(theFromCarrierCode+retnumber);
													retseg.setDepDate(retflightdate);
													retseg.setDepairport(theFromDeparture);
													retseg.setArrairport(theFromArrival);
													retseg.setDeptime(retflightdeptime);
													retseg.setArrtime(retflightarrtime);
													retseg.setCompany(theFromCarrierCode);
													retsegs.add(retseg);
													
													baseFlight.setInfo(segs);
													flightDetail.setPrice(Math.round(Double.parseDouble(price)+Double.parseDouble(retprice)+10));
													flightDetail.setTax(0);
													baseFlight.setDetail(flightDetail);
													baseFlight.setOutboundPrice(Math.round(Double.parseDouble(price.substring(1))));
													baseFlight.setRetinfo(retsegs);
													baseFlight.setRetdepdate(Date.valueOf(flightsearchparam.getRetDate()));
													baseFlight.setRetflightno(retflightNoList);
												    baseFlight.setReturnedPrice(Math.round(Double.parseDouble(retprice)));
												    flightList.add(baseFlight);
												}
											}
											break;
										}
									}
								}
							}
						}
						break;
					}
				}
			}
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		} catch(Exception e){
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("VCE");
		searchParam.setArr("EIN");
		searchParam.setDepDate("2014-06-19");
		searchParam.setRetDate("2014-06-21");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjsairhv001");
		searchParam.setToken("");
		String html = new  Wrapper_gjsairhv001().getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = new  Wrapper_gjsairhv001().process(html,searchParam);
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
