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
package de.hybris.platform.integrationservices.search;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a where clause condition (e.g. code = 'abc'). It doesn't include the keyword 'WHERE' in the condition.
 * The WhereClauseCondition can also store the conjunctive operator (AND, OR, etc.) to the next WhereClauseCondition.
 */
public class WhereClauseCondition
{
	private final String attributeName;
	private final String condition;
	private final ConjunctiveOperator operator;

	/**
	 * Stores the where clause condition without the conjunctive operator (AND, OR, etc) to the next WhereClauseCondition
	 * @param condition Where clause condition (e.g. code = 'abc')
	 */
	public WhereClauseCondition(final String condition)
	{
		this.condition = condition;
		this.operator = ConjunctiveOperator.UNKNOWN;
		this.attributeName = extractAttributeNameFromCondition();
	}

	/**
	 * Stores the where clause condition  with the conjunctive operator (AND, OR, etc) to the next WhereClauseCondition
	 * @param condition Where clause condition
	 */
	public WhereClauseCondition(final String condition, final ConjunctiveOperator operator)
	{
		this.condition = condition;
		this.operator = operator != null? operator : ConjunctiveOperator.UNKNOWN;
		this.attributeName = extractAttributeNameFromCondition();
	}

	private WhereClauseCondition(final String name, final String condition, final ConjunctiveOperator operator)
	{
		this.condition = condition;
		this.operator = operator;
		this.attributeName = name;
	}

	public String getCondition()
	{
		return condition;
	}

	/**
	 * Extracts the attributeName from the condition.
	 * Example filter condition: "{supercategories} = 8796093055118" -> returns "supercategories"
	 *
	 * @return attributeName or "" if no attributeName is not found
	 */
	public String getAttributeName()
	{
		return attributeName;
	}

	private String extractAttributeNameFromCondition()
	{
		final int openCurlyIndex = condition.indexOf('{');
		final int closeCurlyIndex = condition.indexOf('}');
		if (openCurlyIndex > -1 && closeCurlyIndex > -1)
		{
			return condition.substring(openCurlyIndex + 1, closeCurlyIndex);
		}
		return "";
	}

	public ConjunctiveOperator getConjunctiveOperator()
	{
		return operator;
	}

	public String getConjunctiveOperatorString()
	{
		return operator.toString();
	}

	public WhereClauseConditions toWhereClauseConditions()
	{
		return new WhereClauseConditions(this);
	}

	/**
	 * Creates a copy of this condition, in which attribute name is replaced with the specified one.
	 *
	 * @param newName attribute name to be used in the new copy of this condition
	 * @return a new condition that has all fields except the attribute name set to values in this condition; name
	 * is set to the {@code newName}.
	 */
	WhereClauseCondition changeAttributeName(final String newName)
	{
		return new WhereClauseCondition(newName, this.condition, this.operator);
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}

		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		final WhereClauseCondition that = (WhereClauseCondition) o;

		return new EqualsBuilder()
				.append(condition, that.condition)
				.append(operator, that.operator)
				.isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37)
				.append(condition)
				.append(operator)
				.toHashCode();
	}

	@Override
	public String toString()
	{
		return "WhereClauseCondition{" +
				"condition='" + condition + '\'' +
				", conjunctiveOperator='" + operator + '\'' +
				'}';
	}
}
