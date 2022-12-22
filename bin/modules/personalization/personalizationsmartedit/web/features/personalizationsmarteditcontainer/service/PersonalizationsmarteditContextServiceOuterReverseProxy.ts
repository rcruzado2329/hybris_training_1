import {GatewayProxied, SeInjectable} from 'smarteditcommons';
import {PersonalizationsmarteditContextService} from "personalizationsmarteditcontainer/service/PersonalizationsmarteditContextServiceOuter";
import * as angular from 'angular';

@GatewayProxied('applySynchronization', 'isCurrentPageActiveWorkflowRunning')
@SeInjectable()
export class PersonalizationsmarteditContextServiceReverseProxy {

	public static readonly WORKFLOW_RUNNING_STATUS = "RUNNING";

	constructor(
		protected personalizationsmarteditContextService: PersonalizationsmarteditContextService,
		protected workflowService: any,
		protected pageService: any) {
	}

	applySynchronization(): void {
		this.personalizationsmarteditContextService.applySynchronization();
	}

	isCurrentPageActiveWorkflowRunning(): angular.IPromise<boolean> {
		return this.pageService.getCurrentPageInfo().then((pageInfo: any) => {
			return this.workflowService.getActiveWorkflowForPageUuid(pageInfo.uuid).then((workflow: any) => {
				if (workflow == null) {
					return false;
				}
				return workflow.status === PersonalizationsmarteditContextServiceReverseProxy.WORKFLOW_RUNNING_STATUS;
			});
		});
	}

}
