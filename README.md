# Sexual Health App - D5 Increment 1

**Course:** COMP2300 SDDP Group 30
**Sprint:** Increment 1 (D5 Deliverable)
**Delivered User Story:** SDDP30-19 - Calculator Disguise Interface (20 story points)

---

## Quick Start


### Prerequisites

Before you begin, ensure you have:
- **Java 17 or higher** - Check with `java -version`
- **Maven 3.6+** - Check with `mvn -version`
- **Git** - For version control
- **IDE** - IntelliJ IDEA recommended (JavaFX support built-in)

### Setup

```bash
# 1. Clone the repository
git clone https://git.soton.ac.uk/sddp-team-30-2025/sexual-health-app.git
cd sexual-health-app

# 2. Build the project
mvn clean compile

# 3. Run the app
mvn javafx:run

# 4. Run tests
mvn test
```

### First Run

1. App opens to **Setup Wizard**
2. Create a secret equation (e.g., `7 + 6 = 13`)
3. Calculator appears
4. Enter your secret equation to unlock main app
5. Click "Back to Calculator" to return to disguise

### Resetting Your Secret Equation

If you forget your secret equation or need to reset it:

1. Open the calculator
2. Enter `999 ÷ 0` and press equals
3. The secret equation will be deleted
4. You'll be redirected to the Setup Wizard to create a new one


---
### Package Organization

```
src/main/java/com/sddp/sexualhealthapp/
├── SexualHealthApp.java          # ← Main entry point
│
├── calculator/                   # ← Calculator disguise
│   ├── controller/               #    Calculator UI controllers
│   ├── model/                    #    Calculator data models
│   └── service/                  #    Calculator business logic
│
├── articles/                     # ← Example feature (YOUR FEATURES GO HERE! ✅)
│   ├── controller/               #    ArticleController, SearchController
│   ├── model/                    #    Article, Category
│   └── service/                  #    ArticleService, SearchService
│
├── calendar/                     # ← Another feature example
│   ├── controller/               #    CalendarController, EventController
│   ├── model/                    #    Event, Reminder
│   └── service/                  #    EventService
│
├── [your-feature]/               # ← Create new feature folders like these!
│   ├── controller/               #    Feature-specific controllers
│   ├── model/                    #    Feature-specific models
│   └── service/                  #    Feature-specific business logic
│
├── security/                     # ← Shared utilities
├── navigation/                   # ← Shared (read only - SceneManager)
└── util/                         # ← Shared (read only - constants)

src/main/resources/
├── fxml/                         # ← Your FXML files go here
│   ├── calculator.fxml           #    (calculator disguise)
│   ├── setup.fxml                #    (calculator disguise)
│   ├── main-app.fxml             #    (main menu)
│   ├── articles.fxml             # ← Feature-specific FXML
│   ├── calendar.fxml             # ← Feature-specific FXML
│   └── [your-feature].fxml       # ← Add your FXML files here
│
└── css/                          # ← Your CSS files go here
    ├── calculator-styles.css     #    (calculator styling)
    └── [your-feature].css        # ← Feature-specific styles

src/test/java/com/sddp/sexualhealthapp/
├── calculator/                   # Tests for calculator feature
├── articles/                     # Tests for articles feature
└── [your-feature]/               # Tests for your feature
```

**Architecture Rule:** Each major feature gets its own folder (like `calculator/`). This keeps code organized, reduces merge conflicts, and makes ownership clear.

## Troubleshooting

### Common Issues

**Build fails with "package does not exist"**
```bash
mvn clean compile  # Clean and rebuild
```

**JavaFX not found**
- Check Java version: `java -version` (needs Java 17+)
- Verify pom.xml has JavaFX dependencies

**App won't start**
- Ensure you're in the project root directory
- Try: `mvn clean javafx:run`

**Tests failing**
```bash
mvn clean test  # Clean build and run tests
```

**More help:** See [CONTRIBUTING.md](CONTRIBUTING.md) or ask in Discord dev channel

---

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CalculationEngineTest

# Run with coverage report
mvn clean test jacoco:report
```

### Test Coverage

**D5 Test Results:**
- ✅ **55 tests** - All passing
- ✅ **>80% coverage** on business logic
- ✅ **27 tests** for CalculationEngine (arithmetic, edge cases)
- ✅ **28 tests** for SecretAuthService (authentication flow, security)

### Test Structure

```
src/test/java/com/sddp/sexualhealthapp/
├── calculator/
│   └── service/
│       ├── CalculationEngineTest.java      # Tests calculator operations
│       └── SecretAuthServiceTest.java      # Tests authentication
│
├── articles/                                # YOUR TESTS GO HERE (feature-based)
│   ├── controller/                          # Test article controllers
│   ├── model/                               # Test article models
│   └── service/                             # Test article services
│
├── calendar/                                # Calendar feature tests
│   └── [controller/model/service tests]
│
└── [your-feature]/                          # Your feature tests
    └── [controller/model/service tests]
```

**Test Pattern:** Mirror your feature structure in the test folder. If you have 
`articles/model/Article.java`, create `articles/model/ArticleTest.java` in the test folder.

---

## Documentation


### Technical Documentation

- **[package-info.java](src/main/java/com/sddp/sexualhealthapp/package-info.java)** - Architecture overview
- **Javadoc** - Every class and method is documented
- **[pom.xml](pom.xml)** - Maven dependencies and build configuration

### Project Information

- **project-info/** - Sprint documentation, feedback, and reports
  - `feedbacks/` - Instructor feedback from previous sprints
  - Sprint reports and planning documents (add as needed)


---

## Project Status

### ✅ Completed (Sprint 1)

- Calculator disguise interface
- Secret equation authentication
- Setup wizard
- Secure storage (BCrypt)
- Comprehensive testing
- MVC architecture
- Team documentation

### 🔄 In Progress (Sprint 2)

Per D5 sprint plan:
- Event feed and calendar
- Medication reminders
- Account management
- Integration and polish

### 📋 Planned (Sprint 3)

Per D5 sprint plan:
- Forum and community features
- Article search and tagging
- Settings and preferences
- Final evaluation

---

## Team

**SDDP Group 30** - Southampton University COMP2300

| Name | Email | Role (Sprint 1) |
|------|-------|-----------------|
| Josh Wilcox | jw14g24@soton.ac.uk | Calculator interface lead |
| Safiy Hussain | sh6n24@soton.ac.uk | Article system design |
| Taran McVay | tm2n24@soton.ac.uk | Scrum Master, Article display |
| Sam Wiles | sw14g22@soton.ac.uk | Article search, Data privacy |
| Oliver Punter | op2g24@soton.ac.uk | Article packaging |

---

## License

Educational project for COMP2300 Software Development & Design Patterns course.

---

## Quick Links

- 📖 **New to the project?** Read [CONTRIBUTING.md](CONTRIBUTING.md)
- 🐛 **Found a bug?** Create an issue on GitLab
- 💬 **Questions?** Ask in Discord dev channel
- 📊 **Check progress?** View Jira board
- 🔧 **Build failing?** Run `mvn clean compile`

---

**For detailed feature addition examples, testing guides, and Git workflows, see [CONTRIBUTING.md](CONTRIBUTING.md).**
