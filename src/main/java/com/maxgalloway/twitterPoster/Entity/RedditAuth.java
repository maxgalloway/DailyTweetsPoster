package com.maxgalloway.twitterPoster.Entity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

/**
 *  after authenticating with the reddit api, the access token gets saved
 *  in the data-store via this object
 *  
 * Copyright 2018 Max Galloway
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Entity
@Cache
public class RedditAuth {
	
	@Id public String id = System.getProperty("reddit.auth.randomID");
	public String access_token;
	@Ignore public boolean dirty=false;
	
	public static RedditAuth setAuthorization() {
		
		try {
			URL url = new URL("https://www.reddit.com/api/v1/access_token");
		
		
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			
			Properties p = getCredentials();
			String urlParameters  = "grant_type=password&username="
					+System.getProperty("reddit.username")
					+"&password=" 
					+ p.getProperty("password");
			
			conn.setRequestProperty(
				"User-Agent", 
				"app-engine:com.appspot."
						+System.getProperty("appengine.instance")
						+":v1.0 (by /u/"
						+System.getProperty("reddit.username")+")"
			);
			
			String encoding = DatatypeConverter.printBase64Binary((System.getProperty("reddit.client_id") 
					+ ":" + p.getProperty("client_secret")).getBytes());
		    conn.setRequestProperty  ("Authorization", "Basic " + encoding);
			
//		    send the data
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(urlParameters);
			writer.close();
		
		
			int respCode = conn.getResponseCode();  // New items get NOT_FOUND on PUT
			
			if (respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_CREATED) {
		
		
			  StringBuffer response = new StringBuffer();
			  String line;
		
		
			  BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			  while ((line = reader.readLine()) != null) {
			    response.append(line);
			  }
			  reader.close();
			  
			  Gson g = new Gson();
			  RedditAuth ra = g.fromJson(response.toString(), RedditAuth.class);
			  ra.dirty = true;
			  
			  return ra;
			  
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.err.println(ex.getMessage());
		}

		return null;
	}
	
	private static Properties getCredentials() {
		Properties prop = new Properties();
		
		try {
			InputStream input = new FileInputStream("WEB-INF/credentials.properties");

			// load a properties file

			prop.load(input);
			
		} catch (IOException f) {
			System.err.println(f.getMessage());
		}
		
		return prop;
	}

}
