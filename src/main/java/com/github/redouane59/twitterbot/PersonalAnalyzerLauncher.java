package com.github.redouane59.twitterbot;

import com.github.redouane59.twitter.TwitterClient;
import com.github.redouane59.twitter.dto.tweet.Tweet;
import com.github.redouane59.twitter.signature.TwitterCredentials;
import com.github.redouane59.twitterbot.impl.PersonalAnalyzerBot;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersonalAnalyzerLauncher {

  public static void main(String[] args) throws IOException {

    TwitterCredentials twitterCredentials = TwitterClient.OBJECT_MAPPER
        .readValue(new File("C:/Users/Perso/Documents/GitHub/twitter-credentials.json"), TwitterCredentials.class);

    if (args.length < 2) {
      LOGGER.error( "missing arguments");
    } else {
      String              userName         = args[0];
      boolean             unfollowMode     = Boolean.parseBoolean(args[1]);
      if (!unfollowMode) {
        if (args.length < 5) LOGGER.error( "missing arguments");
        boolean includeFollowers        = Boolean.parseBoolean(args[2]);
        boolean includeFollowings       = Boolean.parseBoolean(args[3]);
        boolean onlyFollowBackFollowers = Boolean.parseBoolean(args[4]);
        String tweetArchivePath         = args[5];
        boolean useGoogleSheets         = Boolean.parseBoolean(args[6]);
        //PersonalAnalyzerBot bot         = new PersonalAnalyzerBot(userName, tweetArchivePath, useGoogleSheets);
        PersonalAnalyzerBot bot         = new PersonalAnalyzerBot(userName, twitterCredentials);
        bot.launch(includeFollowers, includeFollowings, onlyFollowBackFollowers);
      } else {
        if (args.length < 3) LOGGER.error( "missing arguments");
        PersonalAnalyzerBot   bot       = new PersonalAnalyzerBot(userName, twitterCredentials);
        URL toUnfollowUrl = PersonalAnalyzerLauncher.class.getClassLoader().getResource(args[2]);
        URL whiteListUrl = PersonalAnalyzerLauncher.class.getClassLoader().getResource(args[3]);
        bot.unfollow(bot.getUsersFromJson(toUnfollowUrl), bot.getUsersFromJson(whiteListUrl));
      }
    }
  }


}