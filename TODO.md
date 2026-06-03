# AniForge - Custom Widgets & Sequel Tracking Feature Roadmap

## 1. Custom Dashboard & Launcher Widgets Framework
- [ ] Design extensible Architecture for In-App Home Screen Widgets (Bento Grid components).
- [ ] Establish underlying infrastructure for future Android Home Screen (Desktop) Widgets using Jetpack Glance / AppWidget APIs.
- [ ] Create Shared Widget Settings Data Store (Preferences/Proto DataStore mapping).

## 2. Widget settings.
- [ ] Implement UI with settings for widgets.

## 3. "Now Watching" Core Widget Component
- [ ] Add horizontal swipe gesture navigation (Carousel) to cycle through the active "Watching" anime list.
- [ ] Implement a **+1 Quick Episode Button**:
    - [ ] If the incremented episode reaches total episode count, instantly migrate the anime entry from "Watching" to the "Completed" list.

### 3.1 Premium Custom Progress Bar
- [ ] Build custom linear progress indicator bound from `0` to `total_episodes`.
- [ ] Implement **Windows-style File Copying Animation**:
    - [ ] Create a custom Jetpack Compose Draw Modifier for a continuous infinite shimmer effect.
    - [ ] Apply a semi-transparent white-to-transparent linear gradient stripe running infinitely left-to-right across the active progress bounds.

## 4. Anime Sequel & Continuation Tracker
- [ ] **Detail Screen Integration**:
    - [ ] Add a persistent toggle button labeled "Awaiting Sequel / Expect Continuation" to the `DetailScreen` layout.
- [ ] **Home Screen UI Integration**:
    - [ ] Build a dynamic alert banner / notification feed block on the App's Home Screen.
    - [ ] If an anime from the "Awaiting Sequel" table gets a newly confirmed database entry (Announced / Currently Airing), automatically inject it into the Home Screen spotlight array.