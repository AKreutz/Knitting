# Knitting

An Android app for planning and tracking knitting projects.

## Features

- Create projects with a name and description, and move them through **Created → In progress → Finished**.
- Break a project into steps: cast-on, increases, decreases, plain rows and patterns.
- Every step can carry an optional yarn color; cast-on steps also record method and needle size.
- Paint pattern grids (cables or colorwork) while planning a project.
- Track progress while a project is in progress: stitch, increase, decrease, row and pattern-repeat counters, plus a row counter within the current pattern repeat.
- Edit a project and its steps while it is still in the Created state.

## Tech stack

- Kotlin, Jetpack Compose and Material 3
- Room for local storage
- Min SDK 24, target SDK 37

## Building

Open the project in Android Studio, or from the command line:

```
./gradlew assembleDebug
./gradlew lint
```

Release signing reads `keystore.properties`, which is not meant to be shared.

## Data

The database is not migrated between schema versions: a schema change wipes existing projects and steps (`fallbackToDestructiveMigration` in `KnittingDatabase`).
