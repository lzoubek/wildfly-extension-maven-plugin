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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class JBossModule {

	private final Log log;
	private File root;
	private String name;
	private boolean isZip = false;
	private List<String> resources = new ArrayList<String>();
	
	private JBossModule(Log log) {
		this.log = log;
	}
	
	public static JBossModule readFromZipFile(Log log, File moduleZip) throws Exception {
		JBossModule m = new JBossModule(log);
		m.isZip = true;
		m.root = moduleZip;
		if (!(moduleZip.canRead() && moduleZip.isFile())) {
			throw new FileNotFoundException("File "+moduleZip.getAbsolutePath()+" does not exist");
		}
		ZipInputStream zin = null;
        BufferedInputStream bin = null;
        boolean moduleXmlFound = false;
        try {
            bin = new BufferedInputStream(new FileInputStream(moduleZip));
            zin = new ZipInputStream(bin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().endsWith("module.xml")) {
                	moduleXmlFound = true;
                	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            		Document doc = dBuilder.parse(zin);
            		m.readModuleXmlInfo(doc);
                    break;
                }
            }
        } finally {
            try {
                // it should be enough to close the latest created stream in case of chained (nested) streams, @see http://www.javapractices.com/topic/TopicAction.do?Id=8
                if (zin != null) {
                    zin.close();
                }
            } catch (IOException ex) {
            }
        }
        if (!moduleXmlFound) {
        	throw new FileNotFoundException("module.xml was not found in "+moduleZip.getAbsolutePath());
        }
		return m;
	}
	
	private void readModuleXmlInfo(Document doc) throws Exception {
		this.name = doc.getDocumentElement().getAttribute("name");
		XPath xPath =  XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList) xPath.compile("//resources/resource-root").evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength();i++) {
			this.resources.add(nodeList.item(i).getAttributes().getNamedItem("path").getTextContent());
		}
	}
	
	public static JBossModule readFromDir(Log log,File rootDir) throws Exception {
		JBossModule m = new JBossModule(log);
		m.root = rootDir;
		File moduleFile = new File(rootDir, "module.xml");
		if (!moduleFile.exists() || !moduleFile.canRead()) {
			throw new FileNotFoundException("File "+moduleFile.getAbsolutePath()+" does not exist");
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(moduleFile);
		m.readModuleXmlInfo(doc);		
		return m;
	}
	
	public void installTo(File jbossHome) throws Exception {
		if (isZip) {
			ZipInputStream zin = null;
			FileOutputStream fos;
			log.info("Extracting module ["+this.root.getAbsolutePath()+"] to ["+jbossHome.getAbsolutePath()+"]");
	        try {
	            zin = new ZipInputStream(new FileInputStream(this.root));
	            ZipEntry ze = null;
	            while ((ze = zin.getNextEntry()) != null) {
	            	String fileName = ze.getName();
	                File newFile = new File(jbossHome + File.separator + fileName);
	                if (!ze.isDirectory()) {
		                log.info("Writing "+newFile.getAbsolutePath());
		                File parent = newFile.getParentFile();
		                if (!parent.exists()) {
		                	parent.mkdirs();
		                }
		                fos = new FileOutputStream(newFile);
		                IOUtil.copy(zin, fos);
		                IOUtil.close(fos);	                	
	                }
	            }
	        } finally {
	            try {
	                // it should be enough to close the latest created stream in case of chained (nested) streams, @see http://www.javapractices.com/topic/TopicAction.do?Id=8
	                if (zin != null) {
	                    zin.close();
	                }
	            } catch (IOException ex) {
	            }
	        }
		} else {
			File targetDir = new File(jbossHome,"modules"+File.separator+name.replaceAll("\\.", File.separator)+File.separator+"main");
			if (!targetDir.exists() && !targetDir.mkdirs()) {
				throw new MojoFailureException("Failed to create module directory "+targetDir.getAbsolutePath());
			}
			File moduleFile = new File(this.root, "module.xml");
			for (String r : resources) {
				File resource = new File(root,r);
				if (!(resource.canRead())) {
					throw new MojoFailureException("Resource file ["+r+"] referenced in ["+moduleFile.getAbsolutePath()+"] does not exist, fix ");
				}
				if (resource.isFile()) {
					FileUtils.copyFileToDirectory(resource, targetDir);
				} else if (resource.isDirectory()) {
					FileUtils.copyDirectoryStructure(resource, targetDir);
				}
			}
			FileUtils.copyFileToDirectory(moduleFile, targetDir);
		}

	}
	
	public void registerExtension(File serverConfig, File subsystem) throws Exception {
		File serverConfigBackup = new File(serverConfig.getParentFile(),serverConfig.getName()+".old");
		FileUtils.copyFile(serverConfig, serverConfigBackup);		
		new RegisterExtension(log).register(serverConfigBackup,serverConfig,subsystem,name);
	}
}
