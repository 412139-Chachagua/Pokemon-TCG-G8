import { Directive, ElementRef, EventEmitter, Output, OnDestroy } from '@angular/core';

@Directive({ selector: '[clickOutside]', standalone: true })
export class ClickOutsideDirective implements OnDestroy {
  @Output() clickOutside = new EventEmitter<void>();
  private listener = (event: MouseEvent) => {
    if (!this.el.nativeElement.contains(event.target)) {
      this.clickOutside.emit();
    }
  };

  constructor(private el: ElementRef) {
    document.addEventListener('click', this.listener);
  }

  ngOnDestroy(): void {
    document.removeEventListener('click', this.listener);
  }
}
