/**
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.hybris.merchandising.yaas.client;

import java.util.List;

import com.hybris.platform.merchandising.yaas.CategoryHierarchy;

/**
 * This is a wrapper class for the generated {@link CategoryHierarchy} object.
 *
 */
public class CategoryHierarchyWrapper {
	List<CategoryHierarchy> categories;

	/**
	 * Default constructor, accepts a list of {@link CategoryHierarchy} objects.
	 * @param categories category list to store.
	 */
	public CategoryHierarchyWrapper(final List<CategoryHierarchy> categories) {
		this.categories = categories;
	}

	/**
	 * Retrieves the list of {@link CategoryHierarchy} objects stored within this wrapper.
	 * @return the categories stored.
	 */
	public List<CategoryHierarchy> getCategories() {
		return categories;
	}
}
