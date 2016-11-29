/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.internal.messages;

import java.util.List;

/**
 * The {@link HeosPlayerGroup} is data structure to describe
 * Groups of Heos Players in the Heos ecosystem
 *
 * @author Jarle Player - Initial contribution
 */
public class HeosPlayerGroup implements Cloneable {

    private List<GroupMember> players;
    private transient GroupMember coordinator;
    private String gid;
    private String name;

    public static class GroupMember {

        private String name;
        private String pid;
        private String role;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

    }

    public List<GroupMember> getPlayers() {
        return players;
    }

    public void setPlayers(List<GroupMember> members) {
        this.players = members;

    }

    public GroupMember getCoordinator() {
        for (GroupMember member : players) {
            if (member.getRole().equals("leader")) {
                setCoordinator(member);
                break;
            }
        }
        return coordinator;
    }

    public void setCoordinator(GroupMember coordinator) {
        this.coordinator = coordinator;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
