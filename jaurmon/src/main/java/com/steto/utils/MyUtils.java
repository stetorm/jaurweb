package com.steto.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sbrega on 04/02/2015.
 */
public class MyUtils {

    static public boolean sameDay(Date dateA, Date dateB) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateA);
        int dayA = cal.get(Calendar.DAY_OF_YEAR);
        cal.setTime(dateB);
        int dayB = cal.get(Calendar.DAY_OF_YEAR);

        return dayA==dayB;
    }

    public static boolean epsEquals(double a, double b)
    {
        double eps = 0.0001;
        double normDiff = Math.abs(a-b) / (Math.abs(a+b));
        return  normDiff< eps;
    }

    public static String selectFirstFile(String filePath, final CharSequence extension) {

        String result = "";
        File folder = new File(filePath);
        File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File aFile) {
                return !aFile.isDirectory() && aFile.getName().contains(extension);
            }
        });
        if (files != null && files.length>0) {
            Arrays.sort(files);
            result = files[0].getAbsolutePath();
        }

        return result;
    }

}
