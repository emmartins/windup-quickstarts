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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.reporting.model.InlineHintModel;
import org.jboss.windup.reporting.service.InlineHintService;
import org.jboss.windup.rules.apps.java.model.WindupJavaConfigurationModel;
import org.jboss.windup.rules.apps.java.service.WindupJavaConfigurationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(Arquillian.class)
public class WildFlyServerConfigurationMigrationRuleProviderTest {

    @Deployment
    @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
            @AddonDependency(name = "org.jboss.windup.utils:windup-utils"),
            @AddonDependency(name = "org.jboss.windup.config:windup-config"),
            @AddonDependency(name = "org.jboss.windup.rules.apps:windup-rules-java"),
            @AddonDependency(name = "org.jboss.windup.rules.apps:windup-rules-java-ee"),
            @AddonDependency(name = "org.jboss.windup.reporting:windup-reporting"),
            @AddonDependency(name = "org.jboss.windup.exec:windup-exec"),
            @AddonDependency(name = "org.jboss.windup.quickstarts:wildfly-server-config-migration-rules-java"),
    })
    public static ForgeArchive getDeployment() {
        final ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
                .addBeansXML()
                .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.windup.utils:windup-utils"),
                        AddonDependencyEntry.create("org.jboss.windup.config:windup-config"),
                        AddonDependencyEntry.create("org.jboss.windup.rules.apps:windup-rules-java"),
                        AddonDependencyEntry.create("org.jboss.windup.rules.apps:windup-rules-java-ee"),
                        AddonDependencyEntry.create("org.jboss.windup.reporting:windup-reporting"),
                        AddonDependencyEntry.create("org.jboss.windup.exec:windup-exec"),
                        AddonDependencyEntry.create("org.jboss.windup.quickstarts:wildfly-server-config-migration-rules-java")
                );
        return archive;
    }

    @Inject
    private WindupProcessor processor;

    @Inject
    private GraphContextFactory contextFactory;

    @Test
    public void testRules() {
        Path outPath = Paths.get("target/WindupReport");
        try (GraphContext context = contextFactory.create(outPath)) {
            WindupJavaConfigurationModel javaCfg = WindupJavaConfigurationService.getJavaConfigurationModel(context);
            javaCfg.setSourceMode(true);
            WindupConfiguration wc = new WindupConfiguration();
            wc.setGraphContext(context);
            wc.setInputPath(Paths.get("../test-files/src_example"));
            wc.setOutputDirectory(outPath);
            processor.execute(wc);
            InlineHintService hintService = new InlineHintService(context);
            Iterable<InlineHintModel> hints = hintService.findAll();
            boolean javaRuleHintFound = false;
            for (InlineHintModel hint : hints)  {
                System.out.println("Hint: " + hint);
                if ((""+hint.getHint()).matches(".*WildFly.*"))  {
                    javaRuleHintFound = true;
                }
            }
            Assert.assertTrue("javaRuleHintFound", javaRuleHintFound);
        }  catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}