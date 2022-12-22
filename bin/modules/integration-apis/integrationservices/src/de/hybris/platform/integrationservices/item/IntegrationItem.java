/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.integrationservices.item;

import java.util.Collection;

import java.util.Locale;

/**
 * Representation of a data item sent into the integration services or sent out of the integration services. Integration item
 * must correspond to one of the {@link de.hybris.platform.integrationservices.model.IntegrationObjectItemModel} definitions.
 * If {@link de.hybris.platform.integrationservices.model.IntegrationObjectItemModel} represents a type of the data structures,
 * then the {@code IntegrationItem} is a particular instance of that type carrying particular data values.
 */
public interface IntegrationItem
{
	/**
	 * Retrieves code of the {@link de.hybris.platform.integrationservices.model.IntegrationObjectModel} this item
	 * belongs to.
	 * @return value of the integration object code
	 */
	String getIntegrationObjectCode();

	/**
	 * Retrieves integration key value for this item.
	 * @return calculated integration key value for the item content.
	 */
	String getIntegrationKey();

	/**
	 * Sets value of an attribute in this item.
	 * @param attribute name of the attribute whose value needs to be set.
	 * @param value a value for the attribute. In order to set a value for a localized attribute, {@link LocalizedValue} should be
	 * passed as the {@code value}.
	 * @return {@code true}, if the value was successfully set, or {@code false}, if the specified attribute does not exist for
	 * this item.
	 */
	boolean setAttribute(String attribute, Object value);

	/**
	 * Retrieves value of an attribute.
	 * @param name name of the attribute to read.
	 * @return value for the specified attribute in this item or {@code null}, if the attribute has not been set yet or does not
	 * exist.
	 */
	Object getAttribute(String name);

	/**
	 * Reads value of a localized attribute. This method should ensure the specified attribute is indeed localized. Otherwise, the
	 * implementations should not return a value or indicate the problem by throwing an exception.
	 * @param attribute name of the attribute to read
	 * @param lang language, for which the attribute should be read
	 * @return value of the specified attribute in the specified language or {@code null}, if either the attribute or the language
	 * does not have a value set.
	 */
	String getLocalizedAttribute(final String attribute, final Locale lang);

	/**
	 * Reads value of a localized attribute.
	 * @param attribute name of the attribute to read
	 * @param lang ISO code of the language, e.g. 'en' or 'es_CO', for which the attribute should be read. This is the value that
	 * can be read from {@link Locale} as {@code Locale.toLanguageTag()}
	 * @return value of the specified attribute in the specified language or {@code null}, if either the attribute or the language
	 * does not have a value set.
	 */
	String getLocalizedAttribute(final String attribute, final String lang);

	/**
	 * Retrieves an integration item referenced by specified attribute. It does the same as calling {@link #getAttribute(String)}
	 * but there is no need to cast result of that call to {@code IntegrationItem}.
	 * @param attribute name of the attribute, whose value is a nested {@code IntegrationItem}
	 * @return the referenced item or {@code null}, if this item does not contain the referenced item for the specified attribute
	 * name.
	 */
	IntegrationItem getReferencedItem(String attribute);

	/**
	 * Retrieves multiple integration items referenced by specified attribute. It does the same as calling {@link #getAttribute(String)}
	 * but there is no need to cast result of that call to {@code Collection<IntegrationItem>}.
	 * @param attribute name of the attribute, whose value is a collection of nested {@code IntegrationItem}s
	 * @return the referenced items. if this item does not contain referenced items for the specified attribute name, an empty
	 * collection is returned. If the attribute references a single instance of an {@code IntegrationItem} instead of a collection,
	 * then a collection with that single item will be returned.
	 */
	Collection<IntegrationItem> getReferencedItems(String attribute);
}
