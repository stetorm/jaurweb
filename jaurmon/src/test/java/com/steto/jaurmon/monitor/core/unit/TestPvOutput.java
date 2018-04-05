package com.steto.jaurmon.monitor.core.unit;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by sbrega on 09/02/2015.
 */
public class TestPvOutput {
    // TODO ALLINEARE
               /*

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private String pvOutDirPath;

    @Before
    public void before() throws IOException {
        pvOutDirPath = tempFolder.newFolder().getAbsolutePath();

    }

    @After
    public void after() throws IOException {
        tempFolder.delete();

    }

    @Test
    public void shouldUpdatePvOutputUsingBackupFiles() throws Exception {

        AuroraResponse goodAuroraResponse = mock(AuroraResponse.class);
        when(goodAuroraResponse.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);

        AuroraResponse badAuroraResponse = mock(AuroraResponse.class);
        when(badAuroraResponse.getErrorCode()).thenReturn(ResponseErrorEnum.TIMEOUT);

        AuroraDriver auroraDriver = mock(AuroraDriver.class);

        when(auroraDriver.acquireVersionId(anyInt())).thenReturn(badAuroraResponse);

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(auroraDriver, "resources/aurora.cfg", pvOutDirPath)
        {
            @Override
            public void batchPublish2PvOutput(String dataStorageFileName) throws IOException {
                lastPvOutputDataPublished = dataStorageFileName;
            }

        };

        PvOutputRecord pvOutputRecord1 = RandomObjectGenerator.getPvOutputRecord();
        PvOutputRecord pvOutputRecord2 = RandomObjectGenerator.getPvOutputRecord();

        auroraMonitor.savePvOutputRecord(pvOutputRecord1);
        String dataStorageFileName = auroraMonitor.savePvOutputRecord(pvOutputRecord2);

        // execution
        auroraMonitor.pvOutputjob();

        // verify
        assertTrue(auroraMonitor.lastPvOutputDataPublished.equals(dataStorageFileName));

    }

    @Test
    public void shouldNOTUpdatePvOutputUsingBackupFiles() throws Exception {

        AuroraResponse goodAuroraResponse = mock(AuroraResponse.class);
        when(goodAuroraResponse.getErrorCode()).thenReturn(ResponseErrorEnum.NONE);


        AuroraDriver auroraDriver = mock(AuroraDriver.class);

        when(auroraDriver.acquireVersionId(anyInt())).thenReturn(goodAuroraResponse);

        AuroraMonitorTestImpl auroraMonitor = new AuroraMonitorTestImpl(auroraDriver, "resources/aurora.cfg", pvOutDirPath)
        {
            @Override
            public void batchPublish2PvOutput(String dataStorageFileName) throws IOException {
                lastPvOutputDataPublished = dataStorageFileName;
            }

        };

        PvOutputRecord pvOutputRecord1 = RandomObjectGenerator.getPvOutputRecord();
        PvOutputRecord pvOutputRecord2 = RandomObjectGenerator.getPvOutputRecord();

        auroraMonitor.savePvOutputRecord(pvOutputRecord1);
        String dataStorageFileName = auroraMonitor.savePvOutputRecord(pvOutputRecord2);

        // execution
        auroraMonitor.pvOutputjob();

        // verify
        assertTrue(auroraMonitor.lastPvOutputDataPublished.isEmpty());

    }
    */
}
