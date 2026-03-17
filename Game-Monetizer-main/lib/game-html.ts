/**
 * Returns the game HTML string with the daily seed injected.
 *
 * The source HTML lives in assets/game.html (edit THAT file for game changes).
 * Run `node scripts/sync-game-html.js` after editing to regenerate the content module.
 */
import { GAME_HTML_TEMPLATE } from "./game-html-content";

export function getGameHTML(options?: { dailySeed?: string }): string {
  const seedLine = options?.dailySeed
    ? `const DAILY_SEED = "${options.dailySeed}";`
    : `const DAILY_SEED = null;`;

  return GAME_HTML_TEMPLATE.replace("/* __SEED_INJECTION__ */", seedLine);
}
