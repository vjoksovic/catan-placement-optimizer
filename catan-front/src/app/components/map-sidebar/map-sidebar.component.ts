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
import { TACTIC_VALUES, type TacticId } from '../../models/tactic';

const TACTIC_STORAGE_KEY = 'catan.tactics.perSeat';

function isTacticId(value: string): value is TacticId {
  return (TACTIC_VALUES as readonly string[]).includes(value);
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
  @Input() winnerCaption: { seat: string; colorLabel: string; swatch: 'white' | 'blue' | 'orange' } | null = null;
  @Input({ required: true }) heatmapOn!: boolean;
  @Input({ required: true }) playersBySeat!: readonly PlayerHeuristicRow[];
  @Input({ required: true }) playersByOverall!: readonly PlayerHeuristicRow[];
  @Input({ required: true }) resourceOnMap!: readonly ResourceOnMapRow[];
  @Input({ required: true }) generateMapLoading!: boolean;
  @Input({ required: true }) generateMapFailed!: boolean;
  @Input({ required: true }) mapGenerated!: boolean;
  /** While a session is active, generate is disabled. */
  @Input({ required: true }) gameStarted!: boolean;
  @Input({ required: true }) gameOver!: boolean;
  @Input({ required: true }) hasSettlementsOnMap!: boolean;
  @Input({ required: true }) placementRevealInProgress!: boolean;
  @Input({ required: true }) generateMapDisabled!: boolean;
  @Input({ required: true }) startGameDisabled!: boolean;

  @Output() readonly nowPlayingSeatChange = new EventEmitter<NowPlayingSeat>();
  @Output() readonly heatmapToggle = new EventEmitter<void>();
  @Output() readonly generateMap = new EventEmitter<readonly TacticId[]>();
  @Output() readonly startGame = new EventEmitter<void>();
  @Output() readonly abortPlacing = new EventEmitter<void>();

  /** Configure tactic per seat before calling the API. */
  generateDialogOpen = false;
  readonly tacticOptions = TACTIC_VALUES;
  readonly tacticLabels: Record<TacticId, string> = {
    BALANCED: 'Balanced',
    PRODUCTION_FOCUSED: 'Production-focused',
    SCARCITY_FOCUSED: 'Scarcity-focused',
  };
  seatTactics: [TacticId, TacticId, TacticId] = ['BALANCED', 'BALANCED', 'BALANCED'];

  ngOnInit(): void {
    this.applyStoredTactics();
    this.cdr.markForCheck();
  }

  /** Opens tactics popup only — values are saved to localStorage (also on each change). */
  openTacticsDialog(): void {
    if (this.generateMapDisabled) {
      return;
    }
    this.applyStoredTactics();
    this.generateDialogOpen = true;
    this.cdr.markForCheck();
  }

  /** Reads latest tactics from local storage and requests map generation (no popup). */
  runGenerateMap(): void {
    if (this.generateMapDisabled) {
      return;
    }
    this.applyStoredTactics();
    this.generateMap.emit(this.seatTactics);
    this.cdr.markForCheck();
  }

  /** Restore last saved triple from {@link localStorage}, if valid. */
  private applyStoredTactics(): void {
    const stored = this.readTacticsFromStorage();
    if (stored) {
      this.seatTactics = stored;
    }
  }

  private readTacticsFromStorage(): [TacticId, TacticId, TacticId] | null {
    if (typeof window === 'undefined') {
      return null;
    }
    try {
      const raw = window.localStorage.getItem(TACTIC_STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed) || parsed.length !== 3) {
        return null;
      }
      const out: TacticId[] = [];
      for (let i = 0; i < 3; i++) {
        const s = String(parsed[i]);
        if (!isTacticId(s)) {
          return null;
        }
        out.push(s);
      }
      return [out[0]!, out[1]!, out[2]!];
    } catch {
      return null;
    }
  }

  private persistTactics(): void {
    if (typeof window === 'undefined') {
      return;
    }
    try {
      window.localStorage.setItem(TACTIC_STORAGE_KEY, JSON.stringify(this.seatTactics));
    } catch {
      // quota / private mode
    }
  }

  closeGenerateDialog(): void {
    this.generateDialogOpen = false;
  }

  onSeatTacticModelChange(index: 0 | 1 | 2, value: unknown): void {
    const s = String(value);
    if (!isTacticId(s)) {
      return;
    }
    const next: [TacticId, TacticId, TacticId] = [...this.seatTactics];
    next[index] = s;
    this.seatTactics = next;
    this.persistTactics();
    this.cdr.markForCheck();
  }

  /** Persist tactics and close popup (does not generate). */
  saveTacticsDialog(): void {
    this.persistTactics();
    this.generateDialogOpen = false;
    this.cdr.markForCheck();
  }

  /** Footer generate / regenerate — “Regenerate” when a board already exists. */
  primaryMapActionLabel(): string {
    if (this.generateMapLoading) {
      return this.mapGenerated ? 'Regenerating…' : 'Generating…';
    }
    return this.mapGenerated ? 'Regenerate map' : 'Generate map';
  }
}
