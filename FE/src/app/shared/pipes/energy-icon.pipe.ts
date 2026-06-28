import { inject, Pipe, PipeTransform } from '@angular/core';
import { CardRepositoryService } from '../../core/services/card-repository.service';

const ENERGY_TYPES = new Set([
  'GRASS', 'FIRE', 'WATER', 'LIGHTNING', 'PSYCHIC',
  'FIGHTING', 'DARKNESS', 'METAL', 'FAIRY', 'COLORLESS',
]);

@Pipe({ name: 'energyIcon', standalone: true })
export class EnergyIconPipe implements PipeTransform {
  private readonly cardRepo = inject(CardRepositoryService);

  transform(type: string): string {
    const upper = type?.toUpperCase() ?? '';
    if (ENERGY_TYPES.has(upper)) {
      return `assets/icons/energy/energy-${upper.toLowerCase()}.svg`;
    }
    const def = this.cardRepo.getFromCache(type);
    if (def?.types?.length) {
      return `assets/icons/energy/energy-${def.types[0].toLowerCase()}.svg`;
    }
    return 'assets/icons/energy/energy-colorless.svg';
  }
}
