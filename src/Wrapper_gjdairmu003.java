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
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;

public class Wrapper_gjdairmu003 implements QunarCrawler {
	QFHttpClient httpClient = null;

	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "http://tw.ceair.com/muovc/front/reservation/flight-search!doFlightSearch.shtml";
		BookingResult bookingResult = new BookingResult();

		try {
			BookingInfo bookingInfo = new BookingInfo();
			bookingInfo.setAction(bookingUrlPre);
			bookingInfo.setMethod("get");
			Map<String, String> map = new LinkedHashMap<String, String>();
			map.put("cond.tripType", "OW");
			map.put("cond.depCode", arg0.getDep());
			map.put("cond.arrCode", arg0.getArr());
			map.put("cond.routeType", "3");
			map.put("cond.cabinRank", "ECONOMY");
			map.put("depDate", arg0.getDepDate());
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
			httpClient.getParams().setCookiePolicy(
					CookiePolicy.BROWSER_COMPATIBILITY);
			String getUrl = String
					.format("http://tw.ceair.com/muovc/front/reservation/flight-search!doFlightSearch.shtml?cond.tripType=OW&cond.depCode=%s&cond.arrCode=%s&cond.routeType=3&depDate=%s&depRtDate=&cond.cabinRank=ECONOMY",
							arg0.getDep(), arg0.getArr(), arg0.getDepDate());
			get = new QFGetMethod(getUrl);
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

	@Override
	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String html = StringUtils.substringBetween(arg0,
				"<div class=\"flight_table ow\">", "</div>");
		/*
		 * ProcessResultInfo中，
		 * ret为true时，status可以为：SUCCESS(抓取到机票价格)|NO_RESULT(无结果，没有可卖的机票)
		 * ret为false时
		 * ，status可以为:CONNECTION_FAIL|INVALID_DATE|INVALID_AIRLINE|PARSING_FAIL
		 * |PARAM_ERROR
		 */
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(html)) {
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;
		}
		// 需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (html.contains("Today Flight is full, select an other day or check later for any seat released. ")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;
		}
		try {
			String ht = StringUtils.substringBetween(html, "</thead>", "</table>").replace("\r\n", "").trim();
			Map cityMap=getCity();
			if (null != ht) {
				List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
				String tbody[] = ht.split("<tbody>");
				for (int i = 0; i < tbody.length; i++) {
					if (null != tbody[i] && !"".equals(tbody[i])) {
						OneWayFlightInfo baseFlight = new OneWayFlightInfo();
						FlightDetail flightDetail = new FlightDetail();
						List<FlightSegement> segs = new ArrayList<FlightSegement>();
						List<String> flightNoList = new ArrayList<String>();
						Double prices = 0d;
						Double taxs = 0d;
						String monetaryunit = "";
						String[] tr = tbody[i].substring(0,tbody[i].indexOf("<tr class=\"detail\">") - 19).trim().split("<tr class=\"booking\">");
						for (int j = 0; j < tr.length; j++) {
							if (null != tr[j] && !"".equals(tr[j])) {
								String[] td = tr[j].trim().split("<td");

								String[] depdatetime = null;
								String[] arrdatetime = null;
								String flightNo = "";
								FlightSegement seg = new FlightSegement();
								if (j == 1) {
									String depDateTimeStr = StringUtils.substringBetween(td[2], "/>","</td>").trim();
									depdatetime = depDateTimeStr.split(" ");
									String arrDateTimeStr = StringUtils.substringBetween(td[3], "/>","</td>").trim();
									arrdatetime = arrDateTimeStr.split(" ");
									//获取起飞城市
									String depairport=StringUtils.substringBetween(td[5],">","</td>").replace("\n", "").trim();
									seg.setDepairport(cityMap.get(depairport).toString());
									//获取到达城市
									String arrairport=StringUtils.substringBetween(td[6],">","</td>").replace("\n", "").trim();
									seg.setArrairport(cityMap.get(arrairport).toString());
									flightNo = StringUtils.substringBetween(td[4], "/>", "</td>").trim();
									String price = "0";
									String tax = "0";
									if (!td[7].contains("<span> - </span>")) {
										if ("".equals(monetaryunit)) {
											monetaryunit = StringUtils.substringBetween(td[7],"/>", "<span");
										}
										String[] span = td[7].split("<span");
										price = StringUtils.substringBetween(span[2], "\">", "</span>");
										if (null != price && !"".equals(price)) {
											prices += Double.parseDouble(price);
										}
										tax = StringUtils.substringBetween(span[3], "\">", "</span>");
										if (null != tax && !"".equals(tax)) {
											taxs += Double.parseDouble(tax);
										}

									} else if (!td[8]
											.contains("<span> - </span>")) {
										if ("".equals(monetaryunit)) {
											monetaryunit = StringUtils.substringBetween(td[8],"/>", "<span");
										}
										String[] span = td[8].split("<span");
										price = StringUtils.substringBetween(span[2], "\">", "</span>");
										tax = StringUtils.substringBetween(span[3], "\">", "</span>");
										if (null != price && !"".equals(price)) {
											prices += Double.parseDouble(price.replace(",", ""));
										}
										if (null != tax && !"".equals(tax)) {
											taxs += Double.parseDouble(tax.replace(",", ""));
										}
									} else {
										if (j == 0) {
											result.setRet(false);
											result.setStatus(Constants.PARSING_FAIL);
											return result;
										}

									}
								} else {
									String depDateTimeStr = StringUtils.substringBetween(td[1], "/>","</td>").trim();
									depdatetime = depDateTimeStr.split(" ");
									String arrDateTimeStr = StringUtils.substringBetween(td[2], "/>","</td>").trim();
									arrdatetime = arrDateTimeStr.split(" ");
									//获取起飞城市
									String depairport=StringUtils.substringBetween(td[4],">","</td>").replace("\n", "").trim();
									seg.setDepairport(cityMap.get(depairport).toString());
									//获取到达城市
									String arrairport=StringUtils.substringBetween(td[5],">","</td>").replace("\n", "").trim();
									seg.setArrairport(cityMap.get(arrairport).toString());
									flightNo = StringUtils.substringBetween(td[3], "/>", "</td>").trim();
								}
								
								flightNoList.add(flightNo);
								
								seg.setFlightno(flightNo);
								seg.setDepDate(arg1.getDepDate());
								seg.setDeptime(depdatetime[0].trim());
								seg.setArrtime(arrdatetime[0].trim());
								seg.setCompany(flightNo.substring(0, 2));
								// seg.setArrDate(arrdatetime[1].trim());
								segs.add(seg);
							}
						}
						flightDetail.setArrcity(arg1.getArr());
						flightDetail.setDepcity(arg1.getDep());
						flightDetail
								.setDepdate(Date.valueOf(arg1.getDepDate()));
						flightDetail.setPrice(prices);
						flightDetail.setTax(taxs);
						flightDetail.setMonetaryunit(monetaryunit);
						flightDetail.setFlightno(flightNoList);
						flightDetail.setWrapperid(arg1.getWrapperid());
						baseFlight.setDetail(flightDetail);
						baseFlight.setInfo(segs);
						flightList.add(baseFlight);
					}

				}
				result.setRet(true);
				result.setStatus(Constants.SUCCESS);
				result.setData(flightList);
				return result;
			} else {
				result.setRet(false);
				result.setStatus(Constants.CONNECTION_FAIL);
				return result;
			}
		} catch (Exception e) {
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

	public static void main(String[] args) {
		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("TPE");
		searchParam.setArr("JZH");
		searchParam.setDepDate("2014-07-18");
		searchParam.setTimeOut("60000");
		searchParam.setWrapperid("gjdairmu003");
		searchParam.setToken("");
		Wrapper_gjdairmu003 gjdairmu003 = new Wrapper_gjdairmu003();
		String html = gjdairmu003.getHtml(searchParam);
		System.out.println(html);
		ProcessResultInfo result = new ProcessResultInfo();
		result = gjdairmu003.process(html, searchParam);
		if (result.isRet() && result.getStatus().equals(Constants.SUCCESS)) {
			List<OneWayFlightInfo> flightList = (List<OneWayFlightInfo>) result
					.getData();
			for (OneWayFlightInfo in : flightList) {
				System.out.println("------------" + in.getDetail());
				System.out.println("************" + in.getInfo().toString());
			}
		} else {
			System.out.println(result.getStatus());
		}
	}
}
