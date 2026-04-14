import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import type { PlayerHeuristicRow, ResourceOnMapRow } from '../../models/map.interface';
import { PLAYSTYLE_VALUES, type PlaystyleId } from '../../models/playstyle';

const PLAYSTYLE_STORAGE_KEY = 'catan.playstyles.perSeat';

function isPlaystyleId(value: string): value is PlaystyleId {
  return (PLAYSTYLE_VALUES as readonly string[]).includes(value);
}

export type NowPlayingSeat = '1st' | '2nd' | '3rd';

@Component({
  selector: 'app-map-sidebar',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './map-sidebar.component.html',
  styleUrl: './map-sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapSidebarComponent implements OnInit {
  constructor(private readonly cdr: ChangeDetectorRef) {}
  @Input({ required: true }) nowPlayingSeats!: readonly NowPlayingSeat[];
  @Input({ required: true }) nowPlayingSeat!: NowPlayingSeat;
  /** When true, active seat comes from API (`isPlaying`); seat buttons are disabled. */
  @Input() nowPlayingSeatLocked = false;
  @Input({ required: true }) nowPlayingCaption!: { seat: string; colorLabel: string; swatch: 'white' | 'blue' | 'orange' };
  @Input({ required: true }) heatmapOn!: boolean;
  @Input({ required: true }) playersBySeat!: readonly PlayerHeuristicRow[];
  @Input({ required: true }) playersByOverall!: readonly PlayerHeuristicRow[];
  @Input({ required: true }) resourceOnMap!: readonly ResourceOnMapRow[];
  @Input({ required: true }) generateMapLoading!: boolean;
  @Input({ required: true }) generateMapFailed!: boolean;
  @Input({ required: true }) mapGenerated!: boolean;
  /** While a session is active, generate is disabled. */
  @Input({ required: true }) gameStarted!: boolean;
  @Input({ required: true }) generateMapDisabled!: boolean;
  @Input({ required: true }) startGameDisabled!: boolean;

  @Output() readonly nowPlayingSeatChange = new EventEmitter<NowPlayingSeat>();
  @Output() readonly heatmapToggle = new EventEmitter<void>();
  @Output() readonly generateMap = new EventEmitter<readonly PlaystyleId[]>();
  @Output() readonly startGame = new EventEmitter<void>();
  @Output() readonly abortGame = new EventEmitter<void>();

  /** Configure playstyle per seat before calling the API. */
  generateDialogOpen = false;
  abortDialogOpen = false;
  readonly playstyleOptions = PLAYSTYLE_VALUES;
  readonly playstyleLabels: Record<PlaystyleId, string> = {
    BALANCED: 'Balanced',
    PRODUCTION_FOCUSED: 'Production-focused',
    SCARCITY_FOCUSED: 'Scarcity-focused',
  };
  seatPlaystyles: [PlaystyleId, PlaystyleId, PlaystyleId] = ['BALANCED', 'BALANCED', 'BALANCED'];

  ngOnInit(): void {
    this.applyStoredPlaystyles();
    this.cdr.markForCheck();
  }

  openGenerateDialog(): void {
    if (this.generateMapDisabled) {
      return;
    }
    this.applyStoredPlaystyles();
    this.generateDialogOpen = true;
    this.cdr.markForCheck();
  }

  openAbortDialog(): void {
    this.abortDialogOpen = true;
    this.cdr.markForCheck();
  }

  closeAbortDialog(): void {
    this.abortDialogOpen = false;
    this.cdr.markForCheck();
  }

  confirmAbort(): void {
    this.abortDialogOpen = false;
    this.abortGame.emit();
    this.cdr.markForCheck();
  }

  /** Restore last saved triple from {@link localStorage}, if valid. */
  private applyStoredPlaystyles(): void {
    const stored = this.readPlaystylesFromStorage();
    if (stored) {
      this.seatPlaystyles = stored;
    }
  }

  private readPlaystylesFromStorage(): [PlaystyleId, PlaystyleId, PlaystyleId] | null {
    if (typeof window === 'undefined') {
      return null;
    }
    try {
      const raw = window.localStorage.getItem(PLAYSTYLE_STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed) || parsed.length !== 3) {
        return null;
      }
      const out: PlaystyleId[] = [];
      for (let i = 0; i < 3; i++) {
        const s = String(parsed[i]);
        if (!isPlaystyleId(s)) {
          return null;
        }
        out.push(s);
      }
      return [out[0]!, out[1]!, out[2]!];
    } catch {
      return null;
    }
  }

  private persistPlaystyles(): void {
    if (typeof window === 'undefined') {
      return;
    }
    try {
      window.localStorage.setItem(PLAYSTYLE_STORAGE_KEY, JSON.stringify(this.seatPlaystyles));
    } catch {
      // quota / private mode
    }
  }

  closeGenerateDialog(): void {
    this.generateDialogOpen = false;
  }

  onSeatPlaystyleModelChange(index: 0 | 1 | 2, value: unknown): void {
    const s = String(value);
    if (!isPlaystyleId(s)) {
      return;
    }
    const next: [PlaystyleId, PlaystyleId, PlaystyleId] = [...this.seatPlaystyles];
    next[index] = s;
    this.seatPlaystyles = next;
    this.persistPlaystyles();
    this.cdr.markForCheck();
  }

  confirmGenerate(): void {
    this.generateDialogOpen = false;
    this.persistPlaystyles();
    this.generateMap.emit(this.seatPlaystyles);
  }
}

