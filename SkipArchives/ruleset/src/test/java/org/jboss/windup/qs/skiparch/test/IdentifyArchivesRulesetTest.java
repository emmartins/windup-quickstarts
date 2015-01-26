package org.jboss.windup.qs.skiparch.test;

import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.exec.WindupProcessor;
import org.jboss.windup.exec.configuration.WindupConfiguration;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.GraphContextFactory;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.qs.identarch.model.GAV;
import org.jboss.windup.qs.identarch.model.GAVModel;
import org.jboss.windup.qs.identarch.model.IdentifiedArchiveModel;
import org.jboss.windup.qs.skiparch.SkipArchivesRules;
import org.jboss.windup.qs.skiparch.test.rulefilters.EnumerationOfRulesFilter;
import org.jboss.windup.qs.skiparch.test.rulefilters.RuleFilter;
import org.jboss.windup.util.Logging;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the Victims ruleset.
 *
 * @author Ondrej Zizka, ozizka at redhat.com
 */
@RunWith(Arquillian.class)
@Ignore
public class IdentifyArchivesRulesetTest
{
    private static final Logger log = Logging.get(IdentifyArchivesRulesetTest.class);

    @Deployment
    @Dependencies({
        @AddonDependency(name = "org.jboss.windup.config:windup-config"),
        @AddonDependency(name = "org.jboss.windup.exec:windup-exec"),
        @AddonDependency(name = "org.jboss.windup.utils:utils"),
        @AddonDependency(name = "org.jboss.windup.rules.apps:rules-java"),
        @AddonDependency(name = "org.jboss.windup.reporting:windup-reporting"),
        @AddonDependency(name = "org.jboss.windup.quickstarts:windup-skiparchives"),
        @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
    })
    public static ForgeArchive getDeployment()
    {
        final ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
            .addBeansXML()
            //.addClasses(SkipArchivesRules.class, EnumerationOfRulesFilter.class, OrPredicate.class, )
            .addPackages(true,"org.jboss.windup.qs.victims.test")
            .addAsAddonDependencies(
                AddonDependencyEntry.create("org.jboss.windup.config:windup-config"),
                AddonDependencyEntry.create("org.jboss.windup.exec:windup-exec"),
                AddonDependencyEntry.create("org.jboss.windup.utils:utils"),
                AddonDependencyEntry.create("org.jboss.windup.rules.apps:rules-java"),
                AddonDependencyEntry.create("org.jboss.windup.reporting:windup-reporting"),
                AddonDependencyEntry.create("org.jboss.windup.quickstarts:windup-skiparchives"),
                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi")
            );
        return archive;
    }


    @Inject
    private WindupProcessor processor;

    @Inject
    private GraphContextFactory contextFactory;

    @Test
    public void testJarsAreIdentified()
    {
        try (GraphContext grCtx = contextFactory.create())
        {


            // Create some identifiable archives.
            GraphService<IdentifiedArchiveModel> archGS = new GraphService(grCtx, IdentifiedArchiveModel.class);
            IdentifiedArchiveModel archM = archGS.create();
            // 4e031bb61df09069aeb2bffb4019e7a5034a4ee0 junit:junit:4.11
            archM.setSHA1Hash("4e031bb61df09069aeb2bffb4019e7a5034a4ee0");

            //GraphService<GAVModel> gavGS = new GraphService(grCtx, GAVModel.class);
            //GAVModel gavM = gavGS.create().setGroupId("junit").setArtifactId("junit").setVersion("4.11");
            //archM.setGAV(gavM);

            // Run the SkipArchivesRules.
            runRule(SkipArchivesRules.class, grCtx);

            // Check if the archives were identified.

            // Something found for that SHA1.
            GAVModel gav = archM.getGAV();
            Assert.assertNotNull(gav);

            // Correctly identified.
            final GAV expectedGav = GAV.fromSHA1AndGAV("4e031bb61df09069aeb2bffb4019e7a5034a4ee0 junit:junit:4.11");
            Assert.assertEquals(expectedGav, gav);

        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }


    /**
     * @param ruleProvider RuleProvider to run, with all rules that must be run before.
     */
    private void runRule(Class<? extends WindupRuleProvider> ruleProvider, GraphContext grCtx)
    {
        this.runRules(new EnumerationOfRulesFilter(ruleProvider), grCtx);
    }


    /**
     * TODO: Move this to some test utils?
     */
    private void runRules(RuleFilter ruleFilter, GraphContext grCtx)
    {
        try
        {
            // Windup config.
            WindupConfiguration wc = new WindupConfiguration();
            wc.setGraphContext(grCtx);

            wc.setRuleProviderFilter(ruleFilter);
            //wc.setInputPath(Paths.get("src/test/resources/xercesImpl-2.9.1.jar.war"));
            wc.setOutputDirectory(Paths.get("target/WindupReport"));

            // Run.
            processor.execute(wc);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}