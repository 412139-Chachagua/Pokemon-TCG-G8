import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'conditionIcon', standalone: true })
export class ConditionIconPipe implements PipeTransform {
  transform(condition: string): string {
    return `assets/icons/conditions/condition-${condition.toLowerCase()}.svg`;
  }
}
