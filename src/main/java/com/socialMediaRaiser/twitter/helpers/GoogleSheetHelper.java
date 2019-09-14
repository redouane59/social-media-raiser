package com.socialMediaRaiser.twitter.helpers;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.socialMediaRaiser.twitter.FollowProperties;
import com.socialMediaRaiser.twitter.User;
import com.socialMediaRaiser.twitter.helpers.dto.getUser.AbstractUser;
import lombok.Data;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
public class GoogleSheetHelper extends AbstractIOHelper {
    private Sheets sheetsService;
    private String followedBackColumn;
    private String sheetId;
    private String tabName;
    private String resultColumn;
    private Map<String, Integer> userRows = new HashMap<>();

    public GoogleSheetHelper(String ownerName){
        if(ownerName!=null && ownerName.length()>0){
            FollowProperties.load(ownerName);
        }
        this.sheetId = FollowProperties.ioProperties.getId();
        this.tabName = FollowProperties.ioProperties.getTabName();
        this.resultColumn = FollowProperties.ioProperties.getResultColumn();
        this.followedBackColumn = this.tabName+"!"+FollowProperties.ioProperties.getResultColumn();
        try {
            this.sheetsService = SheetsServiceUtil.getSheetsService();
        } catch(Exception e){
            e.printStackTrace();
        }
        this.setAllUserRows();
    }

    public List<String> getPreviouslyFollowedIds(boolean showFalse, boolean showTrue, Date date) {
        String startLine = "A2";
        int followBackResultIndex = resultColumn.toLowerCase().toCharArray()[0] - 'a';
        List<String> ranges = Arrays.asList(this.tabName +"!"+startLine+":"+resultColumn);
        try{
            BatchGetValuesResponse readResult = sheetsService.spreadsheets().values()
                    .batchGet(this.sheetId)
                    .setRanges(ranges)
                    .execute();
            List<List<Object>> result = readResult.getValueRanges().get(0).getValues();
            List<String> ids = new ArrayList<>();
            for(List<Object> valueArray : result){
                if((showFalse && showTrue)
                        || valueArray.size()<=followBackResultIndex
                        || (valueArray.size()>followBackResultIndex && showFalse && String.valueOf(valueArray.get(followBackResultIndex)).toLowerCase().equals("false"))
                        || (valueArray.size()>followBackResultIndex && showTrue && String.valueOf(valueArray.get(followBackResultIndex)).toLowerCase().equals("true"))){

                    DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                    Date followDate = formatter.parse( String.valueOf(valueArray.get(FollowProperties.ioProperties.getFollowDateIndex())));

                    int diffInDays = -1;
                    if(date!=null && valueArray.size() > FollowProperties.ioProperties.getFollowDateIndex() && date.getDate() == followDate.getDate()){
                        diffInDays = (int) ((followDate.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
                    }

                    if (date==null || diffInDays==0) {
                        ids.add(String.valueOf(valueArray.get(0)));
                    }
                }
            }
            return ids;
        } catch (Exception e){
            e.printStackTrace();
        }

        return new ArrayList<String>();
    }

    public List<List<Object>> getRandomForestData(){
        List<String> ranges = Arrays.asList(FollowProperties.ioProperties.getRfaRange());
        try{
            BatchGetValuesResponse readResult = sheetsService.spreadsheets().values()
                    .batchGet(this.sheetId)
                    .setRanges(ranges)
                    .execute();
            return readResult.getValueRanges().get(0).getValues();
        } catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void addNewFollowerLine(AbstractUser user){
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Date followDate = user.getDateOfFollow();
        if(followDate==null){
            followDate = new Date();
        }
        String followBackDate = "";
        if(user.getDateOfFollowBack()!=null){
            followBackDate = dateFormat.format(followBackDate);
        }

        ValueRange body = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(
                        String.valueOf(user.getId()),
                        user.getUsername(),
                        user.getFollowersCount(),
                        user.getFollowingCount(),
                        user.getTweetCount(),
                        /*user.getFavouritesCount()*/ " ",
                        user.getDescription().
                                replace("\""," ")
                                .replace(";"," ")
                                .replace("\n"," "),
                        dateFormat.format(user.getLastUpdate()),
                        dateFormat.format(user.getDateOfCreation()),
                        user.getCommonFollowers(),
                        Optional.ofNullable(user.getLocation()).orElse(""),
                        dateFormat.format(followDate),
                        followBackDate,
                        user.getRandomForestPrediction()
                )));
        try{
            Sheets.Spreadsheets.Values.Append request =
                    sheetsService.spreadsheets().values().append(this.sheetId, this.tabName+"!A1", body);
            request.setValueInputOption("RAW");
            request.execute();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateFollowBackInformation(String userId, Boolean result) {
        String followedBack = String.valueOf(result).toUpperCase();
        System.out.print("updating " + userId + " -> " + followedBack + " ...");
        int row = this.userRows.get(userId);

        ValueRange requestBody = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(followedBack)));
        try {
            Sheets.Spreadsheets.Values.Update request = sheetsService.spreadsheets().values()
                    .update(this.sheetId, followedBackColumn+row, requestBody);
            request.setValueInputOption("USER_ENTERED");
            request.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setAllUserRows(){
        int startIndex = 2; // sheet starts at line 1 + header 1
        List<String> ids = this.getPreviouslyFollowedIds(true, true);
        for(int i=0; i<ids.size(); i++){
            this.userRows.put(ids.get(i), i+startIndex);
        }
    }
}
