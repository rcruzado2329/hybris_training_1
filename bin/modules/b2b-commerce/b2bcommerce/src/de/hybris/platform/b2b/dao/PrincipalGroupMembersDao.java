/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2b.dao;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.security.PrincipalModel;
import de.hybris.platform.core.model.user.UserGroupModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.List;
import java.util.Set;



public interface PrincipalGroupMembersDao
{
	/**
	 * Finds all members of a Principal Group of a given type. FlexibleSearch filters non specified type members so as
	 * not to have to iterate and instantiate entire collection to filter undesired types
	 * 
	 * <p>
	 * Note: Default method to ensure backwards compatibility with clients who use this interface but have a custom implementation.
	 * </p>
	 */
	default <T extends PrincipalModel> List<T> findHierarchyMembersByType(final Set<UserGroupModel> parents, final Class<T> memberType)
	{
		final ModelService ms = Registry.getCoreApplicationContext().getBean("modelService", ModelService.class);
		final StringBuilder builder = new StringBuilder();
		builder.append(" SELECT {pg.source} ");
		builder.append(" FROM { ");
		builder.append("  PrincipalGroupRelation as pg  ");
		builder.append("  JOIN PrincipalGroup as p ");
		builder.append("    ON {pg.target} = {p.pk} ");
		builder.append("  JOIN ").append(ms.getModelType(memberType)).append(" as m ");
		builder.append("    ON {pg.source} = {m.pk} ");
		builder.append(" } ");
		builder.append(" WHERE  {pg.target} IN (?parents) ");

		final FlexibleSearchQuery query = new FlexibleSearchQuery(builder.toString());
		query.setCount(-1);
		query.setStart(0);
		query.getQueryParameters().put("parents", parents);

		final FlexibleSearchService fs = Registry.getCoreApplicationContext().getBean("flexibleSearchService", FlexibleSearchService.class);
		final SearchResult<T> result = fs.search(query);
		return result.getResult();
	}

	/**
	 * Finds all members of a Principal Group of a given type. FlexibleSearch filters non specified type members so as
	 * not to have to iterate and instantiate entire collection to filter undesired types
	 */
	<T extends PrincipalModel> List<T> findAllMembersByType(final UserGroupModel parent, final Class<T> memberType);

	/**
	 * Finds members of a Principal Group of a given type. FlexibleSearch filters non specified type members so as not to
	 * have to iterate and instantiate entire collection to filter undesired types *
	 */
	<T extends PrincipalModel> List<T> findMembersByType(final UserGroupModel parent, final Class<T> memberType, final int count,
			final int start);

}
