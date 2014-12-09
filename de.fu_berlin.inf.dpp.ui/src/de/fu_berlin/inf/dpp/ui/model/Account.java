/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *  (c) NFQ (www.nfq.com) - 2014
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.ui.model;

import de.fu_berlin.inf.dpp.net.xmpp.JID;

/**
 * Represents a Saros account of the user.
 * In contrast {@link de.fu_berlin.inf.dpp.ui.model.Contact}
 * represents user in the contact list of an account.
 *
 * Maybe in the future both classes will be merged.
 */
public class Account {

    private String username;
    private String domain;

    private JID jid;

    /**
     * @param username the username of the XMPP account
     * @param domain the domain part of the XMPP account
     */
    public Account(String username, String domain) {
        this.username = username;
        this.domain = domain;
        jid = new JID(username, domain);
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getBareJid() { return jid.getRAW(); }
}
