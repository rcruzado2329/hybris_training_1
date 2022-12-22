/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.apiregistryservices.dao;

import de.hybris.platform.apiregistryservices.model.DestinationTargetModel;


/**
 * DAO for the {@link DestinationTargetModel}
 */
public interface DestinationTargetDao
{

	/**
	 * Find the destination target for a specific credential id
	 * @param credentialId
	 * 		id of the credential
	 * @return a destination target
	 */
	DestinationTargetModel findDestinationTargetByCredentialId (String credentialId);

	/**
	 * Find the destination target for a specific id
	 * @param id
	 * 		id of the destination target
	 * @return a destination target
	 */
	DestinationTargetModel findDestinationTargetById (String id);

}
