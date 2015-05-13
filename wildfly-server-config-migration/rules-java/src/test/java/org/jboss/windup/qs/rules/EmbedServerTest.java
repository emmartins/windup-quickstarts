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
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.core.embedded.StandaloneServer;

/**
 * @author emmartins
 */
public class EmbedServerTest {

    @Test
    public void test() throws Exception {
        // start embedded server
        final StandaloneServer server = EmbedServerUtils.startStandaloneServer();
        try {
            // retrieve the model controller client
            final ModelControllerClient mcc = server.getModelControllerClient();
            // execute op to list subsystems
            ModelNode op = new ModelNode();
            op.get("operation").set("read-children-names");
            op.get("child-type").set("subsystem");
            op.get("recursive").set(true);
            op.get("operations").set(true);
            System.out.println(mcc.execute(op).toString());
        } finally {
            if (server != null) {
                server.stop();
            }
        }

    } 
        
}
