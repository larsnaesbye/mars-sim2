/*
 * Mars Simulation Project
 * NaturalAttributeManager.java
 * @date 2023-11-30
 * @author Scott Davis
 */

package com.mars_sim.core.person.ai;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import com.mars_sim.core.tool.RandomUtil;

/**
 * The NaturalAttributeManager class manages a person's natural attributes.
 * There is only natural attribute manager for each person.
 */
public class NaturalAttributeManager implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** A table of the person's natural attributes keyed by its type. */
	private Map<NaturalAttributeType, Integer> attributeMap;
	
	/**
	 * Construction an attribute manager with an initial set
	 * @param initialAttr Initial attribute to load; is optional
	 */
	public NaturalAttributeManager(Map<NaturalAttributeType, Integer> initialAttr) {
		attributeMap = new EnumMap<>(NaturalAttributeType.class);
		if (initialAttr != null) {
			for(var e : initialAttr.entrySet()) {
				setAttribute(e.getKey(), e.getValue());
			}	
		}
	}


	/**
	 * Modify an attribute.
	 * 
	 * @param attributeName the name of the attribute
	 * @param modifier      a positive or negative random number ceiling 
	 */
	public void addAttributeModifier(NaturalAttributeType attributeName, int modifier) {
		int random = RandomUtil.getRandomInt(Math.abs(modifier));
		if (modifier < 0)
			random *= -1;
		setAttribute(attributeName, getAttribute(attributeName) + random);
	}

	/**
	 * Adjust an attribute.
	 * 
	 * @param attributeName the name of the attribute
	 * @param modifier      the modifier 
	 */
	public void adjustAttribute(NaturalAttributeType attributeName, int modifier) {
		setAttribute(attributeName, getAttribute(attributeName) + modifier);
	}
	
	/**
	 * Returns the number of natural attributes.
	 * 
	 * @return the number of natural attributes
	 */
	public int getAttributeNum() {
		return attributeMap.size();
	}

	/**
	 * Gets the integer value of a named natural attribute if it exists. Returns 0
	 * otherwise.
	 * 
	 * @param attribute 
	 */
	public int getAttribute(NaturalAttributeType attribute) {
		int result = 0;
		if (attributeMap.containsKey(attribute))
			result = attributeMap.get(attribute);
		return result;
	}

	/**
	 * Sets an attribute's level.
	 * 
	 * @param attrib {@link NaturalAttributeType} the attribute
	 * @param level  the level the attribute is to be set
	 */
	public void setAttribute(NaturalAttributeType attrib, int level) {
		if (level > 100)
			level = 100;
		if (level < 0)
			level = 0;
		attributeMap.put(attrib, level);
	}

	public Map<NaturalAttributeType, Integer> getAttributeMap() {
		return attributeMap;
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		attributeMap.clear();
		attributeMap = null;
	}
}
