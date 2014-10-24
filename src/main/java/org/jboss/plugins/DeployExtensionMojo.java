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

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	 * Location of JBoss input module
	 */
	@Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-module.zip", required = true)
	private File moduleZip;

	/**
	 * Location of AS7/WildFly server to deploy to
	 */
	@Parameter(defaultValue = "${jboss.home}", required = true)
	private File jbossHome;
	
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
     * Whether to skip the execution of this mojo.
     */
    @Parameter(defaultValue = "false")
    private boolean skipDeploy;
	
	private File serverConfigAbsolute;

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
			module.installTo(jbossHome);	
		}	
		catch (Exception e) {
			throw new MojoFailureException("Failed to install module : "+e.getMessage());
		}

		try {
			module.registerExtension(serverConfigAbsolute, profiles, subsystem);
			
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
			throw new MojoFailureException("standaloneXml = "+serverConfig+" is not writable and existing file. [standaloneXml] must be either absolute path or relative to [jbossHome]");
		}
	}
}
