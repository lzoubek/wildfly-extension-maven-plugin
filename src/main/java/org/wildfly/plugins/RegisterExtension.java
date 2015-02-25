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
package org.wildfly.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class RegisterExtension {
	
	final Log log;
	
	public RegisterExtension(Log log) {
		this.log = log;
	}

	/**
	 * registers extension to standalone.xml
	 * @param options
	 * @param destFile output file (standalone.xml or domain.xml with new stuff)
	 * @param moduleId ID of JBoss Module to register as an JBoss extension
	 * @throws Exception
	 */
	public void register(RegisterOptions options) throws Exception {
		List<Insert> inserts = new ArrayList<Insert>();
		inserts.addAll(Arrays.asList(options.getInserts()));
		if (options.getModuleId() != null) {
			log.info("Register extension module="+options.getModuleId());		
			inserts.add(new Insert("/server/extensions","<extension module=\""+options.getModuleId()+"\"/>"));	
		}
		
		if (options.getSubsystem() != null) {
			inserts.add(new Insert("/server/profile", options.getSubsystem()));
		}
		if (options.getSocketBindingGroups() != null && options.getSocketBinding() != null) {
			for (String group : options.getSocketBindingGroups()) {
				inserts.add(new Insert("/server/socket-binding-group[@name='"+group+"']",options.getSocketBinding())
				.withAttribute("name"));
			}
		}
		new XmlConfigBuilder(this.log, options.getServerConfigBackup(), options.getServerConfig())
		.inserts(inserts)
		.failNoMatch(options.isFailNoMatch())
		.build();
		
		log.info("New serverConfig file written to ["+options.getServerConfig().getAbsolutePath()+"]");
	}



}
