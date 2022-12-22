import {SeComponent} from 'smarteditcommons';
import {PersonalizationsmarteditContextService} from "personalizationsmarteditcontainer/service/PersonalizationsmarteditContextServiceOuter";

@SeComponent({
	templateUrl: 'hasMulticatalogTemplate.html'
})
export class HasMulticatalogComponent {

	public hasMulticatalog: boolean;
	constructor(
		protected personalizationsmarteditContextService: PersonalizationsmarteditContextService
	) {}

	$onInit(): void {
		this.hasMulticatalog = this.getSeExperienceData().siteDescriptor.contentCatalogs.length > 1;
	}

	getSeExperienceData() {
		return this.personalizationsmarteditContextService.getSeData().seExperienceData;
	}
}
