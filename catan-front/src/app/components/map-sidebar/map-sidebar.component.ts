import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import type { PlayerHeuristicRow, ResourceOnMapRow } from '../../models/map.interface';

export type NowPlayingSeat = '1st' | '2nd' | '3rd';

@Component({
  selector: 'app-map-sidebar',
  standalone: true,
  templateUrl: './map-sidebar.component.html',
  styleUrl: './map-sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapSidebarComponent {
  @Input({ required: true }) nowPlayingSeats!: readonly NowPlayingSeat[];
  @Input({ required: true }) nowPlayingSeat!: NowPlayingSeat;
  @Input({ required: true }) nowPlayingCaption!: { seat: string; colorLabel: string; swatch: 'white' | 'blue' | 'orange' };
  @Input({ required: true }) heatmapOn!: boolean;
  @Input({ required: true }) playersByOverall!: readonly PlayerHeuristicRow[];
  @Input({ required: true }) resourceOnMap!: readonly ResourceOnMapRow[];
  @Input({ required: true }) generateMapLoading!: boolean;
  @Input({ required: true }) generateMapFailed!: boolean;

  @Output() readonly nowPlayingSeatChange = new EventEmitter<NowPlayingSeat>();
  @Output() readonly heatmapToggle = new EventEmitter<void>();
  @Output() readonly generateMap = new EventEmitter<void>();
}

