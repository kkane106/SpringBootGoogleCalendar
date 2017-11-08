package com.kriskane.app.configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;

@Configuration
public class GoogleConfiguration {
	
	@Value("${google.client.client-id}")
	private String clientId;
	@Value("${google.client.client-secret}")
	private String clientSecret;

	@Bean
	public JsonFactory getJsonFactory() {
		return JacksonFactory.getDefaultInstance();
	}
	
	@Bean
	public HttpTransport getGoogleHttpTransport() throws GeneralSecurityException, IOException {
		return GoogleNetHttpTransport.newTrustedTransport();
	}
	
	@Bean
	public GoogleClientSecrets getClientCredentials() {
		Details web = new Details();
		web.setClientId(this.clientId);
		web.setClientSecret(this.clientSecret);
		GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);
		return clientSecrets;
	}
	
	@Bean
	public GoogleAuthorizationCodeFlow getAuthCodeFlow(JsonFactory jsonFactory, HttpTransport httpTransport, GoogleClientSecrets clientSecrets) {
		return new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets,
				Collections.singleton(CalendarScopes.CALENDAR)).build();
	}
	

}


