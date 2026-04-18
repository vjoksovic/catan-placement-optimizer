/** Matches backend enum `Tactic` names. */
export const TACTIC_VALUES = ['BALANCED', 'PRODUCTION_FOCUSED', 'SCARCITY_FOCUSED'] as const;

export type TacticId = (typeof TACTIC_VALUES)[number];
