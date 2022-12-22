/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.configurablebundleservices.daos.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.configurablebundleservices.daos.BundleTemplateDao;
import de.hybris.platform.configurablebundleservices.enums.BundleTemplateStatusEnum;
import de.hybris.platform.configurablebundleservices.model.BundleTemplateModel;
import de.hybris.platform.configurablebundleservices.model.BundleTemplateStatusModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Default implementation of the {@link BundleTemplateDao}.
 */
public class DefaultBundleTemplateDao extends AbstractItemDao implements BundleTemplateDao
{
	private static final String RESTRICTION_ONLY_APPROVED = " AND {" + BundleTemplateModel.STATUS + "}= ?status";

    private static final String ORDER_BY_ID = " ORDER BY {" + BundleTemplateModel.ID + "} ASC";

    private static final String FIND_BUNDLETEMPLATE = "GET {" + BundleTemplateModel._TYPECODE + "} WHERE {";

    private static final String FIND_BUNDLETEMPLATE_BY_ID = FIND_BUNDLETEMPLATE  + BundleTemplateModel.ID + "}= ?uid";

	private static final String FIND_STATUS = "GET {" + BundleTemplateStatusModel._TYPECODE +
			"} WHERE {" + BundleTemplateStatusModel.STATUS + "} = ?status AND {" +
			BundleTemplateStatusModel.CATALOGVERSION + "} = ?catalogVersion ";

	private static final String FIND_BUNDLETEMPLATE_QUERY_BY_VERSION = FIND_BUNDLETEMPLATE_BY_ID + " AND  {" + BundleTemplateModel.VERSION
			+ "}=?version";

	private static final String FIND_BUNDLETEMPLATES_BY_PRODUCT_QUERY = FIND_BUNDLETEMPLATE
			+ BundleTemplateModel.PARENTTEMPLATE + "} IS NOT NULL" + RESTRICTION_ONLY_APPROVED + ORDER_BY_ID;

	private static final String FIND_ALL_ROOT_BUNDLETEMPLATES = FIND_BUNDLETEMPLATE
			+ BundleTemplateModel.PARENTTEMPLATE + "} IS NULL AND {"
			+ BundleTemplateModel.CATALOGVERSION + "} = ?catalogVersion ";

	private static final String FIND_ALL_ROOT_BUNDLETEMPLATES_QUERY = FIND_ALL_ROOT_BUNDLETEMPLATES + ORDER_BY_ID;

	private static final String FIND_ALL_APPROVED_ROOT_BUNDLETEMPLATES = FIND_ALL_ROOT_BUNDLETEMPLATES +
            RESTRICTION_ONLY_APPROVED + ORDER_BY_ID;


	@Override
	@Nonnull
	public BundleTemplateModel findBundleTemplateById(final String bundleId)
	{
		validateParameterNotNullStandardMessage("bundleId", bundleId);

		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BUNDLETEMPLATE_BY_ID);
		query.addQueryParameter("uid", bundleId);

		final SearchResult<BundleTemplateModel> result = search(query);

		return result.getResult().stream()
				.filter(bt -> (BundleTemplateStatusEnum.APPROVED.equals(bt.getStatus().getStatus()) ||
						BundleTemplateStatusEnum.ARCHIVED.equals(bt.getStatus().getStatus())))
				.findFirst()
				.orElseThrow(() -> new ModelNotFoundException("No result for the given query"));
	}


	@Override
	@Nonnull
	public BundleTemplateModel findBundleTemplateByIdAndVersion(final String bundleId, final String version)
	{
		validateParameterNotNullStandardMessage("bundleId", bundleId);
		validateParameterNotNullStandardMessage("version", version);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BUNDLETEMPLATE_QUERY_BY_VERSION);
		query.addQueryParameter("uid", bundleId);
		query.addQueryParameter("version", version);
		return searchUnique(query);

	}


	@Override
	@Nonnull
	public List<BundleTemplateModel> findBundleTemplatesByProduct(final ProductModel productModel)
	{
		validateParameterNotNullStandardMessage("productModel", productModel);

		BundleTemplateStatusModel statusModel = findStatusModel(BundleTemplateStatusEnum.APPROVED, productModel.getCatalogVersion());
		if (statusModel == null) {
			return Collections.emptyList();
		}

		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BUNDLETEMPLATES_BY_PRODUCT_QUERY);
		query.addQueryParameter(BundleTemplateModel.STATUS, statusModel);
		final SearchResult<BundleTemplateModel> result = search(query);

		return result.getResult().stream()
				.filter(bt -> bt.getProducts().contains(productModel))
				.collect(Collectors.toList());
	}

	@Override
	@Nonnull
	public List<BundleTemplateModel> findTemplatesByMasterOrderAndBundleNo(final AbstractOrderModel masterAbstractOrder,
			final int bundleNo)
	{
		return Collections.emptyList();
	}

	@Override
	@Nonnull
	public List<BundleTemplateModel> findAllRootBundleTemplates(final CatalogVersionModel catalogVersion)
	{
		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ALL_ROOT_BUNDLETEMPLATES_QUERY);
		query.addQueryParameter("catalogVersion", catalogVersion);
		final SearchResult<BundleTemplateModel> result = search(query);

		return result.getResult();
	}

	@Override
	@Nonnull
	public List<BundleTemplateModel> findAllApprovedRootBundleTemplates(final CatalogVersionModel catalogVersion)
	{
		BundleTemplateStatusModel statusModel = findStatusModel(BundleTemplateStatusEnum.APPROVED, catalogVersion);
		if (statusModel == null) {
			return Collections.emptyList();
		}

		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ALL_APPROVED_ROOT_BUNDLETEMPLATES);
		query.addQueryParameter("catalogVersion", catalogVersion);
		query.addQueryParameter(BundleTemplateModel.STATUS, statusModel);
		final SearchResult<BundleTemplateModel> result = search(query);

		return result.getResult();
	}

	@Override
	@Nonnull
	public List<AbstractOrderEntryModel> findAbstractOrderEntriesByBundleTemplate(final BundleTemplateModel bundleTemplate)
	{
		return Collections.emptyList();
	}

	private BundleTemplateStatusModel findStatusModel(final BundleTemplateStatusEnum status, CatalogVersionModel catalogVersion)
	{
		final FlexibleSearchQuery statusQuery = new FlexibleSearchQuery(FIND_STATUS);
		statusQuery.addQueryParameter(BundleTemplateModel.STATUS, status);
		statusQuery.addQueryParameter(BundleTemplateModel.CATALOGVERSION, catalogVersion);

		try {
			return searchUnique(statusQuery);
		}
		catch (ModelNotFoundException | AmbiguousIdentifierException e) {
			return null;
		}
	}

}
