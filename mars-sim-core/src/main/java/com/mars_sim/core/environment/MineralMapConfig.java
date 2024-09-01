/*
 * Mars Simulation Project
 * MineralMapConfig.java
 * @date 2022-07-14
 * @author Scott Davis
 */

package com.mars_sim.core.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

public class MineralMapConfig {

	// Element names
	private static final String MINERAL = "mineral";
	private static final String COLOR = "color";
	private static final String NAME = "name";
	private static final String FREQUENCY = "frequency";
	private static final String LOCALE_LIST = "locale-list";
	private static final String LOCALE = "locale";

	private List<MineralType> mineralTypes;

	/**
	 * Constructor.
	 * 
	 * @param mineralDoc DOM document containing mineral configuration.
	 */
	public MineralMapConfig(Document mineralDoc) {
		buildMineralList(mineralDoc);
	}

	/**
	 * Gets a list of mineralTypes.
	 * 
	 * @return list of mineralTypes
	 * @throws Exception when mineralTypes can not be resolved.
	 */
	public List<MineralType> getMineralTypes() {
		return mineralTypes;
	}
	
	/**
	 * Builds the mineralTypes list.
	 * 
	 * @param configDoc
	 */
	private synchronized void buildMineralList(Document configDoc) {
		if (mineralTypes != null) {
			// just in case if another thread is being created
			return;
		}
			
		// Build the global list in a temp to avoid access before it is built
		List<MineralType> newList = new ArrayList<>();

		Element root = configDoc.getRootElement();
		List<Element> minerals = root.getChildren(MINERAL);
		for (Element mineral : minerals) {
			// Get mineral name.
			String name = mineral.getAttributeValue(NAME).trim();
			// Get color string.
			String color = mineral.getAttributeValue(COLOR).trim();
			// Get frequency.
			String frequency = mineral.getAttributeValue(FREQUENCY).trim();
			// Create mineralType.
			MineralType mineralType = new MineralType(name, color, frequency);
			// Get locales.
			Element localeList = mineral.getChild(LOCALE_LIST);
			List<Element> locales = localeList.getChildren(LOCALE);

			for (Element locale : locales) {
				String localeName = locale.getAttributeValue(NAME).trim();
				mineralType.addLocale(localeName);
			}

			// Add mineral type to newList.
			newList.add(mineralType);
		}

		// Assign the newList now built
		mineralTypes = Collections.unmodifiableList(newList);
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		if (mineralTypes != null) {
			mineralTypes = null;
		}
	}

	static class MineralType implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		String name;
		String color;
		String frequency;
		private List<String> locales;

		private MineralType(String name, String color, String frequency) {
			this.name = name;
			this.color = color;
			this.frequency = frequency;
			locales = new ArrayList<>(3);
		}

		private void addLocale(String localeName) {
			locales.add(localeName);
		}
		
		public List<String> getLocales() {
			return locales;
		}
		
		public String toString() {
			return name;
		}
		
		public String getColorString() {
			return color;
		}
	}
}
