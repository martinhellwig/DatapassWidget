package de.schooltec.datapass.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Martin on 30.07.2015.
 */
public class HTTPFunctions {

    private ServerConnection serverConnection;
    private final static String URL = "http://www.datapass.de/";

    private String actualAmount;
    private String maxAmount;
    private float alreadyUsed;
    private String lastUpdate;

    private void createServerConnection() {
        serverConnection = new ServerConnection();
    }

    public boolean getPageAndParse() {
        try {
            createServerConnection();
            String htmlContent = serverConnection.getStringFromUrl(URL);

            //first get the two values of traffic amount
            Pattern pattern = Pattern.compile("(\\d{1,4},{0,1}\\d{0,3}.(GB|MB|KB))");
            Matcher matcher = pattern.matcher(htmlContent);

            int i = 0;
            while (matcher.find()) {
                if(i == 0) actualAmount = matcher.group(1);
                if(i == 1) maxAmount = matcher.group(1);
                i++;
            }

            float numberActual = Float.parseFloat(actualAmount.substring(0, actualAmount.length()-3).replace(",", "."));
            float numberMax = Float.parseFloat(maxAmount.substring(0, maxAmount.length()-3).replace(",", "."));

            if(actualAmount.contains("KB")) {
                alreadyUsed = 0f;
            }
            else {
                if (actualAmount.contains("GB")) {
                    if (maxAmount.contains("GB")) {
                        alreadyUsed = numberActual / numberMax;
                    } else { //We assume the amount is in MB (but this can't be possible, because then the maxAMount would be less than the actualAmount)
                        alreadyUsed = numberActual * 1024f / numberMax;
                    }
                } else { //in this case the actualAmount is in MB
                    if (maxAmount.contains("GB")) {
                        alreadyUsed = numberActual / (numberMax * 1024f);
                    } else { //We assume the amount is in MB
                        alreadyUsed = numberActual / numberMax;
                    }
                }
            }

            //Now get the date of last update
            pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}.{4}\\d{2}:\\d{2})");
            matcher = pattern.matcher(htmlContent);
            String time ="";
            while (matcher.find()) {
                time = matcher.group(1);
            }
            time = time.replace(" um ", ",");
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy,HH:mm");
            Date updateDate = format.parse(time);
            SimpleDateFormat outFormatDay = new SimpleDateFormat("dd.MM");
            SimpleDateFormat outFormatHour = new SimpleDateFormat("HH:mm");
            lastUpdate = outFormatDay.format(updateDate) + ", " + outFormatHour.format(updateDate);

            return true;
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getActualAmount() {
        return this.actualAmount;
    }

    public String getMaxAmount() {
        return this.maxAmount;
    }

    public float getAlreadyUsed() {
        return this.alreadyUsed;
    }

    public String getLastUpdate() {
        return this.lastUpdate;
    }
}
