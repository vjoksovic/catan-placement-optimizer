import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MapSidebarComponent } from '../map-sidebar/map-sidebar.component';
import { MapFacadeService } from '../../services/map/map-facade.service';

@Component({
  selector: 'app-map-graph-view',
  standalone: true,
  imports: [MapSidebarComponent],
  templateUrl: './map-graph-view.component.html',
  styleUrl: './map-graph-view.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapGraphViewComponent {
  readonly facade = inject(MapFacadeService);
}
