# AssertK Migrator

Migrate your repo from Truth assertions to AssertK (mostly) automatically.

I do not plan to maintain this tool.
It's an accelerator to an internal migration and then will be archived on GitHub.
Trivial contributions may be merged, but larger changes should just be done in a fork.


## Usage

You must already have a version catalog entry for AssertK.
By default, the script will use `libs.assertk`, but you can change the name with the `--assertk` option.
For example, `assertk-migrator --assertk "test.assertk" path/to/repo` will produce dependencies which use `libs.test.assertk`.

By default, the script will look for `libs.truth`, but you can change the name with the `--truth` option.
For example, `assertk-migrator --truth "test.truth" path/to/repo` will look for `libs.test.truth` dependencies.

The script will print out each file that is being migrated _before_ it is migrated and overwritten.

```
$ assertk-migrator path/to/folder
BUILD module-a/build.gradle
SOURCE module-a/src/test/kotlin/FooTest.kt
SOURCE module-a/src/test/kotlin/BarTest.kt
BUILD module-b/build.gradle
SOURCE module-b/src/test/kotlin/PingTest.kt
SOURCE module-b/src/test/kotlin/PongTest.kt
```

### Accuracy

This tool assumes some things:

- ~90% of files will just compile and work
  - You will have to manually fix up ~10% of files for various reasons
  - Getting to 100% is [not worth our time](https://xkcd.com/1319/).
- You are using something like [ktlint](https://pinterest.github.io/ktlint) to sort and remove the unused dependencies
- You are using something like [gradle-dependencies-sorter](https://github.com/square/gradle-dependencies-sorter) to sort the dependency declarations.

### What about `org.junit.Assert` or `kotlin.test`?

These can be migrated with IntelliJ or `sed` or the like with the same level of fidelity as this tool could do.


## License

    Copyright 2024 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
