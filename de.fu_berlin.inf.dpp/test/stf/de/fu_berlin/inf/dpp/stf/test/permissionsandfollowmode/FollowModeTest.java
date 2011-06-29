package de.fu_berlin.inf.dpp.stf.test.permissionsandfollowmode;

import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.ALICE;
import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.BOB;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.rmi.RemoteException;

import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.StfTestCase;
import de.fu_berlin.inf.dpp.stf.client.util.Util;
import de.fu_berlin.inf.dpp.stf.test.Constants;

public class FollowModeTest extends StfTestCase {

    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(ALICE, BOB);
        setUpWorkbench();
        setUpSaros();
        Util.setUpSessionWithAJavaProjectAndAClass(Constants.PROJECT1,
            Constants.PKG1, Constants.CLS1, ALICE, BOB);
    }

    @After
    public void runAfterEveryTest() throws RemoteException {
        Util.resetFollowModeSequentially(BOB, ALICE);
    }

    /**
     * 
     * @throws IOException
     * @throws CoreException
     */
    @Test
    public void testBobFollowAlice() throws IOException, CoreException {
        ALICE.superBot().views().packageExplorerView()
            .selectClass(Constants.PROJECT1, Constants.PKG1, Constants.CLS1)
            .open();

        ALICE.remoteBot().editor(Constants.CLS1_SUFFIX)
            .setTextFromFile(Constants.CP1);

        ALICE.remoteBot().editor(Constants.CLS1_SUFFIX).save();

        BOB.superBot().views().sarosView().selectParticipant(ALICE.getJID())
            .followParticipant();
        BOB.remoteBot().editor(Constants.CLS1_SUFFIX).waitUntilIsActive();
        assertTrue(BOB.superBot().views().sarosView()
            .selectParticipant(ALICE.getJID()).isFollowing());
        assertTrue(BOB.remoteBot().editor(Constants.CLS1_SUFFIX).isActive());

        String clsContentOfAlice = ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .getFileContent(
                Util.getClassPath(Constants.PROJECT1, Constants.PKG1,
                    Constants.CLS1));

        BOB.superBot()
            .views()
            .packageExplorerView()
            .waitUntilFileContentSame(
                clsContentOfAlice,
                Util.getClassPath(Constants.PROJECT1, Constants.PKG1,
                    Constants.CLS1));
        String clsContentOfBob = BOB
            .superBot()
            .views()
            .packageExplorerView()
            .getFileContent(
                Util.getClassPath(Constants.PROJECT1, Constants.PKG1,
                    Constants.CLS1));
        assertTrue(clsContentOfBob.equals(clsContentOfAlice));

        ALICE.superBot().views().packageExplorerView().tree().newC()
            .cls(Constants.PROJECT1, Constants.PKG1, Constants.CLS2);
        BOB.remoteBot().editor(Constants.CLS2_SUFFIX).waitUntilIsActive();
        assertTrue(BOB.remoteBot().editor(Constants.CLS2_SUFFIX).isActive());

        ALICE.superBot().views().sarosView().selectParticipant(BOB.getJID())
            .followParticipant();

        BOB.remoteBot().editor(Constants.CLS1_SUFFIX).show();

        ALICE.remoteBot().editor(Constants.CLS1_SUFFIX).waitUntilIsActive();

        assertTrue(ALICE.superBot().views().sarosView()
            .selectParticipant(BOB.getJID()).isFollowing());

        assertTrue(ALICE.remoteBot().editor(Constants.CLS1_SUFFIX).isActive());

    }
}
