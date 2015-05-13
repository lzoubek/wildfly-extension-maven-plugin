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

/**
 * An insert item represents 1 edit action to be performed on target XML
 * document. Each 'insert' has {@link #select} attribute. Which denotes location
 * of content you wish to insert/replace. Then it must set either
 * {@link #content} - path to location of another XML file that will appear as a
 * child of elements evaluated by {@link #select} expression or {@link #xml} -
 * which denotes XML content as String. Optionally, {@link attribute} can be
 * defined to identify inserted/replaced content. If {@link attribute} is not
 * defined, content (defined either via {@link #xml} or {@link content}) is
 * loaded and xpath expression is created from root element's attributes and
 * their values, otherwise {@link attribute} is taken as the only one for xpath
 * expression.
 * 
 * @author lzoubek
 * 
 */
public class Insert {

    private String select;
    private File content;
    private String xml;
    private String attribute;

    public Insert() {

    }

    public Insert(String select, String xml) {
        this.select = select;
        this.xml = xml;
    }

    public Insert(String select, File content) {
        this.select = select;
        this.content = content;
    }

    public Insert withAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getXml() {
        return xml;
    }

    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public File getContent() {
        return content;
    }

    public void setContent(File content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return new StringBuilder("insert [")
            .append("select=" + this.select)
            .append(content == null ? "" : " content=" + content)
            .append(attribute == null ? "" : " attribute=" + attribute)
            .append(xml == null ? "" : " xml=" + this.xml)
            .append("]")
            .toString();
    }

}
