/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

import {NgModule} from '@angular/core';
import {SeEntryModule} from 'smarteditcommons';
import {BrowserModule} from '@angular/platform-browser';
import {UpgradeModule} from '@angular/upgrade/static';

@SeEntryModule('personalizationsmarteditcontainermodule')
@NgModule({
	imports: [
		BrowserModule,
		UpgradeModule
	]
})
export class PersonalizationsmarteditContainer {}
