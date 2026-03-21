<p align="center">
    <a href="https://magento.com">
        <img src="https://static.magento.com/sites/all/themes/magento/logo.svg" width="300px" alt="Magento Commerce" />
    </a>
</p>

<!-- Plugin description -->
# PhpStorm Magento 2 Plugin

<table align="center" style="border-collapse: collapse; width: 100%; text-align: center;">
  <caption style="font-size: 1.2em; margin-bottom: 10px;">
    <strong>PhpStorm IDE Plugin</strong> for a better Magento 2 development workflow.
  </caption>
  <thead>
  </thead>
  <tbody>
  </tbody>
  <tfoot>
    <tr style="background-color: #f9f9f9;">
      <td colspan="3" style="padding: 20px;">
        <h3 style="margin: 10px 0;">Support the Project</h3>
        <p>If you find this plugin helpful and want to support its development, consider buying the contributors a coffee:</p>
        <a href="https://buymeacoffee.com/vitalii_b" style="text-decoration: none;">
          <img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Donate-orange.svg" alt="Buy Me a Coffee" style="margin: 10px 0;">
        </a>
        <p style="margin: 10px 0;">Thank you to our sponsors—your support means everything:</p>
        <p><strong>Lucas van Staden</strong></p>
        <p><strong>Ivan Chepurnyi</strong></p>
        <p><strong>Michael Ryvlin</strong></p>
      </td>
    </tr>
  </tfoot>
</table>

## Features

* Configuration smart completion and references for XML/JavaScript files
* `Navigate to configuration` reference in scope of class/interface
* `Go to plugin` reference in scope of class/interface and method
* `Navigate to Web API configuration` reference in scope of class/interface and method
* Plugin class methods generation
* Plugin declaration inspection
* RequireJS reference navigation and completion
* MFTF reference navigation and completion
* GraphQL navigation line markers
* Code generation
* Inspections for XML configuration

<!-- Plugin description end -->

[![Version](http://phpstorm.espend.de/badge/8024/version)](https://plugins.jetbrains.com/plugin/8024)
[![Downloads](http://phpstorm.espend.de/badge/8024/downloads)](https://plugins.jetbrains.com/plugin/8024)
[![Made With Love](https://img.shields.io/badge/Made%20With-Love-orange.svg)](https://magento.com)

## Installation

1. Open `Settings` / `Preferences` in PhpStorm
2. Navigate to `Plugins > Marketplace`
3. Search for `Magento PhpStorm`
4. Install the plugin and restart PhpStorm
5. Go to `Settings / Preferences > Languages & Frameworks > PHP > Frameworks > Magento`
6. Check `Enable` and click the `OK` button

## Works with

* PhpStorm 2025-2026+
* Java runtime 21+

## Setting up development environment

1. Check out this repository.
2. Open the project in a JetBrains IDE with Gradle support.
3. Install or select JDK 21 for the project and Gradle import.
4. Import the Gradle project from `build.gradle.kts`.
5. Run `./gradlew runIde` from the terminal, or use the Gradle tool window and run the `runIde` task.
6. Gradle will start a sandboxed PhpStorm instance based on the configured IntelliJ Platform target (`PS 261.21525.38`).
7. After indexing finishes, enable the plugin in the sandbox IDE if needed and verify the Magento features there.

Useful local commands:

```bash
./gradlew test check
./gradlew buildPlugin
./gradlew publishPlugin
```

## How to contribute
1) Start with looking into [Community Backlog](https://github.com/magento/magento2-phpstorm-plugin/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22). Any ticket in `Ready for Development` and `Good First Issue` columns are a good candidates to start.
2) Didn't satisfy your requirements? [Create a new issue](https://github.com/magento/magento2-phpstorm-plugin/issues/new). It can be for example:
    - **Bug report** - Found a bug in the code? Let us know!
    - **Enhancement** - Know how to improve existing functionality? Open an issue describe how to enhance the plugin.
    - **New feature proposal** - Know how to make a killer feature? Do not hesitate to submit your proposal.
3) The issue will appear in the `Ready for Grooming` column of the [Community Backlog](https://github.com/magento/magento2-phpstorm-plugin/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22). Once it will be discussed and approved the issue will be ready for development.
4) Refer to the [Contributing Guide](.github/CONTRIBUTING.md) for more information on how to contribute.

## Learn to contribute
1) SDK [Developing a Plugin](https://plugins.jetbrains.com/docs/intellij/developing-plugins.html)
2) Good Presentation about platform [How We Built Comma, the Raku IDE, on the IntelliJ Platform](https://www.youtube.com/watch?v=zDP9uUMYrvs)
3) Plugin example [idea-php-symfony2-plugin](https://github.com/Haehnchen/idea-php-symfony2-plugin)

## How to create SandBox for development
1. Create sandbox folder
2. Copy to sandbox folder `composer.json` and `composer.lock`
3. In sandbox folder create `app/code` and `vendor/magento`
4. Copy any of the magento modules (as for example: `framework`, `module-catalog`, `module-checkout`, `module-customer`, `module-sales`) into the `vendor/magento` folder. It is better to add as few modules as possible to reduce reindexing time during application running
5. (Nice to have) Open IDE and go to `Preferences > Editor > File and Code Templates > Includes tab` and add default headers for `PHP File Header` and `XML File Header`

**PHP File Header:**
```php
/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
declare(strict_types=1);
```

**XML File Header:**
```xml
<!--
/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
-->
```

### <img src="https://upload.wikimedia.org/wikipedia/commons/7/76/Slack_Icon.png" width="20"> Join the [#phpstorm-plugin](https://magentocommeng.slack.com/archives/C010C2LUCEA) Slack channel to get more involved

## License

Each Magento source file included in this distribution is licensed under OSL-3.0 license.

Please read the [LICENSE.txt](https://github.com/magento/magento2-phpstorm-plugin/blob/5.4.0-develop/LICENSE.txt) for the full text of the [Open Software License v. 3.0 (OSL-3.0)](http://opensource.org/licenses/osl-3.0.php).
