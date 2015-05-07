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

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.PropertyConfigurator;
import org.wildfly.core.embedded.EmbeddedServerFactory;
import org.wildfly.core.embedded.ServerStartException;
import org.wildfly.core.embedded.StandaloneServer;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author emmartins
 */
class EmbedServerUtils {

    public static StandaloneServer startStandaloneServer() throws IllegalStateException, IllegalArgumentException, ServerStartException {
        return startStandaloneServer(null, true, false, false, null);

    }

    public static StandaloneServer startStandaloneServer(String xml, boolean adminOnlySetting, boolean startEmpty, boolean removeExisting, Long bootTimeout) throws IllegalStateException, IllegalArgumentException, ServerStartException {

        final File jbossHome = getJBossHome();
        boolean removeConfig = startEmpty && removeExisting;
        boolean ok = false;
        StandaloneServer server = null;
        try {


            // Create our own LogContext
            final LogContext embeddedLogContext = LogContext.create();
            // Set up logging from standalone/configuration/logging.properties
            configureLogContext(embeddedLogContext, jbossHome);


            List<String> cmdsList = new ArrayList<>();
            if (xml != null && xml.trim().length() > 0) {
                cmdsList.add("--server-config=" + xml.trim());
            }
            if (adminOnlySetting) {
                cmdsList.add("--admin-only");
            }
            if (startEmpty) {
                cmdsList.add("--internal-empty-config");
                if (removeConfig) {
                    cmdsList.add("--internal-remove-config");
                }
            }

            String[] cmds = cmdsList.toArray(new String[cmdsList.size()]);

            server = EmbeddedServerFactory.create(jbossHome.getAbsolutePath(), null, null, null, cmds);

            server.start();
            ModelControllerClient mcc = server.getModelControllerClient();
            if (bootTimeout == null || bootTimeout > 0) {
                // Poll for server state. Alternative would be to get ControlledProcessStateService
                // and do reflection stuff to read the state and register for change notifications
                long expired = bootTimeout == null ? Long.MAX_VALUE : System.nanoTime() + bootTimeout;
                String status = "starting";
                final ModelNode getStateOp = new ModelNode();
                getStateOp.get(ClientConstants.OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);
                getStateOp.get(ClientConstants.NAME).set("server-state");
                do {
                    try {
                        final ModelNode response = mcc.execute(getStateOp);
                        if (isSuccess(response)) {
                            status = response.get(ClientConstants.RESULT).asString();
                        }
                    } catch (Exception e) {
                        // ignore and try again
                    }

                    if ("starting".equals(status)) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while waiting for embedded server to start");
                        }
                    } else {
                        break;
                    }
                } while (System.nanoTime() < expired);

                if ("starting".equals(status)) {
                    assert bootTimeout != null; // we'll assume the loop didn't run for decades
                    server.stop();
                    throw new IllegalStateException("Embedded server did not exit 'starting' status within " +
                            TimeUnit.MILLISECONDS.toSeconds(bootTimeout) + " seconds");
                }

            }


            ok = true;
            return server;
        } finally {
            if (!ok && server != null) {
                server.stop();
            }
        }
    }

    private static void configureLogContext(LogContext embeddedLogContext, File jbossHome) {
        WildFlySecurityManager.setPropertyPrivileged("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        File standaloneDir =  new File(jbossHome, "standalone");
        File configDir =  new File(standaloneDir, "configuration");
        File logDir =  new File(standaloneDir, "log");
        File bootLog = new File(logDir, "boot.log");
        File loggingProperties = new File(configDir, "logging.properties");
        if (loggingProperties.exists()) {
            WildFlySecurityManager.setPropertyPrivileged("org.jboss.boot.log.file", bootLog.getAbsolutePath());
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(loggingProperties);
                new PropertyConfigurator(embeddedLogContext).configure(fis);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                StreamUtils.safeClose(fis);
            }
        }
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

    private static boolean isSuccess(ModelNode operationResponse) {
        if(operationResponse != null) {
            return operationResponse.hasDefined("outcome") && operationResponse.get("outcome").asString().equals("success");
        }
        return false;
    }

}
