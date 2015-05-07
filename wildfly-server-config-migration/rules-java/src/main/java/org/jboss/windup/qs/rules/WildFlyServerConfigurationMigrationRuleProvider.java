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
import org.jboss.windup.config.AbstractRuleProvider;
import org.jboss.windup.config.metadata.RuleMetadata;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.rules.apps.xml.condition.XmlFile;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Operation;

/**
 * @author emmartins
 */
@RuleMetadata(tags = {"java-ee"})
public class WildFlyServerConfigurationMigrationRuleProvider extends AbstractRuleProvider {
    @Override
    public Configuration getConfiguration(GraphContext context) {
        // op to list subsystems as hint
        final ModelNode modelNode = new ModelNode();
        modelNode.get("operation").set("read-children-names");
        modelNode.get("child-type").set("subsystem");
        modelNode.get("recursive").set(true);
        modelNode.get("operations").set(true);
        final Operation operation = new HintEmbedServerOperation(modelNode);
        return ConfigurationBuilder.begin()
                .addRule()
                .when(
                        XmlFile.matchesXpath("/ns:server")
                                .namespace("ns", "urn:jboss:domain:3.0")
                                .as("wildflyStandaloneConfig")
                )
                .perform(
                        Iteration.over("wildflyStandaloneConfig").perform(operation).endIteration()
                );
    }
}
