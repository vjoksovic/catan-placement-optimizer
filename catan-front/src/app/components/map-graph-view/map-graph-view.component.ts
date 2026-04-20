import { ChangeDetectionStrategy, Component, HostListener, inject } from '@angular/core';
import { MapSidebarComponent } from '../map-sidebar/map-sidebar.component';
import { MapFacadeService } from '../../services/map/map-facade.service';

@Component({
  selector: 'app-map-graph-view',
  standalone: true,
  imports: [MapSidebarComponent],
  templateUrl: './map-graph-view.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapGraphViewComponent {
  readonly facade = inject(MapFacadeService);

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.facade.placementRevealInProgress()) {
      return;
    }
    event.preventDefault();
    event.returnValue = '';
  }
}
