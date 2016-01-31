package com.steto.jaurmon.monitor.core.unit;

import com.steto.jaurmon.utils.FormatStringUtils;
import com.steto.jaurmon.utils.MyUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Created by sbrega on 04/02/2015.
 */
public class TestUtils {


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private String tempFolderPath;

    @Before
    public void before() throws IOException {
        tempFolder.create();

    }

    @After
    public void after() throws IOException {
        tempFolder.delete();

    }

    @Test
    public void shouldBeSameDay() {
        Date now = new Date();
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        Calendar calTomorrow = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        calTomorrow.add(Calendar.DATE, +1);
        calToday.setTime(now);

        assertFalse(MyUtils.sameDay(now, calYesterday.getTime()));
        assertFalse(MyUtils.sameDay(now, calTomorrow.getTime()));
        assertTrue(MyUtils.sameDay(now, calToday.getTime()));

    }

    @Test
    public void shouldConvertDate2String() throws ParseException {
        Date now = new Date();
        Calendar calToday = Calendar.getInstance();
        calToday.setTime(now);
        String timeString = FormatStringUtils.fromDate(now);

        int year = calToday.get(Calendar.YEAR);
        int month = calToday.get(Calendar.MONTH) + 1;
        int day = calToday.get(Calendar.DAY_OF_MONTH);
        int hour = calToday.get(Calendar.HOUR_OF_DAY);

        String expected = String.format("%04d", year) + "-" + String.format("%02d", month) + "-" + String.format("%02d", day) + "_" + String.format("%02d", hour);

        System.out.print(timeString);
        assertEquals(expected, timeString);

    }

    @Test
    public void shouldSortFiles() throws ParseException, IOException {

        File file0 = tempFolder.newFile("2000-02-08_09.txt");
        File file1 = tempFolder.newFile("2014-02-08_09.csv");
        File file2 = tempFolder.newFile("2014-02-08_10.csv");
        File file3 = tempFolder.newFile("2015-02-10_07.csv");
        tempFolder.newFolder("0");

        String selectedFile = MyUtils.selectFirstFile(file1.getParent(), ".csv");

        assertTrue(selectedFile.equals(file1.getAbsolutePath()));


    }

    @Test
    public void shouldSortEmptyFileList() throws ParseException, IOException {

        File file0 = tempFolder.newFile("2000-02-08_09.txt");
        File file1 = tempFolder.newFile("2014-02-08_09.csv");
        File file2 = tempFolder.newFile("2014-02-08_10.csv");
        File file3 = tempFolder.newFile("2015-02-10_07.csv");
        tempFolder.newFolder("0");

        String selectedFile = MyUtils.selectFirstFile(file1.getParent(), ".xxx");

        assertTrue(selectedFile.isEmpty());


    }

}
