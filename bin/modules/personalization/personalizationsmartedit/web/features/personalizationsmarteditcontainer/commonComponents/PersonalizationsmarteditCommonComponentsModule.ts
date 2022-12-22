import {SeModule} from 'smarteditcommons';
import {CatalogFilterDropdownComponent, PERSONALIZATION_CATALOG_FILTER_PROVIDER} from 'personalizationsmarteditcontainer/commonComponents/CatalogFilterDropdownComponent';
import {CatalogVersionFilterDropdownComponent} from 'personalizationsmarteditcontainer/commonComponents/CatalogVersionFilterDropdownComponent';
import {PageFilterDropdownComponent, PERSONALIZATION_CUSTOMIZATION_PAGE_FILTER_PROVIDER} from 'personalizationsmarteditcontainer/commonComponents/PageFilterDropdownComponent';
import {StatusFilterDropdownComponent} from 'personalizationsmarteditcontainer/commonComponents/StatusFilterDropdownComponent';
import {HasMulticatalogComponent} from 'personalizationsmarteditcontainer/commonComponents/HasMulticatalogComponent';
import {PersonalizationsmarteditCommonsModule} from 'personalizationcommons';
import {PersonalizationsmarteditServicesModule} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditServicesModule';
@SeModule({
	imports: [
		'ySelectModule',
		'componentMenuServiceModule',
		'l10nModule',
		PersonalizationsmarteditCommonsModule,
		PersonalizationsmarteditServicesModule
	],
	declarations: [
		CatalogFilterDropdownComponent,
		CatalogVersionFilterDropdownComponent,
		PageFilterDropdownComponent,
		StatusFilterDropdownComponent,
		HasMulticatalogComponent
	],
	providers: [
		PERSONALIZATION_CUSTOMIZATION_PAGE_FILTER_PROVIDER,
		PERSONALIZATION_CATALOG_FILTER_PROVIDER
	]
})
export class PersonalizationsmarteditCommonComponentsModule {}
