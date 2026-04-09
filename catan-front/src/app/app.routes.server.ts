import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    /** Runtime SSR avoids prerender + Vite prebundle edge cases in ng serve (Angular 19). */
    renderMode: RenderMode.Server,
  },
];
