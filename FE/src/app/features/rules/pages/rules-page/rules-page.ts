import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  signal,
  viewChild,
} from '@angular/core';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { SECTIONS } from './rules-content';

@Component({
  selector: 'app-rules-page',
  templateUrl: './rules-page.html',
  styleUrls: ['./rules-page.css'],
  imports: [BackButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RulesPage {
  readonly sidebarOpen = signal(true);
  readonly activeIndex = signal(0);
  readonly activeSubIndex = signal(0);

  readonly contentBody = viewChild<ElementRef<HTMLDivElement>>('contentBody');

  readonly sections = SECTIONS;

  readonly activeContent = computed(() => {
    return this.sections[this.activeIndex()].content;
  });

  constructor() {
    effect(() => {
      const html = this.activeContent();
      const el = this.contentBody();
      if (el) {
        el.nativeElement.innerHTML = html;
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  selectSection(index: number): void {
    this.activeIndex.set(index);
    this.activeSubIndex.set(0);
  }

  scrollToSub(id: string, index: number): void {
    this.activeSubIndex.set(index);
    const container = this.contentBody();
    if (!container) return;
    const el = container.nativeElement.querySelector(`[id="${id}"]`);
    if (!el) {
      console.warn('[scrollToSub] element not found for id:', id);
      return;
    }
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
