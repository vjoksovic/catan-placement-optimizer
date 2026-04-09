import { Routes } from '@angular/router';
import { MapGraphViewComponent } from './components/map-graph-view/map-graph-view.component';

export const routes: Routes = [
  { path: '', component: MapGraphViewComponent, pathMatch: 'full' },
];
