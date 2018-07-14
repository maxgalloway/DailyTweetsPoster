package com.maxgalloway.twitterPoster.Endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.maxgalloway.twitterPoster.Entity.RedditAuth;
import com.maxgalloway.twitterPoster.Entity.RedditPost;
import com.maxgalloway.twitterPoster.Entity.Tweet;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** 
 * endpoint for submitting new tweets to
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
 *
 * see:
 * http://localhost:8080/_ah/api/explorer
 */
@Api(
		name = "twitterPoster",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "twitterPoster.maxgalloway.com",
				ownerName = "twitterPoster.maxgalloway.com",
				packagePath=""
		)
)
public class TweetEndpoint {

	public static final String TOKEN = "TODO_YOUR_SECRET_TOKEN";
	
	@ApiMethod(name="tweet.create", httpMethod=HttpMethod.POST, path="tweet")
	public Tweet create(final Tweet twit) 
			throws NotFoundException, UnauthorizedException{
			
		if (twit.extra != null && twit.extra.equals(TOKEN) && twit.link != null && !twit.link.equalsIgnoreCase("")) {

//			1st: log the incoming tweet
			ofy().save().entity(twit);
			
			RedditPost rp = ofy().transact(new Work<RedditPost>() {
				public RedditPost run() {
//					2nd: check if post for today already exists
					RedditPost check = ofy()
							.load()
							.type(RedditPost.class)
							.id(RedditPost.getToday())
							.now();
					
					if (check == null) {
						check = new RedditPost("");						
					} else {
						check.text += System.lineSeparator();
					}
					
//					3rd: create or update post for today
					check.text += twit.text + System.lineSeparator() + twit.link + System.lineSeparator();					

					ofy().save().entity(check);
					
					return check;
				}
			});

//			use create or update rp on reddit
			this.submitPost(rp);
			
		}
		
		return twit;
	}

	/**
	 * posts (or updates post) on Reddit
	 *
	 * @param RedditPost rp
	 */
	private void submitPost(final RedditPost rp) {		
		ofy().transact(new VoidWork() {
			
			@Override
			public void vrun() {
				RedditAuth ra = ofy()
						.load()
						.type(RedditAuth.class)
						.id(System.getProperty("reddit.auth.randomID"))
						.now();
				
				if (ra == null) {
					ra = RedditAuth.setAuthorization();
					ofy().save().entity(ra).now();
				}
				
				rp.submit(ra);
				
//				if need to re-save auth, do so
				if (ra.dirty) {
					ofy().save().entity(ra);
				}
//				if need to update post, do so
				if (rp.dirty) {
					ofy().save().entity(rp);
				}				
			}
		});

	}
}
