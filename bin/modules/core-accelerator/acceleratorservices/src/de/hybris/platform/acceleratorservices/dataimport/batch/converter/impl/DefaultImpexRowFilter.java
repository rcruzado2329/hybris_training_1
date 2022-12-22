/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.acceleratorservices.dataimport.batch.converter.impl;

import de.hybris.platform.acceleratorservices.dataimport.batch.converter.ImpexRowFilter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;


/**
 * Default implementation of {@link ImpexRowFilter}. This implementation uses Groovy to evaluate the given row supplied
 * as parameter "row". The expression is treated as a boolean. For example, the evaluation if a column is blank can be
 * written as "row[<columnIndex>]".
 */
public class DefaultImpexRowFilter implements ImpexRowFilter
{
	private static final String VARIABLE_NAME = "row";
	private static final String EXPRESSION_SUFFIX = ") ? true : false";
	private static final String EXPRESSION_PREFIX = "return (";

	private Script script;

	/**
	 * @see de.hybris.platform.acceleratorservices.dataimport.batch.converter.ImpexRowFilter#filter(java.util.Map)
	 */
	@Override
	public synchronized boolean filter(final Map<Integer, String> row)
	{
		// script has state -> method has to be synchronized
		script.getBinding().setVariable(VARIABLE_NAME, row);
		return ((Boolean) script.run()).booleanValue();
	}

	/**
	 * @param expression
	 *           the expression to set
	 */
	@Required
	public void setExpression(final String expression)
	{
		Assert.hasText(expression);
		final GroovyShell shell = new GroovyShell();
		script = shell.parse(EXPRESSION_PREFIX + expression + EXPRESSION_SUFFIX);
		script.setBinding(new Binding());
	}
}
