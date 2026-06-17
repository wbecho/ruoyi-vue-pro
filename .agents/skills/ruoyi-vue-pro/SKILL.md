```markdown
# ruoyi-vue-pro Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill provides guidance on the development patterns, coding conventions, and workflows used in the `ruoyi-vue-pro` Java codebase. It covers file naming, import/export styles, commit message conventions, and testing patterns to help contributors maintain consistency and quality.

## Coding Conventions

### File Naming
- **Pattern:** PascalCase
- **Example:**  
  ```java
  UserService.java
  OrderController.java
  ```

### Import Style
- **Pattern:** Relative imports
- **Example:**  
  ```java
  import com.example.service.UserService;
  import com.example.model.Order;
  ```

### Export Style
- **Pattern:** Named exports (Java's `public class`)
- **Example:**  
  ```java
  public class UserService {
      // class implementation
  }
  ```

### Commit Message Convention
- **Type:** Conventional Commits
- **Prefix:** `feat`
- **Average Length:** 11 characters
- **Example:**  
  ```
  feat: add user login endpoint
  ```

## Workflows

### Feature Development
**Trigger:** When implementing a new feature  
**Command:** `/feature`

1. Create a new branch for your feature.
2. Implement the feature following coding conventions.
3. Write or update tests as necessary.
4. Commit changes using the `feat` prefix.
5. Open a pull request for review.

### Code Review
**Trigger:** When reviewing a pull request  
**Command:** `/review`

1. Check for adherence to coding conventions (file naming, imports, exports).
2. Verify commit messages use the `feat` prefix and are concise.
3. Ensure tests are present and passing.
4. Provide feedback or approve the PR.

## Testing Patterns

- **Framework:** Unknown (not detected)
- **Test File Pattern:** `*.test.ts`
- **Example:**  
  ```typescript
  // UserService.test.ts
  import { UserService } from './UserService';

  test('should create a user', () => {
      // test implementation
  });
  ```

## Commands
| Command    | Purpose                                 |
|------------|-----------------------------------------|
| /feature   | Start a new feature development workflow|
| /review    | Begin code review workflow              |
```
