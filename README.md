## Simple Student Information System

A desktop Student Information System written in Java Swing that stores all data in plain CSV files — no database engine, no external dependencies.

It manages **Students**, **Programs**, and **Colleges** with validation, cascading updates, and crash-safe saves.

---

## Requirements

- **JDK 8 or newer** (developed against JDK 26). `javac` and `java` must be on your `PATH`.
- Windows for `run.bat`; any OS works using the manual commands below.

---

## Running the app

**Windows** — just run `run.bat`. It compiles the sources and launches the app.

**Any platform** — compile and run manually:

```
javac Main/StudentInformationSystem.java Main/Managers/*.java Main/Models/*.java Main/Panels/*.java
java -cp . Main.StudentInformationSystem
```

Run these from the project root — the app resolves its data files relative to the working directory, and refuses paths outside it.

---

## Data & storage

**This repository ships with no data.** The app starts with empty Students, Programs, and Colleges tables, and you populate it yourself.

Three CSV files are created under `Main/Data/` the first time you add a record:

| File | Columns |
|---|---|
| `Student.csv` | `id,first_name,last_name,program_code,year,gender` |
| `Program.csv` | `code,name,college` |
| `College.csv` | `code,name` |

Notes on the format:

- An unassigned program or college is stored as the literal token `NULL`.
- Values containing `,`, `"`, or newlines are quoted; embedded quotes are doubled.
- Values starting with `=` or `+` get a leading apostrophe as a spreadsheet-injection guard.
- Missing data files are not an error — they are simply treated as empty.

### Backup files

Every save writes a `.backup` copy plus a **timestamped `.bak` snapshot** next to the CSV. These accumulate quickly (hundreds of files over normal use). They are runtime artifacts, and `.gitignore` excludes them along with `Main/Data/*.csv`, so your local data and its backup history never get committed.

To reset to a clean slate, delete the contents of `Main/Data/` (keep `.gitkeep`).

---

## Features

### Core functionality
-  **Full CRUD** — add, update, and delete Students, Programs, and Colleges
-  **Real-time search** — filters as you type, across every field in the tab
-  **Column sorting** — click a header to sort, click again to reverse
-  **Pagination** — page size of 10 / 15 / 20 / 30 / 50, with Prev, Next, and jump-to-page
-  **Cascading updates** — renaming a program or college code follows through to every record that references it
-  **Delete warnings** — confirmation dialogs spell out exactly what a delete will affect
-  **Malformed-row warnings** — unreadable CSV rows are reported at startup instead of being silently dropped
-  **Duplicate detection** — duplicate codes and IDs are removed on load and rejected on entry

### Safety and validation
-  **Atomic saves** — data is written to a temp file and then moved into place, so an interrupted save can never leave a half-written CSV
-  **Rollback on failure** — if a multi-file save fails partway, in-memory state is restored and the files are re-synced
-  **CSV injection prevention** — special characters and formula prefixes are escaped on write
-  **Path traversal protection** — data paths must stay inside the working directory and end in `.csv`
-  **Input length limits** — enforced by the text fields themselves, not just on submit
-  **Search sanitization** — `< > " ' ; \` stripped, queries capped at 100 characters
-  **Field-level errors** — validation failures name the specific field that is wrong

---

## Validation rules

### Students
| Field | Rule |
|---|---|
| ID | Exactly `YYYY-NNNN` (regex `\d{4}-\d{4}`), must be unique |
| First / Last name | 1–50 chars, letters, spaces, hyphens, apostrophes (`^[a-zA-Z\s'\-]+$`) |
| Year | A single digit `1`–`6` |
| Gender | `M`, `F`, or `Other` |
| Program | Must reference an existing program, or be left unassigned |

### Programs
| Field | Rule |
|---|---|
| Code | **Uppercase letters only**, 2–20 (`^[A-Z]{2,20}$`), unique |
| Name | 1–100 chars, letters, spaces, hyphens, apostrophes, parentheses (`^[a-zA-Z\s'\-()]+$`) |
| College | Must reference an existing college, or be left unassigned |

### Colleges
| Field | Rule |
|---|---|
| Code | **Uppercase letters only**, 2–20 (`^[A-Z]{2,20}$`), unique |
| Name | 1–100 chars, letters, spaces, hyphens, apostrophes, parentheses (`^[a-zA-Z\s'\-()]+$`) |

> Codes reject digits. `BSCS` is valid; `BSIT2` and `bscs` are not — codes are not auto-uppercased, so type them in caps.

---

## How references behave

Deleting a record does **not** block on its dependents — it un-links them:

| Action | Effect |
|---|---|
| Delete a program | Enrolled students have their program set to `NULL` |
| Delete a college | Its programs have their college set to `NULL`, **and** students in those programs have their program set to `NULL` |
| Rename a program code | Enrolled students are updated to the new code |
| Rename a college code | Its programs are updated to the new code |

Each delete asks for confirmation first and states the consequence.

---

## User guide

**Adding** — fill the form fields, click **Add**. The record is validated and saved immediately.

**Updating** — select a row, edit the fields, click **Update**.

**Deleting** — select a row, click **Delete**, confirm the warning.

**Clear / Refresh** — **Clear** empties the form; **Refresh** reloads the table from memory.

**Searching** — type in the search box; results filter live. Search `null` to find records with no program assigned.

**Menu** — *File ▸ Import from CSV* reloads all data from disk, discarding unsaved changes. *File ▸ Export All to CSV* is informational only: it confirms that data is already written to the CSV files on every change. There is no separate export step or file chooser.

---

## Project structure

```
Main/
├── StudentInformationSystem.java   # JFrame, tabs, menu bar
├── Data/                           # CSV storage (created at runtime)
├── Managers/
│   ├── CSVManager.java             # Load/save, atomic writes, backups, path validation
│   └── DataManager.java            # In-memory state, validation, cascades, search
├── Models/
│   ├── Student.java                # + CSV escape/unescape
│   ├── Program.java
│   └── College.java
└── Panels/
    ├── StudentPanel.java           # Table, form, search, pagination
    ├── ProgramPanel.java
    └── CollegePanel.java
```

Compiled `.class` files are gitignored.
