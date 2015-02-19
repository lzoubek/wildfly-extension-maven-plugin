package org.wildfly.plugins;

public class Remove {

	private String select;
	
	public Remove() {
		
	}
	
	public Remove(String select) {
		this.select = select;
	}
	
	public void setSelect(String select) {
		this.select = select;
	}
	
	public String getSelect() {
		return select;
	}
}
