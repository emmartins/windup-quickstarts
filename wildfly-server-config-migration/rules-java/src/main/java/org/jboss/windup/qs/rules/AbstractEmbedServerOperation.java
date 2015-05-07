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

import org.jboss.dmr.ModelNode;
import org.ocpsoft.rewrite.config.Operation;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.event.Rewrite;
import org.wildfly.core.embedded.StandaloneServer;

/**
 * @author emmartins
 */
public abstract class AbstractEmbedServerOperation implements Operation {

    private final ModelNode op;

    public AbstractEmbedServerOperation(ModelNode op) {
        this.op = op;
    }

    @Override
    public void perform(Rewrite rewrite, EvaluationContext evaluationContext) {
        try {
            // start embedded server
            final StandaloneServer server = EmbedServerUtils.startStandaloneServer();
            try {
                // invoke server and pass result to concrete op impl 
                perform(rewrite, evaluationContext, server.getModelControllerClient().execute(op));
            } finally {
                // stop the server
                if (server != null) {
                    server.stop();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void perform(Rewrite rewrite, EvaluationContext evaluationContext, ModelNode operationResult);

}
