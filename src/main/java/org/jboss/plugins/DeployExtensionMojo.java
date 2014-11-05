/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.plugins;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which deploys JBoss module to JBoss AS7/WildFly server
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.INSTALL)
public class DeployExtensionMojo extends AbstractMojo {

	/**
	 * Location of JBoss input module.zip This file should have JBoss module directory structure so it can be laid down 
	 * to modulesHome directory
	 */
	@Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-module.zip", required = true)
	private File moduleZip;

	/**
	 * Location of AS7/WildFly server to deploy to
	 */
	@Parameter(defaultValue = "${jboss.home}", required = true)
	private File jbossHome;
	
	/**
	 * Location of modules home (either relative to jbossHome or absolute). Set this value unless your structure inside moduleZip does not include path to modules.
	 * For Wildfly, this path is "modules/system/layers/base", for older AS7 versions it's just "modules".
	 */
	@Parameter(defaultValue = "")
	private String modulesHome;
	
	/**
	 * Location of server configuration file standalone.xml or domain.xml to write to (can be either relative to jbossHome or absolute)
	 */
	@Parameter(defaultValue = "standalone/configuration/standalone.xml", required = true)
	private String serverConfig;
	
	/**
	 * List of server profiles to set subsystem on (only applies when serverConfig is domain.xml) 
	 */
	@Parameter
	private String[] profiles;

	
	/**
	 * Location of subsystem content to be inserted into standalone.xml
	 */
	@Parameter(defaultValue = "${basedir}/src/main/resources/subsystem.xml", required = true)
	private File subsystem;
	
	/**
	 * Location of socket-binding content to be inserted into standalone.xml
	 */
	@Parameter(defaultValue = "${basedir}/src/main/resources/socket-binding.xml", required = true)
	private File socketBinding;
	
	/**
	 * List of socket-binding-groups to set socketBinding in (only applies when socketBinding exists)
	 * Default : ["standard-sockets"]
	 */
	@Parameter
	private String[] socketBindingGroups = new String[] {"standard-sockets"};
	
	 /**
     * Whether to skip the execution of this mojo.
     */
    @Parameter(defaultValue = "false")
    private boolean skipDeploy;
	
	private File serverConfigAbsolute;
	private File modulesHomeAbsolute;

	public void execute() throws MojoExecutionException,MojoFailureException {
		if (skipDeploy) {
			getLog().info("Skipped execution");
			return;
		}
		validConfiguration();
		JBossModule module = null;
		try {
			module = JBossModule.readFromZipFile(getLog(), moduleZip);
		}
		catch (Exception e) {
			throw new MojoFailureException("Failed to read module : "+e.getMessage());
		}

		try {
			module.installTo(modulesHomeAbsolute);	
		}	
		catch (Exception e) {
			throw new MojoFailureException("Failed to install module : "+e.getMessage());
		}

		try {
			module.registerExtension(new RegisterOptions()
				.serverConfig(serverConfigAbsolute)
				.profiles(profiles)
				.subsystem(subsystem)
				.socketBinding(socketBinding)
				.socketBindingGroups(socketBindingGroups)
			);
			
		} catch (Exception e) {
			throw new MojoFailureException("Failed to register module : "+e.getMessage());
		}	
	}
	
	
	private void validConfiguration() throws MojoFailureException {
		if (!(jbossHome.exists() && jbossHome.isDirectory() && jbossHome.canRead())) {
			throw new MojoFailureException("jbossHome = "+jbossHome.getAbsolutePath()+" is not readable and existing directory");
		}
		if (!new File(jbossHome,"modules").isDirectory()) {
			throw new MojoFailureException("jbossHome = "+jbossHome.getAbsolutePath()+" does not seem to point to AS7/WildFly installation dir");
		}
		
		if (new File(serverConfig).isAbsolute()) {
			serverConfigAbsolute = new File(serverConfig);
		} else {
			serverConfigAbsolute = new File(jbossHome,serverConfig);
		}
		if (!(serverConfigAbsolute.exists() && serverConfigAbsolute.isFile() && serverConfigAbsolute.canWrite())) {
			throw new MojoFailureException("serverConfig = "+serverConfig+" is not writable and existing file. [serverConfig] must be either absolute path or relative to [jbossHome]");
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
			throw new MojoFailureException("modulesHome = "+modulesHome+" is not writable and existing directory. [modulesHome] must be either absolute path or relative to [jbossHome]");
		}
	}
}
