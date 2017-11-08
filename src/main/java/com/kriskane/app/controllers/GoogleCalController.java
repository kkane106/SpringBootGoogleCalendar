package com.kriskane.app.controllers;

import java.util.Collection;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

	public GoogleCalController(JsonFactory jsonFactory, HttpTransport httpTransport, GoogleClientSecrets clientSecrets,
			GoogleAuthorizationCodeFlow flow) {
		this.jsonFactory = jsonFactory;
		this.httpTransport = httpTransport;
		this.flow = flow;
	}

	@Value("${google.client.redirectUri}")
	private String redirectURI;

	@RequestMapping(value = "/google/calendar/events", method = RequestMethod.GET)
	public RedirectView googleConnectionStatusWithParams() throws Exception {
		return new RedirectView(authorize());
	}

	@RequestMapping(value = "/google/calendar/events", method = RequestMethod.GET, params = "code")
	public Collection<Event> oauth2Callback(@RequestParam(value = "code") String code, HttpServletResponse res) {
		// Set default dates for request
		long DAY_IN_MS = 1000 * 60 * 60 * 24;
		DateTime start = new DateTime(new Date());
		DateTime end = new DateTime(new Date(System.currentTimeMillis() + (7 * DAY_IN_MS)));
		Events eventList = null;
		try {
			TokenResponse response = this.flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
			Credential credential = this.flow.createAndStoreCredential(response, "userId"); 
			Calendar client = new Builder(httpTransport, jsonFactory, credential).setApplicationName(APPLICATION_NAME)
					.build();
			eventList = client.events().list("primary") // google calendar id
					.setTimeMin(start).setTimeMax(end).execute();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")."
					+ " Redirecting to google connection status page.");
			res.setStatus(500);
			return null;
		}
		return eventList.getItems();
	}

	private String authorize() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(this.redirectURI);
		return authorizationUrl.build();
	}

}
