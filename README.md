<!---
Copyright 2018 Max Galloway
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--->
Daily Tweets Poster
==================

## Background
[If This Then That (IFTTT)](https://ifttt.com/) a platform for scripting interactions between web services,
called "recipes." For example, it provides by default a recipe where if a specific twitter user sends a 
tweet, then that tweet will be posted to reddit. However, if the user is even moderately active, this 
can be overwhelming. Fortunately, the IFTTT platform allows for the triggers to be handles by customs actions.
This application's purpose is to combine all the tweets into a single reddit post per day, by submitting  the 
first tweet for the day and updating it as new ones come in.

## Building and Running
This application is designed to run on google appengine. You will need to create a project in the google 
cloud dashboard, and you will need the Java 8 SDK and Maven installed locally. Also download and install 
the google cloud SDK https://cloud.google.com/appengine/docs/standard/java/download

Create an appengine project on Google's console, which as of this writing 
is located at https://console.cloud.google.com/
In pom.xml replace TODO_YOUR_APP_ID with your appengine project ID.

run `gcloud init`

To verify the environment is set up correct, run the application locally.

```
mvn clean package
mvn appengine:run
```

navigate to
https://apis-explorer.appspot.com/apis-explorer/?base=http%3A%2F%2Flocalhost%3A8080%2F_ah%2Fapi#p/twitterPoster/v1/twitterPoster.tweet.create

You'll need to temporarily disable your browser's protection against mixed http/https content. Click "Execute without OAuth." 
The server should return a 200 OK and a skeleton response.

## Deploying
All that's left is to replace various placeholders with your data, and send it to appengine.

1. On reddit, register for an api key at https://www.reddit.com/prefs/apps/ Create an application of type "script"
and for a uri put https://TODO_YOUR_APP_ID.appspot.com (with your appengine project ID)
2. in src/main/webapp/WEB-INF/appengine-web.xml replace TODO_YOUR_REDIT_USER_NAME and TODO_YOUR_REDDIT_APP_CLIENT_ID.
In src/main/webapp/WEB-INF/credentials.properties replace TODO_YOUR_REDDIT_USER_PASSWORD and TODO_YOUR_REDDIT_API_SECRET_KEY
3. back in src/main/webapp/WEB-INF/appengine-web.xml replace TODO_SUBREDDIT_TO_POST_IN. You may replace 
TODO_TWITTER_USERNAME, or you can replace the whole value field in this line. This value will be used as title 
of the daily reddit post, and will have the date appended to the end of the title.
4. open a password or random string generator and obtain two of them. Use of them for TODO_RANDOM_STRING  
in src/main/webapp/WEB-INF/appengine-web.xml and the other for TODO_YOUR_SECRET_TOKEN in 
com/maxgalloway/twitterPoster/Endpoint/TweetEndpoint.java
5. publish to appengine with the following commands: 
    ```
    mvn clean package
    mvn endpoints-framework:openApiDocs
    gcloud endpoints services deploy target/openapi-docs/openapi.json
    mvn appengine:deploy
    ```
6. Create an IFTTT account and create a new applet. 
    * For the IF select "New tweet by a specific user"
    * For the THAT search for "webhooks" and "Make a web request"
        * URL: https://TODO_YOUR_APP_ID.appspot.com/_ah/api/twitterPoster/v1/tweet (replacing with your project id)
        * Method: POST
        * content type: application/json
        * body: {"text":"{{Text}}", "link":"{{LinkToTweet}}", "extra":"TODO_YOUR_SECRET_TOKEN"}
            * secret token is from (4) above - everything else is verbatim

## Wrap up
And that's it! Hopefully you find this useful. I want to have a quick word about security before I let you go.
You'll notice that the authentication scheme used here (sending a password in a POST body over HTTPS) is extremely 
minimal. I chose this scheme due to limitations from both IFTTT and appengine, and also because there is 
little damage that someone could do if that credential is compromised. All they could do is submit text 
posts to a subreddit as you. That's a risk I'm willing to accept, especially since I created another reddit account 
just for this application. However, if you fork this repo and build additional functionality, 
I would recommend looking into more robust authentication options suppored on appengine 
https://cloud.google.com/appengine/docs/standard/java/oauth/ (however these will not work with IFTTT).