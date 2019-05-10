package com.socialMediaRaiser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class AbstractBot implements InfoGetter, ActionPerformer  {

    public abstract List<? extends AbstractUser> getPotentialFollowers(Long ownerId, int count, boolean follow);

    public List<? extends AbstractUser> getPotentialFollowers(String userName, int count, boolean follow){
        AbstractUser user = this.getUserFromUserName(userName);
        return this.getPotentialFollowers(user.getId(), count, follow);
    }

    // @TODO same with String and ids
    public List<AbstractUser> follow(List<AbstractUser> users) {
        List<AbstractUser> followed = new ArrayList<>();
        for (AbstractUser user : users) {
            this.follow(user.getUserName());
            followed.add(user);
        }
        return followed;
    }

    public List<String> unfollow(List<String> userNames) {
        List<String> unfollowed = new ArrayList<>();
        for(String userName : userNames){
            boolean result = this.unfollow(userName);
            if(result){
                unfollowed.add(userName);
            }
        }
        return unfollowed;
    }

    public LinkedHashMap<String, Boolean> areFriends(String userName1, List<String> otherUsers){
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for(String otherUserName : otherUsers){
            Boolean areFriends = this.areFriends(userName1, otherUserName);
            if(areFriends==null){
                System.out.println("areFriends was null for " + otherUserName + "! -> false");
                areFriends = false;
            }
            result.put(otherUserName, areFriends);
        }
        return result;
    }

    public LinkedHashMap<Long, Boolean> areFriends(Long userId, List<Long> otherIds){
        LinkedHashMap<Long, Boolean> result = new LinkedHashMap<>();
        for(Long otherId : otherIds){
            Boolean areFriends = this.areFriends(userId, otherId);
            if(areFriends==null){
                System.out.println("areFriends was null for " + otherId + "! -> false");
                areFriends = false;
            }
            result.put(otherId, areFriends);

        }
        return result;
    }

    public LinkedHashMap<AbstractUser, Boolean> areFriends(AbstractUser user, List<AbstractUser> otherUsers){
        LinkedHashMap<AbstractUser, Boolean> result = new LinkedHashMap<>();
        for(AbstractUser otherUser : otherUsers){
            Boolean areFriends = this.areFriends(otherUser.getUserName(), user.getUserName());
            if(areFriends==null){
                areFriends = this.areFriends(otherUser.getId(), user.getId());
                if(areFriends==null) {
                    System.out.println("areFriends was null for " + otherUser.getUserName()
                            + " (" + otherUser.getId()+") ! -> false");
                    areFriends = false;
                }
            }
            result.put(otherUser, areFriends);
        }
        return result;
    }

    public List<String> getUsersNotFollowingBack(String userName, Boolean unfollow) {
        List<String> notFollowingsBackUsers = new ArrayList<>();
        AbstractUser user = this.getUserFromUserName(userName);
        List<Long> followingsId = this.getFollowingIds(user.getId());
        int i=0;

        while(i<followingsId.size()){
            Long followingId = followingsId.get(i);
            AbstractUser following = this.getUserFromUserId(followingId);
            Boolean areFriend = this.areFriends(userName, following.getUserName());
            if(areFriend!=null && !areFriend) {
                notFollowingsBackUsers.add(following.getUserName());
                if(unfollow){
                    this.unfollow(following.getUserName());
                }
            }
            i++;
        }

        return notFollowingsBackUsers;
    }
}