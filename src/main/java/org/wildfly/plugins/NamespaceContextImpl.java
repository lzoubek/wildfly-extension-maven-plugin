package org.wildfly.plugins;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class NamespaceContextImpl implements NamespaceContext {
	
	private final Map<String,String> mapping = new HashMap<String,String>();
	
	public NamespaceContextImpl mapping(String prefix, String namespaceURI) {
		mapping.put(prefix, namespaceURI);
		return this;
	}

	public String getNamespaceURI(String prefix) {
		return mapping.get(prefix);
	}

	public String getPrefix(String namespaceURI) {
		return null;
	}

	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}

}
