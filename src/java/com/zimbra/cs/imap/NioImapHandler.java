/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioOutputStream;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.stats.ZimbraPerf;

import java.io.IOException;
import java.net.Socket;

final class NioImapHandler extends ImapHandler implements NioHandler {
    private final NioConnection connection;
    private NioImapRequest request;

    NioImapHandler(NioImapServer server, NioConnection conn) {
        super(server);
        connection = conn;
        mOutputStream = conn.getOutputStream();
    }

    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return true;
        } else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }

        connection.startTls();
        sendOK(tag, "begin TLS negotiation now");
        mStartedTLS = true;
        return true;
    }

    @Override
    public void connectionOpened() throws IOException {
        sendGreeting();
    }

    @Override
    protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void messageReceived(Object msg) throws IOException {
        if (request == null) {
            request = new NioImapRequest(this);
        }
        if (request.parse(msg)) {
            // Request is complete
            setUpLogContext(connection.getRemoteAddress().toString());
            try {
                if (!processRequest(request)) {
                    dropConnection();
                }
            } catch (ImapParseException ipe) {
                handleParseException(ipe);
            } finally {
                ZimbraLog.clearContext();
                if (request != null) {
                    request.cleanup();
                    request = null;
                }
            }
            if (mConsecutiveBAD >= MAXIMUM_CONSECUTIVE_BAD) {
                dropConnection();
            }
        }
    }

    private boolean processRequest(NioImapRequest req) throws IOException, ImapParseException {
        if (req.isMaxRequestSizeExceeded())
            throw new ImapParseException(req.getTag(), "maximum request size exceeded");

        ImapSession i4selected = mSelectedFolder;
        if (i4selected != null)
            i4selected.updateAccessTime();

        long start = ZimbraPerf.STOPWATCH_IMAP.start();

        try {
            if (!checkAccountStatus())
                return STOP_PROCESSING;

            if (mAuthenticator != null && !mAuthenticator.isComplete())
                return continueAuthentication(req);

            try {
                boolean keepGoing = executeRequest(req);
                mConsecutiveBAD = 0;
                return keepGoing;
            } catch (ImapParseException ipe) {
                handleParseException(ipe);
                return CONTINUE_PROCESSING;
            }
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null)
                ZimbraPerf.IMAP_TRACKER.addStat(mLastCommand.toUpperCase(), start);
        }
    }

    /**
     * Called when connection is closed. No need to worry about concurrent execution since requests are processed in
     * sequence for any given connection.
     */
    @Override
    protected void dropConnection(boolean sendBanner) {
        try {
            unsetSelectedFolder(false);
        } catch (Exception e) {
        }

        if (mCredentials != null && !mGoodbyeSent) {
            ZimbraLog.imap.info("dropping connection for user " + mCredentials.getUsername() + " (server-initiated)");
        }

        if (!connection.isOpen()) {
            return; // No longer connected
        }
        ZimbraLog.imap.debug("dropConnection: sendBanner = %s\n", sendBanner);
        cleanup();

        if (sendBanner && !mGoodbyeSent) {
            sendBYE();
        }
        connection.close();
    }

    @Override
    public void dropConnection() {
        dropConnection(true);
    }

    @Override
    public void connectionClosed() {
        cleanup();
        connection.close();
    }

    private void cleanup() {
        if (request != null) {
            request.cleanup();
            request = null;
        }
        try {
            unsetSelectedFolder(false);
        } catch (Exception e) {}
    }

    @Override
    public void connectionIdle() {
        notifyIdleConnection();
    }

    @Override
    protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean authenticate() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void notifyIdleConnection() {
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override
    protected void enableInactivityTimer() {
        connection.setMaxIdleSeconds(mConfig.getAuthenticatedMaxIdleTime());
    }

    @Override
    protected void completeAuthentication() throws IOException {
        if (mAuthenticator.isEncryptionEnabled()) {
            connection.startSasl(mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }

    @Override
    protected void flushOutput() throws IOException {
        mOutputStream.flush();
    }

    @Override
    void sendLine(String line, boolean flush) throws IOException {
        NioOutputStream out = (NioOutputStream) mOutputStream;
        if (out != null) {
            out.write(line);
            out.write("\r\n");
            if (flush)
                out.flush();
        }
    }
}
