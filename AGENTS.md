# Offline Trading Journal App - Agent Guidelines & Conventions

This project is a high-end, professional, offline-first trading journal application built with Jetpack Compose, Room Database, and MVVM architecture. It operates 100% offline.

## 📁 Core Code Architecture

1. **`com.example.data.model` (Entities)**
   - All persistence is controlled via Room.
   - Core entities: `Trade`, `DailyJournal`, `Market`, `Tag`, `ChecklistItem`.
   
2. **`com.example.data.analysis` (Offline Intelligent Analysis Engine)**
   - Powered by a highly specific, rule-based inference system inside `AnalysisEngine.kt`.
   - Analyzes win-rates, profit factor, drawdown, behavior-level risks (revenge trading, overtrading), and asset-level tendencies.
   - Calculates a **Trader Personality Profile** (e.g., Disciplined Planner, Emotional Revenger, Hyperactive Scalper, Under-prepared Explorer).
   
3. **`com.example.data.backup` (Robust Safe Backup Engine)**
   - `BackupHelper.kt` manages transaction-safe local backup import/export.
   - Imports/Exports full database state (including checklist configurations and daily psychological logs) using manual fast JSON serialization to keep size minimal.

## 🎯 Important Constraints

- **100% Offline**: Do NOT add any cloud, Firebase, or external API networking integrations. All features (analysis, insights, backup, etc.) must remain entirely on-device.
- **Transaction Safety**: Any database restore operation must be wrapped in a transaction (`db.runInTransaction`). Ensure existing tables are cleanly purged using their respective DAOs before reloading data.
- **Multi-language Support**: Ensure Farsi (fa), Arabic (ar), and English (en) localized texts are respected throughout all dashboard widgets, dialogs, and reports.
