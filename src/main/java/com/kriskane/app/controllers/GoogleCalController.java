package com.kriskane.app.controllers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Builder;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

@RestController
public class GoogleCalController {
	
	private static final Log LOGGER = LogFactory.getLog(GoogleCalController.class);
	private static final String APPLICATION_NAME = "Google Calendar";
	private JsonFactory jsonFactory;
	private HttpTransport httpTransport;
	private GoogleAuthorizationCodeFlow flow;
	
	public GoogleCalController(	JsonFactory jsonFactory, 
								HttpTransport httpTransport, 
								GoogleClientSecrets clientSecrets, 
								GoogleAuthorizationCodeFlow flow) {
		this.jsonFactory = jsonFactory;
		this.httpTransport = httpTransport;
		this.flow = flow;
	}

	@Value("${google.client.redirectUri}")
	private String redirectURI;
	
	@RequestMapping(value = "/google/calendar/events", method = RequestMethod.GET)
	public RedirectView googleConnectionStatus(
			RedirectAttributes redirectAttributes) throws Exception {
		return new RedirectView(authorize(null));
	}

	/**
	 * Example Request:
	 * http://localhost:8080/google/calendar/events?start=2017-11-01&end=2017-11-06&tmz=420
	 * NOTE : timezone 420 is the MST timezone. I am deriving this from JavaScript's `new Date().getTimezoneOffset();`
	 */
	@RequestMapping(value = "/google/calendar/events", method = RequestMethod.GET, params = {"start", "end", "tmz"})
	public RedirectView googleConnectionStatusWithParams(
			@RequestParam(value = "start") String startDate, 
			@RequestParam(value = "end") String endDate, 
			@RequestParam(value = "tmz") String tmz, 
			HttpServletRequest req, 
			RedirectAttributes redirectAttributes) throws Exception {
		// forward request parameters to authorize function so that they can be included as 'state' param with request		
		return new RedirectView(authorize("start=" + startDate + "&end=" + endDate + "&tmz=" + tmz));
	}
	
	/*
	 * DateTime format : "2017-05-05T16:30:00.000+05:30"
	 * Current DateTime : new DateTime(newDate());
	 * Convert String to DateTime : new DateTime("2017-05-05T16:30:00.000+05:30");
	 */
	@RequestMapping(value = "/google/calendar/events", method = RequestMethod.GET, params = "code")
	public Collection<Event> oauth2Callback(@RequestParam(value = "code") String code, @RequestParam(value = "state", required = false) String state, HttpServletRequest req, HttpServletResponse res) {
		Map<String, DateTime> dates = new HashMap<>();
		if (state == null) {
			long DAY_IN_MS = 1000 * 60 * 60 * 24;
			dates.put("start", new DateTime(new Date()));
			dates.put("end", new DateTime(new Date(System.currentTimeMillis() + (7 * DAY_IN_MS))));
		} else {
			dates = parseDateTimeFromParams(state);
		}
		Events eventList = null;
		try {
			TokenResponse response = this.flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
			Credential credential = this.flow.createAndStoreCredential(response, "userId"); // modify to store userId as string if so desired
			Calendar client = new Builder(httpTransport, jsonFactory, credential)
					.setApplicationName(APPLICATION_NAME).build();
			eventList = client.events()
						.list("primary") // google calendar id
						.setTimeMin(dates.get("start"))
						.setTimeMax(dates.get("end"))
						.execute();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")."
					+ " Redirecting to google connection status page.");
			res.setStatus(500);
			return null;
		}
		return eventList.getItems();
	}

	private String authorize(String state) throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(this.redirectURI);
		if (state != null) {
			authorizationUrl.setState(state);
		}
		return authorizationUrl.build();
	}
	
	/**
	 * Preserve this version with timezones on branch
	 */
	
	/** Used to parse DateTime from return state parameter, along with Timezone included with og request from JS
	 * NOTE : This is entirely optional, you could choose not to include these parameters at all and instead deal only
	 * with hardcoded DateTime objects. I have chosen to include a more robust example.	
	 * @param params
	 * @return
	 */
	private Map<String, DateTime> parseDateTimeFromParams(String params) {
		System.out.println(params);
		Map<String, DateTime> dates = new HashMap<>();
		List<String> strDates = Arrays.asList(params.split("&"));
		String formattedTimezone = "";
		String tmzMinutes = strDates.get(2).split("=")[1];
		float tmz = Float.parseFloat(tmzMinutes) / 60;
		// Determine sign		
		if ((int) tmz > 0) {
			formattedTimezone += "-";
		} else {
			formattedTimezone += "+";
		}
		// Determine if value requires preceding 0
		if (Math.abs((int) tmz) < 10) {
			formattedTimezone += "0";
		}
		// Determine if timezone requires :30 suffix
		if ((int) tmz == tmz) {
			formattedTimezone += ((int) tmz) + ":00";
		} else {
			formattedTimezone += ((int) tmz) + ":30";
		}
		
		dates.put("start", new DateTime(strDates.get(0).split("=")[1] + "T00:00:00.000" + formattedTimezone));
		dates.put("end", new DateTime(strDates.get(1).split("=")[1] + "T23:59:59.000" + formattedTimezone));
		return dates;
	}

}
