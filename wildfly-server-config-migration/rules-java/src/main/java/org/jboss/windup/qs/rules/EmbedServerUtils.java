/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.windup.qs.rules;

import org.wildfly.core.embedded.EmbeddedServerFactory;
import org.wildfly.core.embedded.ServerStartException;
import org.wildfly.core.embedded.StandaloneServer;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.File;

/**
 *
 * @author emmartins
 */
class EmbedServerUtils {

    public static StandaloneServer startStandaloneServer() throws IllegalStateException, IllegalArgumentException, ServerStartException {
        return startStandaloneServer(null, true, false, false, null);
    }

    public static StandaloneServer startStandaloneServer(String xml, boolean adminOnlySetting, boolean startEmpty, boolean removeExisting, Long bootTimeout) throws IllegalStateException, IllegalArgumentException, ServerStartException {
        WildFlySecurityManager.setPropertyPrivileged("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        final StandaloneServer standaloneServer = EmbeddedServerFactory.create(getJBossHome().getAbsolutePath(), null, null);
        try {
            standaloneServer.start();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return standaloneServer;
    }

    private static File getJBossHome() throws IllegalArgumentException {
        String jbossHome = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
        if (jbossHome == null || jbossHome.length() == 0) {
            throw new IllegalArgumentException("Environment variable JBOSS_HOME is not set");
        }
        return validateJBossHome(jbossHome, "environment variable JBOSS_HOME");
    }

    private static File validateJBossHome(String jbossHome, String source) throws IllegalArgumentException {
        File f = new File(jbossHome);
        if (!f.exists()) {
            throw new IllegalArgumentException(String.format("File %s specified by %s does not exist", jbossHome, source));
        } else if (!f.isDirectory()) {
            throw new IllegalArgumentException(String.format("File %s specified by %s is not a directory", jbossHome, source));
        }
        return f;
    }

}
