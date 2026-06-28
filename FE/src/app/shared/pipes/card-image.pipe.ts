import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'cardImage', standalone: true })
export class CardImagePipe implements PipeTransform {
  transform(cardId: string, size: 'small' | 'large' = 'small'): string {
    const [set, number] = cardId.split('-');
    const suffix = size === 'large' ? '_hires' : '';
    return `https://images.pokemontcg.io/${set}/${number}${suffix}.png`;
  }
}
