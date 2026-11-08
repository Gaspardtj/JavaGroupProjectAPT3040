# Library Management System

Recovered Java class project, now using PostgreSQL. Requires JDK 11 or newer and PostgreSQL (the setup script defaults to the installed PostgreSQL 18 on Windows).

## First-time setup

1. Create `postgres-admin.local.properties` in this folder containing `password=YOUR_POSTGRES_PASSWORD` (the password chosen during PostgreSQL installation).
2. Run `./setup-database.ps1` in PowerShell from this folder. It creates the `library_app` login and empty `library_management_system` database, applies `schema.sql`, and removes the administrator password from the setup file after success. It does not reset an existing role or database.
3. Run `./run.ps1` to compile and launch the console app.

The generated `database.local.properties` contains the app connection settings and is ignored by Git. Keep it private. Environment variables `LIBRARY_DB_URL`, `LIBRARY_DB_USER`, and `LIBRARY_DB_PASSWORD` override these settings. Run scripts from this folder; they handle the working directory automatically.

`./run.ps1 -BuildOnly` builds `build/library-management.jar`. The script downloads the pinned official PostgreSQL JDBC 42.7.14 driver on first use and verifies its SHA-256. No Maven installation is needed. Driver source: https://jdbc.postgresql.org/.

`./test.ps1` runs PostgreSQL integration checks for adding/listing/updating books, case-insensitive search, member defaults, invalid records, and interactive input. Test rows are rolled back; identity sequences can advance.

## Recovered features

- Add and list books and members
- Search books by title or author
- View book/member statistics

The original repository contains no database backup. It also has no borrow/return menu or loan tracking; the existing copy-count model helpers are preserved. `LibraryItem` and `Magazine` remain as the original standalone inheritance examples.

## Troubleshooting

- Connection refused: check that the PostgreSQL Windows service is running and the host/port match your settings.
- Authentication failed: check the local configuration; the app uses its own login, not the postgres administrator.
- Missing tables: run the setup script successfully before launching.
- PowerShell script policy restriction: launch with `powershell -ExecutionPolicy Bypass -File ./run.ps1` for that process only.

The old hardcoded MySQL password was removed from source. It remains in historical Git commits; change it anywhere it is still used. Old tracked IntelliJ `out` classes are not used by the build.
