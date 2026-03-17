# DODGE.IO - Mobile Game App

## Overview
A neon-themed dodge game wrapped as a mobile app with Expo/React Native, ready for App Store and Google Play publishing. The game runs in a WebView (native) / iframe (web) with the full canvas-based game engine, while React Native provides the native shell, navigation, stats persistence, haptic feedback, and ad integration hooks.

## Architecture
- **Frontend**: Expo Router (file-based routing), React Native
- **Backend**: Express.js (port 5000) - serves landing page and API
- **Database**: PostgreSQL (Drizzle ORM) - daily leaderboard scores
- **Game Engine**: HTML5 Canvas rendered in WebView/iframe, communicates with RN via postMessage
- **State**: AsyncStorage for persistent local stats, PostgreSQL for global leaderboard
- **Fonts**: Space Grotesk (Google Fonts)

## File Structure
```
app/
  _layout.tsx        - Root layout with providers (GameProvider, QueryClient)
  index.tsx          - Main menu screen
  game.tsx           - Regular game screen
  daily.tsx          - Daily challenge screen (seeded RNG, submits to leaderboard)
  stats.tsx          - Personal stats screen
  leaderboard.tsx    - Global daily leaderboard screen
components/
  GameWebView.tsx    - Cross-platform game renderer (iframe on web, WebView on native)
  ErrorBoundary.tsx  - Error boundary wrapper
  ErrorFallback.tsx  - Error fallback UI
contexts/
  GameContext.tsx     - Game stats persistence context (AsyncStorage)
lib/
  game-html.ts       - Enhanced game HTML generator with all enemy types & sounds
  query-client.ts    - React Query client
  device-id.ts       - Anonymous device ID and display name management
constants/
  colors.ts          - Neon theme color constants
  ads.ts             - AdMob ad unit IDs and interstitial interval config
shared/
  schema.ts          - Drizzle ORM schema (users, daily_scores tables)
server/
  index.ts           - Express server
  routes.ts          - API routes (daily score submission & leaderboard)
  storage.ts         - Database storage layer (Drizzle ORM)
```

## Game Features
- **8 enemy types**: Bullet, Seeker, Wave, Bomber, Laser, Spiral, Splitter, Teleporter
- **3 power-ups**: Shield, Slow, Shrink
- **Sound effects**: Web Audio API synthesized sounds for all game events
- **Near-miss system**: Bonus points and streak multiplier for close dodges
- **Daily Challenge**: Seed-based deterministic enemy patterns, streak tracking
- **Global Leaderboard**: Anonymous daily high scores (one best score per device per day)
- **Rewarded ads**: "Watch ad for free shield" flow (mock for dev, ready for AdMob integration)
- **Stats tracking**: Games played, high score, near misses, shields used, daily streak

## API Endpoints
- `POST /api/daily-scores` - Submit a daily challenge score (upserts best per device/day)
- `GET /api/daily-scores/:date` - Get leaderboard for a specific date (top 50)
- `GET /api/daily-scores/:date/device/:deviceId` - Get device's best score for a date

## Key Dependencies
- expo, expo-router, expo-av, expo-haptics
- react-native-webview (native only)
- @react-native-async-storage/async-storage
- @expo-google-fonts/space-grotesk
- @tanstack/react-query
- drizzle-orm, pg (PostgreSQL)

## Workflows
- `Start Backend` - Express server on port 5000
- `Start Frontend` - Expo dev server on port 8081

## Ad Integration
- `react-native-google-mobile-ads` v15.6.0 installed with AdMob plugin in app.json
- **Rewarded ads**: Platform-specific `lib/use-rewarded-ad.native.ts` (real AdMob) / `lib/use-rewarded-ad.ts` (web mock)
  - "Watch ad for free shield" on death screen; grants shield after full ad view
- **Interstitial ads**: Platform-specific `lib/use-interstitial-ad.native.ts` (real AdMob) / `lib/use-interstitial-ad.ts` (web mock)
  - Shows every 3 retries (configurable via INTERSTITIAL_INTERVAL in constants/ads.ts)
  - Appears between death screen and next game start, doesn't interrupt gameplay
- On web/Expo Go: falls back to mock delays (ads always available)
- AdMob App ID: ca-app-pub-8808962900000209~6590867888 (Android, in app.json)
- Rewarded Ad Unit ID: ca-app-pub-8808962900000209/3377692824
- Interstitial Ad Unit ID: needs real ID from AdMob console (placeholder in constants/ads.ts)

## EAS Build
- EAS project linked: @ayneldjamama/dodge-io (ID: 35e40624-540c-4296-9145-8994155a5bd4)
- Android package: app.playvibegames.dodgeio
- Build profiles configured in eas.json (development, preview, production)
