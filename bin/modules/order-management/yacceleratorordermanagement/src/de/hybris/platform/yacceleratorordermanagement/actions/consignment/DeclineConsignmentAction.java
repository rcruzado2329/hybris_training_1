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
 *
 */
package de.hybris.platform.yacceleratorordermanagement.actions.consignment;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.ordercancel.OrderCancelException;
import de.hybris.platform.ordercancel.OrderCancelService;
import de.hybris.platform.ordermodify.model.OrderEntryModificationRecordEntryModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.ordersplitting.model.ConsignmentEntryModel;
import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import de.hybris.platform.ordersplitting.model.ConsignmentProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import de.hybris.platform.task.RetryLaterException;
import de.hybris.platform.warehousing.allocation.AllocationService;
import de.hybris.platform.warehousing.allocation.decline.action.DeclineActionStrategy;
import de.hybris.platform.warehousing.constants.WarehousingConstants;
import de.hybris.platform.warehousing.data.allocation.DeclineEntries;
import de.hybris.platform.warehousing.data.allocation.DeclineEntry;
import de.hybris.platform.warehousing.enums.DeclineReason;
import de.hybris.platform.warehousing.process.BusinessProcessException;
import de.hybris.platform.warehousing.process.WarehousingBusinessProcessService;
import de.hybris.platform.warehousing.taskassignment.services.WarehousingConsignmentWorkflowService;
import de.hybris.platform.yacceleratorordermanagement.constants.YAcceleratorOrderManagementConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateIfAnyResult;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertTrue;


/**
 * Declines the consignment.
 *
 * @deprecated since 19.05.16. See {@link ReAllocateConsignmentAction}.
 */
@Deprecated
public class DeclineConsignmentAction extends AbstractAction<ConsignmentProcessModel>
{
	protected static final String DECLINE_ENTRIES = "declineEntries";
	protected static final String RE_SOURCE_CHOICE = "reSource";
	protected static final String IS_CONSIGNMENT_AUTO_DECLINED = "isConsignmentAutoDecline";
	private static final long RETRY_INTERVAL = 3000L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeclineConsignmentAction.class);
	private AllocationService allocationService;
	private WarehousingBusinessProcessService<AbstractOrderModel> orderBusinessProcessService;
	private Map<DeclineReason, DeclineActionStrategy> declineActionsMap;
	private Map<DeclineReason, DeclineActionStrategy> externalWarehouseDeclineActionsMap;
	private WarehousingConsignmentWorkflowService warehousingConsignmentWorkflowService;
	private OrderCancelService orderCancelService;


	@Override
	public String execute(final ConsignmentProcessModel consignmentProcessModel)
	{
		String transition = Transition.OK.toString();

		validateParameterNotNullStandardMessage("Consignment", consignmentProcessModel.getConsignment());
		validateParameterNotNullStandardMessage("Context parameters", consignmentProcessModel.getContextParameters());

		Collection<BusinessProcessParameterModel> contextParams = new ArrayList<>();
		contextParams.addAll(consignmentProcessModel.getContextParameters());

		final Optional<BusinessProcessParameterModel> declineEntriesParam = contextParams.stream()
				.filter(param -> param.getName().equals(DECLINE_ENTRIES)).findFirst();

		assertTrue("Nothing to Decline", declineEntriesParam.isPresent());

		final DeclineEntries declinedEntries = (DeclineEntries) declineEntriesParam.get().getValue();
		validateParameterNotNullStandardMessage("declinedEntries", declinedEntries);
		validateIfAnyResult(declinedEntries.getEntries(), "Nothing to Decline");

		final AbstractOrderModel order = declinedEntries.getEntries().stream().findFirst().get().getConsignmentEntry()
				.getConsignment().getOrder();
		Boolean isAutoDecline = Boolean.FALSE;

		//Extracting manual decline entries and performing manual reallocation
		Collection<DeclineEntry> manualEntries = declinedEntries.getEntries().stream()
				.filter(declineEntry -> declineEntry.getReallocationWarehouse() != null).collect(toList());
		if (!manualEntries.isEmpty())
		{
			LOGGER.debug("Performing Manual Reallocation for {} decline entries", manualEntries.size());
			DeclineEntries manualDeclineEntries = new DeclineEntries();
			manualDeclineEntries.setEntries(manualEntries);
			performManualDecline(manualDeclineEntries);
		}

		//Extracting auto decline entries and performing auto reallocation
		Collection<DeclineEntry> autoEntries = declinedEntries.getEntries().stream()
				.filter(declineEntry -> declineEntry.getReallocationWarehouse() == null).collect(toList());
		if (!autoEntries.isEmpty())
		{
			LOGGER.debug("Performing Auto Reallocation for {} decline entries", autoEntries.size());
			DeclineEntries autoDeclineEntries = new DeclineEntries();
			autoDeclineEntries.setEntries(autoEntries);
			if (!performAutoDecline(order, autoDeclineEntries, consignmentProcessModel.getConsignment()))
			{
				transition = Transition.WAIT.toString();
			}
			isAutoDecline = Boolean.TRUE;
		}
		//Calling corresponding declineActionStrategy to be executed, based on selected reasons for decline
		executeDeclineActions(declinedEntries);

		//Updating the consignment process context
		updateConsignmentContextParameters(consignmentProcessModel, contextParams, declineEntriesParam.get(), isAutoDecline);

		return transition;
	}

	/**
	 * Removes the {#link DeclineEntries} from the consignment process context and add flag to indicate if autoDecline was done for the consignment
	 *
	 * @param consignmentProcessModel
	 * 		- process for the consignment to be declined
	 * @param contextParams
	 * 		- context parameters for the consignment to be declined
	 * @param declineEntriesParam
	 * 		- declined entries param in the context parameters for the consignment to be declined
	 * @param isAutoDecline
	 * 		- flag to indicate if auto decline was performed on the consignment
	 */
	protected void updateConsignmentContextParameters(ConsignmentProcessModel consignmentProcessModel,
			Collection<BusinessProcessParameterModel> contextParams, BusinessProcessParameterModel declineEntriesParam,
			Boolean isAutoDecline)
	{
		LOGGER.debug("Cleaning up the declinedEntries param from context parameters of the consignmentProcess");
		contextParams.remove(declineEntriesParam);
		getModelService().remove(declineEntriesParam);
		final Optional<BusinessProcessParameterModel> existingAutoDeclineParam = contextParams.stream().filter(
				businessProcessParameter -> !Strings.isNullOrEmpty(businessProcessParameter.getName()) && IS_CONSIGNMENT_AUTO_DECLINED
						.equals(businessProcessParameter.getName())).findAny();
		final BusinessProcessParameterModel autoDeclineParam = existingAutoDeclineParam.isPresent() ?
				existingAutoDeclineParam.get() :
				new BusinessProcessParameterModel();
		autoDeclineParam.setValue(isAutoDecline);
		autoDeclineParam.setProcess(consignmentProcessModel);
		if (!existingAutoDeclineParam.isPresent())
		{
			autoDeclineParam.setName(IS_CONSIGNMENT_AUTO_DECLINED);
			contextParams.add(autoDeclineParam);
		}
		consignmentProcessModel.setContextParameters(contextParams);
		getModelService().save(consignmentProcessModel);
	}

	/**
	 * Performs the auto decline for the given {@link DeclineEntries} and trigger the sourcing for the associated {@link AbstractOrderModel}
	 *
	 * @param autoDeclineEntries
	 * 		- entries to be declined
	 * @param order
	 * 		- associated {@link AbstractOrderModel} with the {@link DeclineEntries},for which sourcing needs to be triggered
	 */
	protected boolean performAutoDecline(final AbstractOrderModel order, final DeclineEntries autoDeclineEntries,
			final ConsignmentModel consignmentModel)
	{
		LOGGER.debug("perform Auto Decline");
		boolean result = true;

		getAllocationService().autoReallocate(autoDeclineEntries);
		try
		{
			getOrderBusinessProcessService()
					.triggerChoiceEvent(order, YAcceleratorOrderManagementConstants.ORDER_ACTION_EVENT_NAME, RE_SOURCE_CHOICE);
		}
		catch (final BusinessProcessException e)
		{
			if (OrderStatus.CANCELLING.equals(order.getStatus()) || OrderStatus.CANCELLED.equals(order.getStatus()))
			{
				if (!isConsignmentBeingCancelled(consignmentModel))
				{
					LOGGER.info("Order {} is cancelling. Consignment {} will retry to send an event to it", order.getCode(),
							consignmentModel.getCode());
					final RetryLaterException retryLaterException = new RetryLaterException();
					retryLaterException.setRollBack(true);
					retryLaterException.setDelay(RETRY_INTERVAL);

					throw retryLaterException;
				}

				LOGGER.info("Consignment {} is cancelling. It's going to wait step for receiving the cancellation event",
						consignmentModel.getCode());
				result = false;
			}
		}

		return result;
	}

	/**
	 * Checks if the passed consignment is being cancelled. Returns true if it is, otherwise false
	 *
	 * @param consignmentModel
	 * @return true if consignment is being cancelled, otherwise false
	 */
	protected boolean isConsignmentBeingCancelled(final ConsignmentModel consignmentModel)
	{
		final Collection<Long> cancellingEntries;
		try
		{
			cancellingEntries = getOrderCancelService().getPendingCancelRecordEntry((OrderModel) consignmentModel.getOrder())
					.getOrderEntriesModificationEntries().stream().map(OrderEntryModificationRecordEntryModel::getOrderEntry)
					.map(e -> e.getPk().getLong()).collect(toSet());
		}
		catch (final OrderCancelException e)
		{
			e.printStackTrace();
			return false;
		}

		return consignmentModel.getConsignmentEntries().stream().map(ConsignmentEntryModel::getOrderEntry)
				.map(e -> e.getPk().getLong()).anyMatch(cancellingEntries::contains);
	}

	/**
	 * Performs the manual reallocation for the given {@link DeclineEntries},
	 * Also, it starts the {@link ConsignmentProcessModel} as well as {@link de.hybris.platform.workflow.model.WorkflowModel} for task assignment for the newly created consignments
	 *
	 * @param manualDeclineEntries
	 * 		- entries to be reallocated to the selected warehouse
	 */
	protected void performManualDecline(DeclineEntries manualDeclineEntries)
	{
		final Collection<ConsignmentModel> newConsignments = getAllocationService().manualReallocate(manualDeclineEntries);


		final OrderModel order = (OrderModel) newConsignments.iterator().next().getOrder();

		final String orderProcessCode = getOrderBusinessProcessService().getProcessCode(order);
		final OrderProcessModel orderProcess = getOrderBusinessProcessService().getProcess(orderProcessCode);
		newConsignments.forEach(consignment -> {
			final ConsignmentProcessModel subProcess = getOrderBusinessProcessService()
					.createProcess(consignment.getCode() + WarehousingConstants.CONSIGNMENT_PROCESS_CODE_SUFFIX,
							YAcceleratorOrderManagementConstants.CONSIGNMENT_SUBPROCESS_NAME);
			subProcess.setParentProcess(orderProcess);
			subProcess.setConsignment(consignment);
			getModelService().save(subProcess);

			LOGGER.info("Start Consignment sub-process: '" + subProcess.getCode() + "'");
			getOrderBusinessProcessService().startProcess(subProcess);
		});
	}

	/**
	 * Executes a {@link de.hybris.platform.warehousing.allocation.decline.action.DeclineActionStrategy},
	 * for each declined entry according to the specified reason.
	 *
	 * @param declinedEntries
	 * 		the {@link DeclineEntries} to be declined
	 */
	protected void executeDeclineActions(final DeclineEntries declinedEntries)
	{
		Assert.notNull(declinedEntries, "Decline Entries cannot be null");
		Assert.isTrue(CollectionUtils.isNotEmpty(declinedEntries.getEntries()), "No entries found to be declined");

		final Map<DeclineReason, List<DeclineEntry>> declineReasonEntriesMap = new HashMap<>(); //NOSONAR
		declinedEntries.getEntries().forEach(entry -> {
			if (declineReasonEntriesMap.containsKey(entry.getReason()))
			{
				declineReasonEntriesMap.get(entry.getReason()).add(entry);
			}
			else
			{
				declineReasonEntriesMap.put(entry.getReason(), new ArrayList<>(Arrays.asList(entry)));
			}
		});

		final boolean isExternalWarehouseDeclined = declinedEntries.getEntries().iterator().next().getConsignmentEntry()
				.getConsignment().getWarehouse().isExternal();

		declineReasonEntriesMap.forEach((declineReason, entries) -> {
			if (CollectionUtils.isNotEmpty(entries) && isExternalWarehouseDeclined
					&& getExternalWarehouseDeclineActionsMap().get(declineReason) != null)
			{
				getExternalWarehouseDeclineActionsMap().get(declineReason).execute(entries);
			}
			else if (CollectionUtils.isNotEmpty(entries) && !isExternalWarehouseDeclined
					&& getDeclineActionsMap().get(declineReason) != null)
			{
				getDeclineActionsMap().get(declineReason).execute(entries);
			}
		});
	}

	protected AllocationService getAllocationService()
	{
		return allocationService;
	}

	@Required
	public void setAllocationService(AllocationService allocationService)
	{
		this.allocationService = allocationService;
	}

	protected WarehousingBusinessProcessService<AbstractOrderModel> getOrderBusinessProcessService()
	{
		return orderBusinessProcessService;
	}

	@Required
	public void setOrderBusinessProcessService(WarehousingBusinessProcessService<AbstractOrderModel> orderBusinessProcessService)
	{
		this.orderBusinessProcessService = orderBusinessProcessService;
	}

	protected Map<DeclineReason, DeclineActionStrategy> getDeclineActionsMap()
	{
		return declineActionsMap;
	}

	@Required
	public void setDeclineActionsMap(final Map<DeclineReason, DeclineActionStrategy> declineActionsMap)
	{
		this.declineActionsMap = declineActionsMap;
	}

	protected Map<DeclineReason, DeclineActionStrategy> getExternalWarehouseDeclineActionsMap()
	{
		return externalWarehouseDeclineActionsMap;
	}

	@Required
	public void setExternalWarehouseDeclineActionsMap(
			final Map<DeclineReason, DeclineActionStrategy> externalWarehouseDeclineActionsMap)
	{
		this.externalWarehouseDeclineActionsMap = externalWarehouseDeclineActionsMap;
	}

	protected WarehousingConsignmentWorkflowService getWarehousingConsignmentWorkflowService()
	{
		return warehousingConsignmentWorkflowService;
	}

	@Required
	public void setWarehousingConsignmentWorkflowService(
			final WarehousingConsignmentWorkflowService warehousingConsignmentWorkflowService)
	{
		this.warehousingConsignmentWorkflowService = warehousingConsignmentWorkflowService;
	}

	@Override
	public Set<String> getTransitions()
	{
		return Transition.getStringValues();
	}

	protected OrderCancelService getOrderCancelService()
	{
		return orderCancelService;
	}

	@Required
	public void setOrderCancelService(final OrderCancelService orderCancelService)
	{
		this.orderCancelService = orderCancelService;
	}

	protected enum Transition
	{
		OK, WAIT;

		public static Set<String> getStringValues()
		{
			final Set<String> res = new HashSet<>();

			for (final Transition transition : Transition.values())
			{
				res.add(transition.toString());
			}
			return res;
		}
	}
}
