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

/**
 * this class just holds all inputs for changes to be made in standalone.xml or domain.xml
 * @author lzoubek
 *
 */
public class RegisterOptions {

	private File serverConfig;
	private File subsystem;
	private String[] profiles;
	private File socketBinding;
	private String[] socketBindingGroups;
	
	public RegisterOptions() {
		
	}

	public RegisterOptions serverConfig(File serverConfig) {
		this.serverConfig = serverConfig;
		return this;
	}

	public RegisterOptions subsystem(File subsystem) {
		this.subsystem = subsystem;
		return this;
	}

	public RegisterOptions socketBinding(File socketBinding) {
		this.socketBinding = socketBinding;
		return this;
	}

	public RegisterOptions profiles(String[] profiles) {
		this.profiles = profiles;
		return this;
	}
	
	public RegisterOptions socketBindingGroups(String[] socketBindingGroups) {
		this.socketBindingGroups = socketBindingGroups;
		return this;
	}

	public String[] getProfiles() {
		return profiles;
	}

	public File getServerConfig() {
		return serverConfig;
	}

	public File getSocketBinding() {
		return socketBinding;
	}

	public File getSubsystem() {
		return subsystem;
	}
	public String[] getSocketBindingGroups() {
		return socketBindingGroups;
	}
	
}
