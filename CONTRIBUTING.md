# How to contribute to Eclipse Ditto' WoT tooling
 
First of all, thanks for considering to contribute to Eclipse Ditto' WoT tooling. We really appreciate the time and effort you want to
spend helping to improve things around here.

In order to get you started as fast as possible we need to go through some organizational issues first, though.

## Legal Requirements

Ditto is an [Eclipse IoT](https://iot.eclipse.org) project and as such is governed by the Eclipse Development process.
This process helps us in creating great open source software within a safe legal framework.

For you as a contributor, the following preliminary steps are required in order for us to be able to accept your contribution:

* Sign the [Eclipse Foundation Contributor Agreement](https://www.eclipse.org/legal/ECA.php).
In order to do so:
  * Obtain an Eclipse Foundation user ID. Anyone who currently uses Eclipse Bugzilla or Gerrit systems already has one of those.
If you don't already have an account simply [register on the Eclipse web site](https://dev.eclipse.org/site_login/createaccount.php).
  * Once you have your account, log in to the [projects portal](https://projects.eclipse.org/), select *My Account* and then the *Contributor License Agreement* tab.

* Add your GitHub username to your Eclipse Foundation account. Log in to Eclipse and go to [Edit my account](https://dev.eclipse.org/site_login/myaccount.php).

The easiest way to contribute code/patches/whatever is by creating a GitHub pull request (PR). When you do make sure that you *Sign-off* your commit records using the same email address used for your Eclipse account.

You do this by adding the `-s` flag when you make the commit(s), e.g.

    $> git commit -s -m "Shave the yak some more"

You can find all the details in the [Contributing via Git](http://wiki.eclipse.org/Development_Resources/Contributing_via_Git) document on the Eclipse web site.

## Codestyle

We use the [Google Java Style Guide](https://github.com/google/styleguide) where a formatter for Eclipse IDE is available. 

The only adjustment: use longer lines ("line split") with 120 characters instead of only 100.

## Making your Changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Make your changes
* Make sure you include test cases for non-trivial features
* Make sure the test suite passes after your changes
* Please make sure to format your code with the above mentioned formatter
* Commit your changes into that branch
* Use descriptive and meaningful commit messages
* If you have more than one commit, squash your commits into a single commit 
* Make sure you use the `-s` flag when committing as explained above
* Push your changes to your branch in your forked repository

## License header

Please make sure any file you newly create contains a proper license header. Find the latest one in use here:
[legal/headers/license-header.txt](legal/headers/license-header.txt)

Adjusted for Java classes:
```java
/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```

Adjusted for XML files:
```xml
<!--
  ~ Copyright (c) 2025 Contributors to the Eclipse Foundation
  ~
  ~ See the NOTICE file(s) distributed with this work for additional
  ~ information regarding copyright ownership.
  ~
  ~ This program and the accompanying materials are made available under the
  ~ terms of the Eclipse Public License 2.0 which is available at
  ~ http://www.eclipse.org/legal/epl-2.0
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->
```

## Codestyle

We follow Kotlin coding conventions and best practices:

- Use 4 spaces for indentation
- Maximum line length of 120 characters
- Follow Kotlin naming conventions
- Use meaningful variable and function names
- Add proper documentation for public APIs
- Include unit tests for new functionality

## Making your Changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Make your changes
* Make sure you include test cases for non-trivial features
* Make sure the test suite passes after your changes
* Please make sure to format your code according to Kotlin conventions
* Commit your changes into that branch
* Use descriptive and meaningful commit messages
* If you have more than one commit, squash your commits into a single commit
* Make sure you use the `-s` flag when committing as explained above
* Push your changes to your branch in your forked repository

## License header

Please make sure any file you newly create contains a proper license header. Use the following format for Kotlin files:

```kotlin
/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```

For XML files:
```xml
<!--
  ~ Copyright (c) 2025 Contributors to the Eclipse Foundation
  ~
  ~ See the NOTICE file(s) distributed with this work for additional
  ~ information regarding copyright ownership.
  ~
  ~ This program and the accompanying materials are made available under the
  ~ terms of the Eclipse Public License 2.0 which is available at
  ~ http://www.eclipse.org/legal/epl-2.0
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->
```

## Testing

Before submitting your changes, please ensure:

1. All existing tests pass: `mvn test`
2. The project builds successfully: `mvn clean install`
3. New functionality has appropriate test coverage
4. Integration tests pass if applicable

## Submitting the Changes

Submit a pull request via the normal GitHub UI. Please include:

- A clear description of the changes
- Reference to any related issues
- Test results showing the changes work as expected
- Any breaking changes or migration notes

## After Submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.
* Respond to any review comments promptly
* Be patient with the review process

## Development Environment Setup

1. **Prerequisites**:
    - JDK 21 or higher
    - Apache Maven 3.9.x or higher
    - Git

2. **IDE Setup**:
    - IntelliJ IDEA (recommended) or Eclipse with Kotlin plugin
    - Import as Maven project
    - Ensure Kotlin plugin is installed and configured

3. **Build and Test**:
   ```bash
   mvn clean install
   mvn test
   ```

## Project Structure

- `src/main/kotlin/`: Main source code
- `src/test/kotlin/`: Test code
- `src/main/resources/`: Configuration files
- `generated/`: Output directory for generated OpenAPI specs

## Common Development Tasks

### Adding a New Feature

1. Create a feature branch
2. Implement the feature with tests
3. Update documentation if needed
4. Run full test suite
5. Submit pull request

### Fixing a Bug

1. Create a bug fix branch
2. Write a test that reproduces the bug
3. Fix the bug
4. Ensure the test passes
5. Run full test suite
6. Submit pull request

### Code Review Guidelines

- Be constructive and respectful
- Focus on the code, not the person
- Suggest improvements rather than just pointing out issues
- Use inline comments for specific suggestions
- Provide context for your feedback

## Questions or Need Help?

If you have questions or need help with contributing:

- Open an issue on GitHub
- Check existing documentation
- Review existing pull requests for examples
- Ask in the project discussions

Thank you for contributing to Eclipse Ditto's WoT tooling!
