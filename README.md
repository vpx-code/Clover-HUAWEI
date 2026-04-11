# Clover 🍀

> Award-winning pill reminder for senior citizens, designed for the Huawei ecosystem.

<p align="center">
  <img src="https://victorperez.tech/screenshots/clover/clover-weather.png" alt="Clover weather-adaptive UI" width="100%"/>
</p>

---

## Screenshots

<p align="center">
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-1.webp" alt="Screenshot 1" width="200"/>
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-2.webp" alt="Screenshot 2" width="200"/>
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-3.webp" alt="Screenshot 3" width="200"/>
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-4.webp" alt="Screenshot 4" width="200"/>
</p>

<p align="center">
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-5.webp" alt="Screenshot 5" width="200"/>
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-6.webp" alt="Screenshot 6" width="200"/>
  <img src="https://victorperez.tech/screenshots/clover/clover-screenshot-7.webp" alt="Screenshot 7" width="200"/>
</p>

---

## About

Clover is a medication reminder app for senior citizens, built natively for Android and the Huawei ecosystem. What started as a two-week hackathon prototype grew into a multi-year project that won three international awards and was published on both AppGallery and Google Play.

The project went through two major iterations: an initial Java prototype, and a full rewrite in Kotlin with a redesigned architecture and a companion Huawei Watch app.

---

## Awards

- 🥇 **Huawei Student Developers Spain Hackathon** — 1st Place (2020)
- 🥇 **Huawei AppsUP Europe** — Winner (2021)
- 🏅 **Huawei AppsUP Europe** — Honorable Mention (2022)

---

## Architecture

The app was fully rewritten from Java to Kotlin after the initial hackathon win. Key architectural decisions:

- **MVVM** — Clean separation of concerns between UI, business logic, and data layers
- **Dagger Hilt** — Dependency injection for testability and modularity
- **AlarmManager + Notification SDK** — Custom notification queue for medication reminders. Reengineered from scratch in the Kotlin rewrite — by far the most complex feature in the app, handling scheduling, cancellation, and persistence across reboots
- **Animatable** — Custom UI animations for the app character, built without Material Design components. The entire interface was designed from scratch

---

## Huawei HMS Integration

The app is built on Huawei Mobile Services (HMS) and integrates the following kits:

| Kit | Usage |
|-----|-------|
| **Awareness Kit** | Detects weather changes and dynamically adapts the entire app UI |
| **Map Kit + Site Kit** | Integrates with Petal Maps to find nearby pharmacies, with click-to-call |
| **ML Kit** | Text recognition on medication packaging to auto-fill treatment data |
| **Watch SDK** | Powers the companion Huawei Watch app (separate Java application) |

---

## Clover for Watch

In 2021, I built **Clover for Watch** — a companion Huawei Watch app that lets users dismiss medication reminders directly from their wrist.

The Watch app is a separate Java application that communicates with the main Kotlin app via the Huawei Watch SDK, handling its own notification layer and syncing treatment state bidirectionally.

This companion app earned an Honorable Mention at AppsUP Europe 2022.

---

## Availability

- **AppGallery** — Primary distribution platform throughout the app's lifetime
- **Google Play** — Briefly available during the app's peak period

---

## Reflection

> *"I'm satisfied: Apple only figured out the potential of this kind of app a year later, when iOS 16 was released."*

Clover eventually became too large for a single developer to maintain part-time, and the project was sunset. But it remains the project I'm most proud of — a self-taught developer building a production-quality, award-winning app from scratch, without AI tooling, before any of this was easy.

---

## Author

**Víctor Pérez Jiménez** — Solutions Engineer & Indie Hacker  
[victorperez.tech](https://victorperez.tech) · [@vpx_tech](https://x.com/vpx_tech) · [LinkedIn](https://www.linkedin.com/in/victorperezjimenez/)
