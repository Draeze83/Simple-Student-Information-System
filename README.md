## Simple Student Information System

This is a desktop Student Information System written in Java Swing that stores all data in plain CSV files.

It lets you manage students, programs, and colleges with basic validation and safety checks.

---

## 🎯 Features

### Core Functionality
- ✅ **CRUD Operations** - Create, Read, Update, Delete for Students, Programs, and Colleges
- ✅ **Real-time Search** - Instant search across all fields in each entity
- ✅ **Column Sorting** - Click headers to sort data (ascending/descending)
- ✅ **CSV Import/Export** - Load from and export data to CSV files
- ✅ **Data Validation** - Comprehensive input validation and sanitization
- ✅ **Referential Integrity** - Prevents deletion of referenced entities
- ✅ **Error Handling** - Clear error messages and failure recovery

### 🔒 Security Features
- ✅ **CSV Injection Prevention** - Automatic escaping of special characters
- ✅ **Path Traversal Protection** - Validates all file paths
- ✅ **Atomic Save Operations** - Backup before overwrite with rollback
- ✅ **Input Length Limits** - Prevents memory exhaustion attacks
- ✅ **Format Validation** - Regex validation for all inputs
- ✅ **Error Propagation** - Users notified of all failures
- ✅ **Code Sanitization** - Alphanumeric-only codes prevent injection
- ✅ **Safe Character Sets** - Names restricted to letters, spaces, hyphens

---

## 📊 Entity Structure

### Students
- **ID**: YYYY-NNNN format (e.g., 2024-0001)
- **Names**: 1-50 characters, letters/spaces/hyphens only
- **Year**: 1-6 only
- **Gender**: M, F, or Other
- **Program**: Must reference existing program

### Programs
- **Code**: 2-20 uppercase alphanumeric (e.g., BSCS)
- **Name**: 1-100 characters
- **College**: Must reference existing college

### Colleges
- **Code**: 2-20 uppercase alphanumeric (e.g., CCS)
- **Name**: 1-100 characters

---

### 🤨 Validation Rules:
- ✅ Student ID: Must match `\d{4}-\d{4}`
- ✅ Names: `[a-zA-Z\s'-]+` pattern
- ✅ Codes: `[A-Z0-9]{2,20}` pattern
- ✅ Year: Integer 1-6 only
- ✅ Gender: M, F, or Other only

---

## 🎮 User Guide

### Adding Records
1. Fill form fields
2. Click **Add**
3. System validates and saves
4. Backup created automatically

### Searching
- Type in search box
- Filters in real-time
- Searches all fields

### Sorting
- Click column header
- Click again to reverse

### Exporting
- Click **Export CSV**
- Choose location
- Data saved with proper escaping
