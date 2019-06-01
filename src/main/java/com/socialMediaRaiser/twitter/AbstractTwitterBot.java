package com.socialMediaRaiser.twitter;

import com.socialMediaRaiser.AbstractBot;
import com.socialMediaRaiser.RelationType;
import com.socialMediaRaiser.twitter.helpers.*;
import lombok.Data;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.*;

@Data
public abstract class AbstractTwitterBot extends AbstractBot implements ITwitterBot{

    private HttpClient client = HttpClient.newHttpClient();
    private URLHelper urlHelper = new URLHelper();
    private RequestHelper requestHelper = new RequestHelper();
    private JsonHelper jsonHelper = new JsonHelper();
    private final String IDS = "ids";
    private final String USERS = "users";
    private final String CURSOR = "cursor";
    private final String RETWEET_COUNT = "retweet_count";
    private final String RELATIONSHIP = "relationship";
    private final String FOLLOWING = "following";
    private final String FOLLOWED_BY = "followed_by";
    private final String SOURCE = "source";
    private final int MAX_GET_F_CALLS = 30;

    protected AbstractTwitterBot(){
        super(new GoogleSheetHelper());
    }

    protected abstract List<Long> getFollowedRecently();

    // can manage up to 5000 results / call . Max 15 calls / 15min ==> 75.000 results max. / 15min
    private List<Long> getUserIdsByRelation(String url){
        Long cursor = -1L;
        List<Long> result = new ArrayList<>();
        int nbCalls = 1;
        do {
            String url_with_cursor = url + "&"+CURSOR+"=" + cursor;
            JSONObject response = this.getRequestHelper().executeRequest(url_with_cursor, RequestMethod.GET);
            if(response!=null && response.has(IDS)){
                List<Long> ids = this.getJsonHelper().jsonLongArrayToList(response.get(IDS));
                if(ids!=null){
                    result.addAll(ids);
                }
            } else{
                System.err.println("response null or ids not found !");
            }

            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
        }
        while (cursor != 0 && cursor != null && nbCalls < MAX_GET_F_CALLS);
        return result;
    }

    // can manage up to 200 results/call . Max 15 calls/15min ==> 3.000 results max./15min
    private List<User> getUsersInfoByRelation(String url) {
        Long cursor = -1L;
        List<User> result = new ArrayList<>();
        int nbCalls = 1;
        System.out.print("users : ");
        do {
            String url_with_cursor = url + "&"+CURSOR+"=" + cursor;
            JSONObject response = this.getRequestHelper().executeRequest(url_with_cursor, RequestMethod.GET);
            if(response==null){
                break;
            }
            List<User> users = this.getJsonHelper().jsonUserArrayToList(response.get(USERS));
            result.addAll(users);
            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
            System.out.print(result.size() + " | ");
        } while (cursor != 0 && cursor!=null && nbCalls < MAX_GET_F_CALLS);
        System.out.print("\n");
        return result;
    }

    private List<Long> getUserIdsByRelation(Long userId, RelationType relationType){
        String url = null;
        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerIdsUrl(userId);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingIdsUrl(userId);
        }
        return this.getUserIdsByRelation(url);
    }

    // can manage up to 5000 results / call . Max 15 calls / 15min ==> 75.000 results max. / 15min
    private List<Long> getUserIdsByRelation(String userName, RelationType relationType){
        String url = null;

        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerIdsUrl(userName);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingIdsUrl(userName);
        }
        return this.getUserIdsByRelation(url);
    }

    // can manage up to 200 results/call . Max 15 calls/15min ==> 3.000 results max./15min
    private List<User> getUsersInfoByRelation(String userName, RelationType relationType) {
        String url = null;
        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerUsersUrl(userName);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingUsersUrl(userName);
        }

        return this.getUsersInfoByRelation(url);
    }

    private List<User> getUsersInfoByRelation(Long userId, RelationType relationType) {
        String url = null;
        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerUsersUrl(userId);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingUsersUrl(userId);
        }
        return this.getUsersInfoByRelation(url);
    }

    @Override
    public List<Long> getFollowingIds(String userName) {
        Long cursor = -1L;
        List<Long> result = new ArrayList<>();
        String url = this.urlHelper.getFollowingUsersUrl(userName);
        int nbCalls = 1;
        do {
            System.out.println("looking for following with cursor = " + cursor);
            String url_with_cursor = url + "&"+CURSOR+"=" + cursor;
            JSONObject response = this.getRequestHelper().executeRequest(url_with_cursor, RequestMethod.GET);
            result.addAll(this.getJsonHelper().jsonLongArrayToList(response.get("users")));
            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
        }
        while (cursor != 0 && cursor!=null && nbCalls < MAX_GET_F_CALLS);
        System.out.println(result.size() + " followings found for " + userName);
        return result;
    }

    @Override
    public List<Long> getFollowerIds(Long userId)  {
        return this.getUserIdsByRelation(userId, RelationType.FOLLOWER);
    }

    @Override
    public List<Long> getFollowerIds(String userName)  {
        return this.getUserIdsByRelation(userName, RelationType.FOLLOWER);
    }

    @Override
    public List<User> getFollowerUsers(Long userId) {
        return this.getUsersInfoByRelation(userId, RelationType.FOLLOWER);
    }

    @Override
    public List<User> getFollowerUsers(String userName) {
        if(this.urlHelper.canCallGetFollowers()){
            System.out.print("(1) ");
            return this.getUsersInfoByRelation(userName, RelationType.FOLLOWER);
        } else{
            System.out.print("(2) ");
            List<Long> followerIds = this.getUserIdsByRelation(userName, RelationType.FOLLOWER);
            List<User> result = new ArrayList<>();
            int i=0;
            while(this.urlHelper.canCallUserInfo() && i<followerIds.size()){
                Long followerId = followerIds.get(i);
                result.add(this.getUserFromUserId(followerId));
                i++;
            }
            if(!this.urlHelper.canCallUserInfo()){
                this.urlHelper.setFollowersCount(0);
            }
            return result;
        }
    }

    @Override
    public List<Long> getFollowingIds(Long userId) {
        return this.getUserIdsByRelation(userId, RelationType.FOLLOWING);
    }

    @Override
    public List<User> getFollowingsUsers(String userName) {
        return this.getUsersInfoByRelation(userName, RelationType.FOLLOWING);
    }

    @Override
    public List<User> getFollowingsUsers(Long userId) {
        return this.getUsersInfoByRelation(userId, RelationType.FOLLOWING);
    }

    @Override
    public RelationType getRelationType(Long userId1, Long userId2){
        String url = this.urlHelper.getFriendshipUrl(userId1, userId2);
        JSONObject response = this.getRequestHelper().executeRequest(url, RequestMethod.GET);
        if(response!=null && response.has(RELATIONSHIP)) {
            JSONObject relationship = (JSONObject) response.get(RELATIONSHIP);
            JSONObject sourceResult = (JSONObject) relationship.get(SOURCE);
            Boolean followedBy = (Boolean) sourceResult.get(FOLLOWED_BY);
            Boolean following = (Boolean) sourceResult.get(FOLLOWING);
            if(followedBy && !following){
                return RelationType.FOLLOWER;
            } else if (!followedBy && following){
                return RelationType.FOLLOWING;
            } else if (!followedBy && !following){
                return RelationType.NONE;
            } else if (followedBy && following){
                return RelationType.FRIENDS;
            }
        }
        System.err.print("areFriends was null for " + userId2 + "! -> false ");
        return null;
    }

    // API KO
    @Override
    public List<Long> getRetweetersId(Long tweetId) {
        String url = this.urlHelper.getRetweetersUrl(tweetId);
        return this.getUserIdsByRelation(url);
    }

    int getNbRT(Long tweetId) {
        String url = this.urlHelper.getTweetInfoUrl(tweetId);
        JSONObject response = this.getRequestHelper().executeRequest(url, RequestMethod.GET);
        if(response!=null){
            return (int)response.get(RETWEET_COUNT);
        } else{
            return -1;
        }
    }

    @Override
    public boolean follow(String userName){
        String url = this.urlHelper.getFollowUrl(userName);
        JSONObject jsonResponse = this.requestHelper.executeRequest(url, RequestMethod.POST);
        if(jsonResponse!=null) {
            if (jsonResponse.has(JsonHelper.FOLLOWING)) {
                System.out.println(userName + " followed ! ");
                return true;
            } else{
                System.err.println("following property not found :(  " + userName + " not followed !");
            }
        }
        return false;
    }

    @Override
    public boolean follow(Long userId) {
        String url = this.urlHelper.getFollowUrl(userId);
        JSONObject jsonResponse = this.requestHelper.executeRequest(url, RequestMethod.POST);
        if(jsonResponse!=null) {
            if (jsonResponse.has(JsonHelper.FOLLOWING)) {
                System.out.println(userId + " followed ! ");
                return true;
            } else{
                System.err.println("following property not found :(  " + userId + " not followed !");
            }
        }
        return false;
    }

    @Override
    public boolean unfollow(String userName){
        String url = this.urlHelper.getUnfollowUrl(userName);
        JSONObject jsonResponse = this.requestHelper.executeRequest(url, RequestMethod.POST);
        if(jsonResponse!=null){
            System.out.println(userName + " unfollowed");
            return true;
        }
        return false;
    }

    @Override
    public boolean unfollow(Long userId) {
        String url = this.urlHelper.getUnfollowUrl(userId);
        JSONObject jsonResponse = this.requestHelper.executeRequest(url, RequestMethod.POST);
        if(jsonResponse!=null){
            System.out.println(userId + " unfollowed");
            return true;
        }
        System.err.println(userId + " not unfollowed");
        return false;
    }

    public User getUserFromUserId(Long userId)  {
        String url = this.getUrlHelper().getUserUrl(userId);
        JSONObject response = this.getRequestHelper().executeRequest(url, RequestMethod.GET);
        if(response!=null){
            User user = this.getJsonHelper().jsonResponseToUser(response);
            user.addLanguageFromLastTweet(this.getUserLastTweets(user.getId(),2));
            return user;
        } else{
            System.err.println("user " + userId + " not found !!");
            return null;
        }
    }

    @Override
    public User getUserFromUserName(String userName)  {
        String url = this.getUrlHelper().getUserUrl(userName);
        JSONObject response = this.getRequestHelper().executeRequest(url, RequestMethod.GET);
        if(response!=null){
            User user = this.getJsonHelper().jsonResponseToUser(response);
            user.addLanguageFromLastTweet(this.getUserLastTweets(user.getId(),2));
            return user;
        } else{
            return null;
        }
    }

    public List<User> getUsersFromUserNames(List<String> userNames)  {
        String url = this.getUrlHelper().getUsersUrlbyNames(userNames);
        JSONArray response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null){
            List<User> users = this.getJsonHelper().jsonUserArrayToList(response);
            for(User user : users){
                user.addLanguageFromLastTweet(this.getUserLastTweets(user.getId(),2));
            }
            return users;
        } else{
            return null;
        }
    }

    List<User> getUsersFromUserIds(List<Long> userIds)  {
        String url = this.getUrlHelper().getUsersUrlbyIds(userIds);
        JSONArray response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null){
            List<User> users = this.getJsonHelper().jsonUserArrayToList(response);
            for(User user : users){
                user.addLanguageFromLastTweet(this.getUserLastTweets(user.getId(),2));
            }
            return users;
        } else{
            return null;
        }
    }

    JSONObject getRateLimitStatus(){
        String url = this.getUrlHelper().getRateLimitUrl();
        return this.getRequestHelper().executeRequest(url, RequestMethod.GET);
    }

    public void checkNotFollowBack(boolean unfollow, boolean writeInSheet, Date date) throws IOException {
        List<Long> followedPreviously = this.getIOHelper().getPreviouslyFollowedIds(true, true, date);
        User user = this.getUserFromUserName(FollowProperties.getStringProperty(FollowProperties.TWEET_NAME));
        this.areFriends(user.getId(), followedPreviously, unfollow, writeInSheet);
    }

    public List<Tweet> getUserLastTweets(Long userId, int count){
        String url = this.getUrlHelper().getUserTweetsUrl(userId, count);
        JSONArray response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null && response.length()>0){
            return this.getJsonHelper().jsonResponseToTweetList(response);
        }
        return null;
    }

    public List<Tweet> getUserLastTweets(String userName, int count){
        String url = this.getUrlHelper().getUserTweetsUrl(userName, count);
        JSONArray response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null && response.length()>0){
            return this.getJsonHelper().jsonResponseToTweetList(response);
        }
        return null;
    }

    @Override
    public void likeTweet(Long tweetId) {

    }

    @Override
    public void retweetTweet(Long tweetId) {

    }

    @Override
    public List<Tweet> searchForTweets(String query, int count, String fromDate, String toDate){ // @todo use date
        if(count<10){
            count = 10;
            System.err.println("count minimum = 10");
        }
        String url = this.getUrlHelper().getSearchTweetsUrl();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query",query);
        parameters.put("maxResults",String.valueOf(count));
        parameters.put("fromDate",fromDate);
        parameters.put("toDate",toDate);

        JSONObject response = this.getRequestHelper().executePostRequest(url,parameters);
        JSONArray responseArray = (JSONArray)response.get("results");
        if(response!=null && response.length()>0){
            return this.getJsonHelper().jsonResponseToTweetList(responseArray);
        }
        return null;
    }
}
