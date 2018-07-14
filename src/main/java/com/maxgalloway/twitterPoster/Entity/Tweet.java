package com.maxgalloway.twitterPoster.Entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

import java.lang.String;

/**
 *  Tweet object which will be saved to the data-store
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
public class Tweet {
	@Id public String link;
	public String text;
	@Ignore public String extra;

	public Tweet() {
		this.link = "";
		this.text = "";
	};

	public Tweet(String url, String text) {
		this();
		this.link  = url;
		this.text = text;
	}
	
	public Tweet(Tweet other) {
		this.link = other.link;
		this.text = other.text;
	}
}
