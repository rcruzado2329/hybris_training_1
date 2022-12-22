import {SeDirective} from "smarteditcommons";

@SeDirective({
	selector: '[personalization-prevent-parent-scroll]'
})
export class PersonalizationPreventParentScrollDirective {

	constructor(
		private $scope: angular.IScope,
		private $element: JQuery<HTMLElement>
	) {
	}

	$postLink(): void {

		const onWheel = (event: any) => {
			const originalEventCondition = event.originalEvent && (event.originalEvent.wheelDeltaY || event.originalEvent.wheelDelta);
			const IEEventCondition = -(event.deltaY || event.delta) || 0;
			this.$element.parent()[0].scrollTop -= (event.wheelDeltaY || originalEventCondition || event.wheelDelta || IEEventCondition);
			event.stopPropagation();
			event.preventDefault();
			event.returnValue = false;
		};

		this.$element.parent()[0].addEventListener('wheel', onWheel, false);

		this.$scope.$parent.$on('$destroy', () => {
			this.$element.parent()[0].removeEventListener('wheel', onWheel, false);
		});
	}
}
