/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.b2b.dao;

import de.hybris.platform.b2b.model.B2BCostCenterModel;
import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.List;
import java.util.Set;



public interface B2BCostCenterDao extends GenericDao<B2BCostCenterModel>
{

	/**
	 * Returns list of active {@link B2BCostCenterModel}s associated with any B2BUnit in the set passed and having the
	 * matching currency.
	 * 
	 * @param branch
	 * @param currency
	 * @return List {@link B2BCostCenterModel}
	 */
	List<B2BCostCenterModel> findActiveCostCentersByBranchAndCurrency(final Set<B2BUnitModel> branch, final CurrencyModel currency);

	/**
	 * Finds B2BCostCenter by code, If none is found null is returned.
	 * 
	 * @param code
	 *           , the code of the desired Cost Center
	 */
	B2BCostCenterModel findByCode(final String code);

	/**
	 * Returns list of distinct {@link CurrencyModel} for a {@link B2BCostCenterModel} associated with a set of B2BUnits.
	 * 
	 * <p>
	 * Note: Default method to ensure backwards compatibility with clients who use this interface but have a custom implementation.
	 * </p>
	 * @param branch
	 * @return List of {@link CurrencyModel}
	 */
	default List<CurrencyModel> findCurrenciesForAllCostCentersOfUnit(final Set<B2BUnitModel> branch)
	{
		final StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" SELECT DISTINCT {c:currency} ");
		queryBuilder.append(" FROM {B2BCostCenter as c}");
		queryBuilder.append(" WHERE {c:active} = 1 ");
		queryBuilder.append(" AND {c:unit} in ( ?branch ) ");
		queryBuilder.append(" ORDER BY {c:currency} ASC");

		final FlexibleSearchQuery query = new FlexibleSearchQuery(queryBuilder.toString());
		query.setCount(-1);
		query.setDisableSearchRestrictions(true);
		query.setStart(0);
		query.getQueryParameters().put("branch", branch);

		final FlexibleSearchService fs = Registry.getCoreApplicationContext().getBean("flexibleSearchService", FlexibleSearchService.class);
		final SearchResult<CurrencyModel> result = fs.search(query);
		return result.getResult();
	}
}
