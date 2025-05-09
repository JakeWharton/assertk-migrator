# AssertK Migrator

Migrate your repo from Truth assertions to AssertK (mostly) automatically.

I do not plan to maintain this tool.
It's an accelerator to an internal migration and then will be archived on GitHub.
Trivial contributions may be merged, but larger changes should just be done in a fork.

Our migration is complete, so the project is being archived.
Feel free to fork or make changes locally to support your own migrations.


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


## Automatic migration

Here's a quick Bash script to automatically migrate your modules one-by-one.
Once this script completes, the only modules left require manual intervention to compile and/or pass tests.

```bash
#!/usr/bin/env bash

dirs=$(git grep -r 'libs.truth' | cut -d':' -f1 | xargs -n 1 dirname | shuf)

for dir in $dirs; do
  git reset --hard HEAD
  git checkout master

  branch="$(whoami).assertk-$dir"
  git checkout -b "$branch" || continue

  /path/to/assertk-migrator/build/install/assertk-migrator/bin/assertk-migrator $dir

  ./gradlew -q -p "$dir" sortDep spotApply || continue
  ./gradlew -q -p "$dir" check || continue

  git commit -am "Migrate $dir to AssertK"
  git push origin "$branch"
  gh pr create --fill
  gh pr merge --merge --auto
done
```

It assumes:
- Truth can be identified by `libs.truth` dependency.
- You have `gh` installed and authenticated.
- `master` is your trunk branch.
- You have this repo cloned and you've run `./gradlew installDist` in it.
- Merge commits and auto-merge is enabled on your repo with appropriate branch protection.
- You have Spotless and https://github.com/square/gradle-dependencies-sorter applied on every module.

You should change:
- `/path/to/assertk-migrator` to be the path to your checkout of this repo.

Enjoy! …and use at your own risk!


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
