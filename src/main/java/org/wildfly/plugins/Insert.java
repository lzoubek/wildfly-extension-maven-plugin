package org.wildfly.plugins;

import java.io.File;
/**
 * An insert item represents 1 edit action to be performed on target XML document. Each 'insert' has {@link #select} attribute. Which denotes
 * location of content you wish to insert/replace. Then it must set either {@link #content} - path to location of another XML file that will 
 * appear as a child of elements evaluated by {@link #select} expression or {@link #xml} - which denotes XML content as String. Optionally, {@link attribute}
 * can be defined to identify inserted/replaced content. If {@link attribute} is not defined, content (defined either via {@link #xml} or {@link content}) is loaded
 * and xpath expression is created from root element's attributes and their values, otherwise {@link attribute} is taken as the only one for xpath expression.  
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
		return  new StringBuilder("insert [")
		.append("select="+this.select)
		.append(" content="+this.content)
		.append(" xml="+this.xml)
		.append("]").toString();
	}
	
}
