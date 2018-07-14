package com.maxgalloway.twitterPoster.Entity;

import com.google.gson.Gson;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.String;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * a whole day's reddit post gets stored in the data-store via this object
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
public class RedditPost {

	private static final String POST_FULLNAME_PREFIX = "t3_";
	private static final String ENCODING = "UTF-8";

	@Id public String date; // mm/dd/yyyy identifiers must be unique
	public String url;
	public String text;
	public String postId;
	@Ignore public boolean dirty=false;

	public RedditPost() {
	};

	public RedditPost(String url, String text) {
		this();
		this.url  = url;
		this.text = text;
		this.date = getToday();
	}

	public RedditPost(String text) {
		this();
		this.text = text;
		this.date = getToday();
	}

	public static String getToday() {
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		return sdf.format(d);
	}

	public void submit(RedditAuth ra) {
		
		int lastResult = 0;
		
		do {			
			lastResult = this.doSubmit(ra);
			
//			re-authorize and go again if 401
		} while (lastResult == HttpURLConnection.HTTP_UNAUTHORIZED && (ra = RedditAuth.setAuthorization()) != null);
	}

	private int doSubmit(RedditAuth ra) {
		try {
			URL url;
			String urlParameters;
			
			if (this.postId == null) { // create
				url = new URL("https://oauth.reddit.com/api/submit");

				urlParameters  = "title="
						+URLEncoder.encode(System.getProperty("reddit.base_post_title"), ENCODING)
						+" "
						+URLEncoder.encode(this.date, ENCODING)
						+ "&text="
						+URLEncoder.encode(this.text, ENCODING)
						+"&sr="
						+System.getProperty("reddit.subreddit")
						+"&kind=self&api_type=json&extension=json";
				
			} else { // update
				url = new URL("https://oauth.reddit.com/api/editusertext");
				urlParameters = "text="
						+URLEncoder.encode(this.text, ENCODING)
						+"&sr="
						+System.getProperty("reddit.subreddit")
						+"&kind=self&api_type=json&thing_id="
						+URLEncoder.encode(POST_FULLNAME_PREFIX + this.postId, ENCODING);
			}
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty(
				"User-Agent", 
				"app-engine:com."
						+System.getProperty("appengine.instance")
						+":v1.0 (by /u/"
						+System.getProperty("reddit.username")+")"
			);
			conn.setRequestProperty("Authorization", "bearer " + ra.access_token);

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
				
				RedditPostResponse rr = g.fromJson(response.toString(), RedditPostResponse.class);
				
//				return success if no errors, or generic error if errors
				if (rr.json.errors.length == 0) {
					
					if (this.postId == null && rr.json.data.id != null) {
						this.postId = rr.json.data.id;
						this.dirty = true;
					}
					
					return HttpURLConnection.HTTP_OK;
					
				} else {
					System.err.println(Arrays.deepToString(rr.json.errors));
					return HttpURLConnection.HTTP_BAD_REQUEST;
				}
				
			} else {
				return respCode;
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.err.println(ex.getMessage());
			return HttpURLConnection.HTTP_INTERNAL_ERROR;
		}
	}
}
