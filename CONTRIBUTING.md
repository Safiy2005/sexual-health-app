# Contributing to Sexual Health App
---

## Quick Start

### Prerequisites
- Java 17 or higher installed
- IntelliJ IDEA (or another Java IDE)
- Git

### Getting Started
```bash
# Clone the repository
git clone [repository-url]
cd sexual-health-app

# Build the project
mvn clean compile

# Run the app
mvn javafx:run

# Run tests
mvn test
```

---

## Project Structure

```
src/main/java/com/sddp/sexualhealthapp/
├── SexualHealthApp.java          # ← Main entry point
│
├── calculator/                   # ← Calculator disguise
│   ├── controller/               #    Calculator UI controllers
│   ├── model/                    #    Calculator data models
│   └── service/                  #    Calculator business logic
│
├── articles/                     # ← Example feature
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

**Key Architecture Rule:** Each major feature (articles, calendar, medication, forum, 
account, etc.) gets its own top-level folder, following the calculator/ pattern. This 
keeps features self-contained, reduces merge conflicts, and makes team ownership clear.

---

## MVC Pattern Explained

MVC = Model-View-Controller. It's a way to organize code.

### Model (Data)
- **What**: Classes that hold data
- **Where**: `mainapp/model/`
- **Example**: `Article.java`, `Event.java`, `Reminder.java`
- **Contains**: Data fields, getters, setters, validation

```java
// Example Model
public class Event {
    private String name;
    private LocalDate date;

    // Getters and setters...
}
```

### View (UI)
- **What**: FXML files that define the user interface
- **Where**: `src/main/resources/fxml/`
- **Example**: `articles.fxml`, `events.fxml`
- **Contains**: Buttons, labels, text fields, layouts

```xml
<!-- Example View -->
<VBox>
    <Label text="Welcome"/>
    <Button text="Click Me" onAction="#handleClick"/>
</VBox>
```

### Controller (Logic)
- **What**: Classes that connect Model and View
- **Where**: `mainapp/controller/`
- **Example**: `ArticleController.java`, `EventController.java`
- **Contains**: Button click handlers, data loading, navigation

```java
// Example Controller
public class ArticleController {
    @FXML
    private void handleClick(ActionEvent event) {
        // Logic here
    }
}
```

**Simple Rule**:
- **Model** = "What data do I have?"
- **View** = "What does the user see?"
- **Controller** = "What happens when the user clicks?"

---

## Testing Your Code

### Running the App

```bash
# Compile
mvn clean compile

# Run
mvn javafx:run
```

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test
mvn test -Dtest=ArticleServiceTest
```

### Writing a Test

Create test in `src/test/java/com/sddp/sexualhealthapp/mainapp/service/`:

```java
package com.sddp.sexualhealthapp.mainapp.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArticleServiceTest {

    @Test
    void testArticleCreation() {
        Article article = new Article("Title", "Content", "Category");

        assertEquals("Title", article.getTitle());
        assertEquals("Content", article.getContent());
        assertEquals("Category", article.getCategory());
    }
}
```

---

## Git Workflow

### Creating a Feature Branch

```bash
# Create a new branch for your feature
git checkout -b feature/articles-display

# Make your changes...

# Stage your changes
git add .

# Commit with a clear message
git commit -m "feat(articles): add article list display

- Created ArticleController
- Added articles.fxml
- Implemented article loading"

# Push to GitLab
git push origin feature/articles-display
```

### Commit Message Format

```
<type>(<scope>): <subject>

<body>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding tests
- `refactor`: Code refactoring

**Examples:**
```
feat(articles): add article search functionality
fix(calculator): correct division by zero handling
docs(readme): update setup instructions
test(auth): add unit tests for secret equation validation
```

### Pull Request Process

1. Push your feature branch
2. Create a Merge Request on GitLab
3. Request review from team members
4. Address any feedback
5. Once approved, merge into `main`

---

## Getting Help

### Common Issues

**Issue: App won't compile**
```bash
# Clean and rebuild
mvn clean compile
```

**Issue: JavaFX not found**
```bash
# Make sure you're running with Maven
mvn javafx:run
# NOT: java -jar ...
```

**Issue: FXML not loading**
- Check the path in `fx:controller` matches your package
- Check the FXML file is in `src/main/resources/fxml/`
- Check method names in `onAction` match your controller methods

**Issue: Tests failing**
```bash
# Run tests with more detail
mvn test -X
```

### Resources

- [JavaFX Documentation](https://openjfx.io/)
- [JUnit 5 Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Git Basics](https://git-scm.com/book/en/v2/Getting-Started-Git-Basics)

### Team Communication

- **Discord**: Ask questions in the dev channel
- **GitLab Issues**: Report bugs or request features
- **Team Meetings**: Weekly standup meetings

---

## Checklist for New Features

Before submitting a pull request, make sure:

- [ ] Code compiles without errors (`mvn clean compile`)
- [ ] Tests pass (`mvn test`)
- [ ] Code is in the correct package (`mainapp.*` for new features)
- [ ] FXML files are in `src/main/resources/fxml/`
- [ ] Controller methods have `@FXML` annotation
- [ ] Code is properly documented with Javadoc comments
- [ ] No hardcoded strings (use `AppConstants` instead)
- [ ] Feature is linked to a user story in Jira
- [ ] Git commit message follows format
- [ ] Pull request has a clear description

---

**Good luck and happy coding!** 🚀

If you get stuck, don't hesitate to ask the team for help!
