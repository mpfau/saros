package saros.negotiation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import saros.editor.IEditorManager;
import saros.exceptions.LocalCancellationException;
import saros.exceptions.OperationCanceledException;
import saros.exceptions.SarosCancellationException;
import saros.filesystem.IFile;
import saros.filesystem.IProject;
import saros.filesystem.IResource;
import saros.filesystem.IWorkspace;
import saros.filesystem.checksum.IChecksumCache;
import saros.monitoring.IProgressMonitor;
import saros.negotiation.NegotiationTools.CancelOption;
import saros.net.IReceiver;
import saros.net.ITransmitter;
import saros.net.xmpp.JID;
import saros.net.xmpp.filetransfer.XMPPFileTransfer;
import saros.net.xmpp.filetransfer.XMPPFileTransferManager;
import saros.session.ISarosSession;
import saros.session.ISarosSessionManager;
import saros.session.User;
import saros.synchronize.StartHandle;

/**
 * Implementation of {@link AbstractOutgoingProjectNegotiation} utilizing a transferred zip archive
 * to exchange differences in the project files.
 */
public class ArchiveOutgoingProjectNegotiation extends AbstractOutgoingProjectNegotiation {

  private static final Logger log = Logger.getLogger(ArchiveOutgoingProjectNegotiation.class);
  private File zipArchive = null;

  public ArchiveOutgoingProjectNegotiation( //
      final JID peer, //
      final ProjectSharingData projects, //
      final ISarosSessionManager sessionManager, //
      final ISarosSession session, //
      final IEditorManager editorManager, //
      final IWorkspace workspace, //
      final IChecksumCache checksumCache, //
      final XMPPFileTransferManager fileTransferManager, //
      final ITransmitter transmitter, //
      final IReceiver receiver, //
      final AdditionalProjectDataFactory additionalProjectDataFactory //
      ) {
    super(
        peer,
        projects,
        sessionManager,
        session,
        editorManager,
        workspace,
        checksumCache,
        fileTransferManager,
        transmitter,
        receiver,
        additionalProjectDataFactory);
  }

  @Override
  protected void setup(IProgressMonitor monitor) throws IOException {
    // NOP
  }

  @Override
  protected void prepareTransfer(IProgressMonitor monitor, List<FileList> fileLists)
      throws IOException, SarosCancellationException {

    List<StartHandle> stoppedUsers = null;
    try {
      stoppedUsers = stopUsers(monitor);
      monitor.subTask("");

      sendAndAwaitActivityQueueingActivation(monitor);
      monitor.subTask("");

      User user = session.getUser(getPeer());

      if (user == null) throw new LocalCancellationException(null, CancelOption.DO_NOT_NOTIFY_PEER);

      /*
       * inform all listeners that the peer has started queuing and can
       * therefore process IResourceActivities now
       *
       * TODO this needs a review as this is called inside the "blocked"
       * section and so it is not allowed to send resource activities at
       * this time. Maybe change the description of the listener interface
       * ?
       */
      session.userStartedQueuing(user);

      zipArchive = createProjectArchive(fileLists, monitor);
      monitor.subTask("");
    } finally {
      if (stoppedUsers != null) startUsers(stoppedUsers);
    }
  }

  @Override
  protected void transfer(IProgressMonitor monitor, List<FileList> fileLists)
      throws SarosCancellationException, IOException {
    if (zipArchive != null)
      sendArchive(zipArchive, getPeer(), TRANSFER_ID_PREFIX + getID(), monitor);
  }

  @Override
  protected void cleanup(IProgressMonitor monitor) {
    if (zipArchive != null && !zipArchive.delete())
      log.warn("could not delete archive file: " + zipArchive.getAbsolutePath());
    super.cleanup(monitor);
  }

  /**
   * @param fileLists a list of file lists containing the files to archive
   * @return zip file containing all files denoted by the file lists or <code>null</code> if the
   *     file lists do not contain any files
   */
  private File createProjectArchive(final List<FileList> fileLists, final IProgressMonitor monitor)
      throws IOException, SarosCancellationException {

    boolean skip = true;

    int fileCount = 0;

    for (final FileList list : fileLists) {
      skip &= list.getPaths().isEmpty();
      fileCount += list.getPaths().size();
    }

    if (skip) return null;

    checkCancellation(CancelOption.NOTIFY_PEER);

    final List<Pair<IFile, String>> filesToCompress = new ArrayList<>(fileCount);

    final List<IResource> projectsToLock = new ArrayList<IResource>();

    for (final FileList list : fileLists) {
      final String projectID = list.getProjectID();

      final IProject project = projects.getProject(projectID);

      if (project == null)
        throw new LocalCancellationException(
            "project with id " + projectID + " was unshared during synchronization",
            CancelOption.NOTIFY_PEER);

      projectsToLock.add(project);

      /*
       * force editor buffer flush because we read the files from the
       * underlying storage
       */
      if (editorManager != null) editorManager.saveEditors(project);

      final StringBuilder aliasBuilder = new StringBuilder();

      aliasBuilder.append(projectID).append(PATH_DELIMITER);

      final int prefixLength = aliasBuilder.length();

      for (final String path : list.getPaths()) {
        // assert path is relative !
        aliasBuilder.append(path);

        IFile file = project.getFile(path);
        String qualifiedPath = aliasBuilder.toString();

        filesToCompress.add(new ImmutablePair<>(file, qualifiedPath));

        aliasBuilder.setLength(prefixLength);
      }
    }

    log.debug(this + " : creating archive");

    File tempArchive = null;

    try {
      tempArchive = File.createTempFile("saros_" + getID(), ".zip");
      workspace.run(
          new CreateArchiveTask(tempArchive, filesToCompress, monitor),
          projectsToLock.toArray(new IResource[0]));
    } catch (OperationCanceledException e) {
      LocalCancellationException canceled = new LocalCancellationException();
      canceled.initCause(e);
      throw canceled;
    }

    monitor.done();

    return tempArchive;
  }

  private void sendArchive(
      File archive, JID remoteContact, String transferID, IProgressMonitor monitor)
      throws SarosCancellationException, IOException {

    log.debug(this + " : sending archive");
    monitor.beginTask("Sending archive file...", 100);

    XMPPFileTransfer transfer =
        fileTransferManager.fileSendStart(remoteContact, archive, transferID);
    monitorFileTransfer(transfer, monitor);

    monitor.done();

    log.debug(this + " : archive send");
  }
}
