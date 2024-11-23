/*
 * Mars Simulation Project
 * TypeGenerator.java
 * @date 2024-02-23
 * @author Barry Evans
 */
package com.mars_sim.tools.helpgenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import com.github.mustachejava.Mustache;
import com.mars_sim.core.process.ProcessItem;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.tools.helpgenerator.GenericsGrouper.NamedGroup;
import com.mars_sim.tools.helpgenerator.HelpContext.ItemQuantity;
import com.mars_sim.tools.helpgenerator.HelpContext.ResourceUse;

/**
 * This is an abstract generator for a configuration type. It provides generic methods.
 */
public abstract class TypeGenerator<T> {
    private static Logger logger = Logger.getLogger(TypeGenerator.class.getName());

    // CReate an empty reosurce use
    protected static final ResourceUse EMPTY_USE = HelpContext.buildEmptyResourceUse();

    private HelpContext parent;
    private String typeName;
    private String title;
    private String description;

    private Mustache detailsTemplate;

    // Used to group entities for grouped index
    private Function<T,String> grouper;

    private String groupName;

    /**
     * Converts a set of Process inputs/outputs to a generic item quantity
     */
    protected static List<ItemQuantity> toQuantityItems(List<ProcessItem> list) {
		return list.stream()
					.sorted((o1, o2)->o1.getName().compareTo(o2.getName()))
					.map(v -> HelpContext.createItemQuantity(v.getName(), v.getType(), v.getAmount()))
					.toList();
	}

    
    /**
     * Converts a set of resource value pairs of a specific type to a generic item quantity
     */
    protected static List<ItemQuantity> toQuantityItems(Map<String,Integer> items, ItemType type) {
		return items.entrySet().stream()
					.map(v -> HelpContext.createItemQuantity(v.getKey(), type, v.getValue()))
					.toList();
	}

    /**
     * Create an instance.
     * @param parent The parent generator to provide context.
     * @param typeName The type name used for the folder and reference.
     * @param title Title in  the index
     * @param description Description of the entity being rendered.
     */
    protected TypeGenerator(HelpContext parent, String typeName, String title, String description) {
        this.typeName = typeName;
        this.parent = parent;
        this.title = title;
        this.description = description;
        this.detailsTemplate = parent.getTemplate(typeName + "-detail");
    }

    /**
     * Get the title of the type. This is called from Mustache templates.
     * @return the Type name
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the description of the type. This is called from Mustache templates.
     * @return the description name
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the name of the type. This is called from Mustache templates.
     * @return the Type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Define an grouper to create a groupe index page.
     * @param grouper
     */
    protected void setGrouper(String name, Function<T,String> grouper) {
        this.groupName = name;
        this.grouper = grouper;
    }
    
    protected HelpContext getParent() {
        return parent;
    }

    /**
     * Creates an index page for the given entities. This can be overriden for a specialised index
     * @param entities List of entities
     * @param targetDir Target folder for output
     * @throws IOException
     */
	private void createIndex(List<T> entities, File targetDir)
		throws IOException {
        if (grouper == null) {
            parent.createFlatIndex(title, description, entities, typeName, targetDir);
        }
        else {
            List<NamedGroup<T>> groups = GenericsGrouper.getGroups(entities, grouper);
            parent.createGroupedIndex(title, description, groupName, groups, typeName, targetDir);
        }
	}

    /**
	 * Generate the files for all the entities of this type including an index.
     * @param outputDir The top levle folder of generated files
	 * @throws IOException
	 */
	public void generateAll(File outputDir) throws IOException {
		logger.info("Generating files for " + typeName);

		File targetDir = new File(outputDir, typeName);
		targetDir.mkdirs();

		List<T> vTypes = getEntities(); 
	
		// Create index
		createIndex(vTypes, targetDir);

		// Individual entity pages
		for(T v : vTypes) {
            File targetFile = new File(targetDir, parent.generateFileName(getEntityName(v)));
            try(FileOutputStream dest = new FileOutputStream(targetFile)) {
                generateEntity(v, dest);
            }
		}
	}

    /**
	 * Prepare the scope to support the process-inout template
	 * @param scope Scop of properties
	 * @param inputTitle Title of inputs
	 * @param inputs
	 * @param outputTitle Title of outputs
	 * @param outputs
	 */
	protected void addProcessInputOutput(Map<String, Object> scope,
			String inputTitle, List<ItemQuantity> inputs,
			String outputTitle, List<ItemQuantity> outputs) {

		scope.put("inputs", inputs);
		scope.put("inputsTitle", inputTitle);
		scope.put("outputs", outputs);
		scope.put("outputsTitle", outputTitle);
	}

    /**
	 * Prepare the scope to support the process-flow template for a specific resource. Adds
     * the required props to hold any processes that uses the resource as either an input or output
     * @param resourceName Name of the resource for flows
	 * @param scope Scope of properties
	 */
    protected void addProcessFlows(String resourceName, Map<String, Object> scope) {
		var resourceUsed = getParent().getResourceUsageByName(resourceName);

		if (resourceUsed == null) {
            resourceUsed = EMPTY_USE;
        }
        scope.put("inputProcesses", resourceUsed.asInput());
        scope.put("outputProcesses", resourceUsed.asOutput());	
    }

    /**
     * Generate a help page for a specific entity using the context generator.
     * @param e Entity to render
     * @param output Destinatino of content
     * @throws IOException
     */
    public void generateEntity(T e, OutputStream output) throws IOException
    {
        // Add base properties
        var vScope = parent.createScopeMap(description + " - " + getEntityName(e));
        vScope.put(typeName, e);
        vScope.put("type.title", title);

        // Add any customer properties
        addEntityProperties(e, vScope);

        // Generate the file
        parent.generateContent(detailsTemplate, vScope, output);
    }

    /**
     * Add any entity specific properties. Should be overriden by subclasses
     * @param entity The entity to display
     * @param scope Scope of the properties to use for the template
     * @return
     */
    protected void addEntityProperties(T entity, Map<String,Object> scope) {
        // Default implement needs no extra properties
    }

    /**
     * Get a list of the entities to be rendered
     * @return
     */
    protected abstract List<T> getEntities();
    
    /**
     * Get the identifable/unique name for this entity.
     * If there was a common internface this would not be required.
     * @param v entity
     * @return
     */
    protected abstract String getEntityName(T v);

}