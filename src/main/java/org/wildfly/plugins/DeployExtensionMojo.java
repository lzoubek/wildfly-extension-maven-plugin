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
package org.wildfly.plugins;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.FileUtils;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Goal which deploys JBoss module to JBoss AS7/WildFly server
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyCollection = ResolutionScope.COMPILE)
public class DeployExtensionMojo extends AbstractMojo {

    /**
     * Location of JBoss input module.zip This file should have JBoss module
     * directory structure so it can be laid down to {@link #modulesHome}
     * directory.
     */
    @Parameter()
    private File moduleZip;

    /**
     * An alternative to {@link #moduleZip} parameter. This parameter has higher
     * priority than {@link #moduleZip}. A string of the form
     * groupId:artifactId:version[:packaging][:classifier]. Default for
     * packaging is <strong>zip</strong> and for classifier it's
     * <strong>module</strong>
     * 
     * @since 0.7
     */
    @Parameter()
    private String artifact;

    /**
     * Location of AS7/WildFly server to deploy to
     */
    @Parameter(defaultValue = "${jboss.home}", required = true)
    private File jbossHome;

    /**
     * Location of modules home (either relative to {@link #jbossHome} or
     * absolute). Set this value unless your structure inside {@link #moduleZip}
     * does not include path to modules. For Wildfly, this path is
     * "modules/system/layers/base", for older AS7 versions it's just "modules".
     */
    @Parameter(defaultValue = "modules/system/layers/base")
    private String modulesHome;

    /**
     * Location of server configuration file (standalone.xml) write to (can be
     * either relative to jbossHome or absolute)
     */
    @Parameter(defaultValue = "standalone/configuration/standalone.xml")
    private String serverConfig;

    /**
     * Location where plugin will backup original server configuration file
     * (standalone.xml) - can be either relative to {@link #jbossHome} or
     * absolute
     */
    @Parameter(defaultValue = "standalone/configuration/standalone.xml.old")
    private String serverConfigBackup;

    /**
     * Location of subsystem content to be inserted into standalone.xml
     */
    @Parameter()
    private File subsystem;

    /**
     * Location of socket-binding content to be inserted into standalone.xml
     */
    @Parameter()
    private File socketBinding;

    /**
     * List of socket-binding-groups to set socketBinding in (only applies when
     * socketBinding exists) Default : ["standard-sockets"]
     */
    @Parameter
    private String[] socketBindingGroups = new String[] { "standard-sockets" };

    /**
     * List of data to be inserted to {@link #serverConfig}. This is pretty
     * powerful stuff to put/replace any XML content anywhere in
     * {@link #serverConfig}
     */
    @Parameter
    private Insert[] edit;

    /**
     * Fails the build if any of <strong>select</strong> expression within
     * <strong>edit</strong> does not match any node (thus it wouldn't update
     * serverConfig)
     */
    @Parameter
    private boolean failNoMatch;

    /**
     * Whether to skip the execution of this mojo.
     */
    @Parameter(defaultValue = "false")
    private boolean skipDeploy;

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private MojoExecution mojoExecution;

    private File serverConfigAbsolute;
    private File serverConfigBackupAbsolute;
    private File modulesHomeAbsolute;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipDeploy) {
            getLog().info("Skipped execution");
            return;
        }
        validConfiguration();

        if (artifact != null) {
            moduleZip = resolveArtifactModuleZip();
        }

        JBossModule module = null;
        if (moduleZip != null) {
            try {
                module = JBossModule.readFromZipFile(getLog(), moduleZip);
            } catch (Exception e) {
                throw new MojoFailureException("Failed to read module : " + e.getMessage());
            }

            try {
                module.installTo(modulesHomeAbsolute);
            } catch (Exception e) {
                throw new MojoFailureException("Failed to install module : " + e.getMessage());
            }
        }
        // stay silent if the module.zip does not exist

        try {
            RegisterOptions options = new RegisterOptions();
            if (module != null) {
                options.withExtension(module.getModuleId());
            }

            options.serverConfig(serverConfigAbsolute).serverConfigBackup(serverConfigBackupAbsolute).subsystem(subsystem)
                    .socketBinding(socketBinding).socketBindingGroups(socketBindingGroups).inserts(edit).failNoMatch(failNoMatch);

            register(options);

        } catch (Exception e) {
            getLog().error(e);
            throw new MojoFailureException("Failed to update server configuration file : " + e.getMessage());
        }
    }

    private File resolveArtifactModuleZip() throws MojoExecutionException {
        if (this.artifact == null) {
            return null;
        }
        getLog().info("Invoking maven-dependency-plugin:copy to pull " + artifact);

        String gav = String.valueOf(this.artifact);
        // there's no way to set output file name, so we need to parse artifact
        // we now how output file will look like because we call
        // maven-dependency-plugin with stripClassifier=true and
        // stripVersion=true
        final String tmpOutputDir = "${project.build.directory}/tmp/wildfly-extension-plugin";
        String[] pieces = gav.split(":");
        String moduleTempFile = pieces[1];
        if (pieces.length > 3) { // packaging was specified
            moduleTempFile += "." + pieces[3];
        } else {
            // fill defaults
            moduleTempFile += ".zip";
            gav += ":zip:module";
        }
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.10")),
                goal("copy"),
                configuration(
                        element(name("outputDirectory"), tmpOutputDir), 
                        element(name("overWriteIfNewer"), "true"),
                        element(name("overWriteReleases"), "true"), 
                        element(name("overWriteSnapshots"), "true"),
                        element(name("stripClassifier"), "true"),
                        element(name("stripVersion"), "true"), 
                        element(name("artifact"), gav)),
                executionEnvironment(mavenProject, mavenSession, pluginManager));

        moduleTempFile = String.valueOf(evalPluginParameterExpression(tmpOutputDir + "/" + moduleTempFile));
        return new File(moduleTempFile);
    }

    private Object evalPluginParameterExpression(String expression) {
        PluginParameterExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(mavenSession, mojoExecution);
        try {
            return evaluator.evaluate(expression);
        } catch (ExpressionEvaluationException e) {
            getLog().error("Failed to evaluate [" + expression + "]", e);
        }
        return null;
    }

    public void register(RegisterOptions options) throws Exception {
        File serverConfig = options.getServerConfig();
        getLog().info(
                "Backup original serverConfig [" + serverConfig.getAbsolutePath() + "] to [" + options.getServerConfigBackup().getAbsolutePath()
                        + "]");
        FileUtils.copyFile(serverConfig, options.getServerConfigBackup());
        new RegisterExtension(getLog()).register(options);
    }

    private void validConfiguration() throws MojoFailureException {
        if (!(jbossHome.exists() && jbossHome.isDirectory() && jbossHome.canRead())) {
            throw new MojoFailureException("jbossHome = " + jbossHome.getAbsolutePath() + " is not readable and existing directory");
        }
        if (!new File(jbossHome, "modules").isDirectory()) {
            throw new MojoFailureException("jbossHome = " + jbossHome.getAbsolutePath() + " does not seem to point to AS7/WildFly installation dir");
        }

        if (new File(serverConfig).isAbsolute()) {
            serverConfigAbsolute = new File(serverConfig);
        } else {
            serverConfigAbsolute = new File(jbossHome, serverConfig);
        }
        if (!(serverConfigAbsolute.exists() && serverConfigAbsolute.isFile() && serverConfigAbsolute.canWrite())) {
            throw new MojoFailureException("serverConfig = " + serverConfig
                    + " is not writable and existing file. [serverConfig] must be either absolute path or relative to [jbossHome]");
        }

        if (new File(serverConfigBackup).isAbsolute()) {
            serverConfigBackupAbsolute = new File(serverConfigBackup);
        } else {
            serverConfigBackupAbsolute = new File(jbossHome, serverConfigBackup);
        }
        if (!(serverConfigBackupAbsolute.getParentFile().exists() && serverConfigAbsolute.getParentFile().isDirectory() && serverConfigAbsolute
                .getParentFile().canWrite())) {
            throw new MojoFailureException(
                    "serverConfigBackup = "
                            + serverConfigBackup
                            + " 's parent directory does not exist or is writable. [serverConfigBackup] must be either absolute path or relative to [jbossHome]");
        }

        if (modulesHome == null) {
            modulesHome = "";
        }

        if (new File(modulesHome).isAbsolute()) {
            modulesHomeAbsolute = new File(modulesHome);
        } else {
            modulesHomeAbsolute = new File(jbossHome, modulesHome);
        }
        if (!(modulesHomeAbsolute.exists() && modulesHomeAbsolute.isDirectory() && modulesHomeAbsolute.canWrite())) {
            throw new MojoFailureException("modulesHome = " + modulesHome
                    + " is not writable and existing directory. [modulesHome] must be either absolute path or relative to [jbossHome]");
        }
    }
}
