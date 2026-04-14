/** Matches backend enum `Playstyle` names. */
export const PLAYSTYLE_VALUES = ['BALANCED', 'PRODUCTION_FOCUSED', 'SCARCITY_FOCUSED'] as const;

export type PlaystyleId = (typeof PLAYSTYLE_VALUES)[number];
