package com.zimbra.cs.index;

import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.Zimbra;

public class LocalIndexingQueueAdapterTest extends AbstractIndexingQueueAdapterTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("");
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        adapter = new LocalIndexingQueueAdapter();
        adapter.drain();
        adapter.clearAllTaskCounts();
    }

    @Override
    protected boolean isQueueSourceAvailable() throws Exception {
        return true;
    }
}
