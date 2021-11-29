# Translation Management Tool

A small utility to help keep platform localization files in sync between Android and iOS Projects.  
This will audit the differences between Android and iOS strings and create a _synchronized android localization file_[^1].  
This synchronized file contains the iOS string values for _common string identifiers_[^2] on both platforms.

## Placeholder Mapping between Platforms
Currently the mapping of placeholders from [iOS](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/Strings/Articles/formatSpecifiers.html#//apple_ref/doc/uid/TP40004265-SW1) to [Android](https://developer.android.com/reference/java/util/Formatter.html#syntax) is hardcoded within [AndroidValueSanitizer.kt](./src/main/kotlin/com/minhiew/translation/AndroidValueSanitizer.kt).

_For the time being all iOS placeholders map to %@ on Android which is not standard to the Android platform_

__TODO: Allow placeholder mapping from iOS to Android to be a configurable option__

## Handling placeholder count mismatches
Unexpected changes in the number of text copy placeholders between platforms can create easy to miss regressions.

For example consider the scenario where the number of placeholders on android was originally 2:
```
<string name="welcome_messages">Hello, %1$s! You have %2$d new messages.</string>

var text = getString(R.string.welcome_messages, username, mailCount)
```

On iOS the text copy for this key has 3 placeholders
```
"welcome_messages" = "Hello, %1$@! You have %2$d new messages. More info at %3$@.";
```

Copying over this text could lead to strange regressions that are not easily detected.  
In these scenarios the tool by default will not replace this text on android and will instead warn you within the [differences.csv](./sample/output/en/1%20WARNINGS%20-%20differences.csv) file.

If you want to override this default behaviour you can update the `blockPlaceholderMismatch` configuration option to `false`


## Building The Jar
```
./gradlew clean build
```

## Configuration
To use this utility you must create a corresponding [translation-tool.conf](./sample/translation-tool.conf) file[^3] within the same folder as the jar file.

```
# required - path to output folder
outputDirectory = "./output"

# optional (default false) - setting this to true deletes the output directory before every run
cleanOutputDirectory = true

# optional (default true) - whether we should overwrite android strings when placeholder counts do not match between platforms
blockPlaceholderMismatch = true

# optional (default true) - whether we should use the main android strings file as the base of all translations.
# when this option is set to false it will use each corresponding language localization file as the base instead of the main one
# we default this option to true so that comments and changes to the main android strings file can propagate to all other locales
useMainAndroidFileAsBaseTemplate = true

# optional (default false) - replace android source files with synchronized versions
replaceAndroidSourceFile = false

# required - The main localization (usually English) that will be synchronized from ios to android
main {
    language = "en"
    androidFile =  "./input/android/values/strings.xml"
    iosFile = "./input/ios/Base.lproj/Localizable.strings"
}

# optional (default empty list) - List of other localizations to synchronize
localizations = [
	{
        language = "fr"
        androidFile = "./input/android/values-fr/strings.xml"
        iosFile = "./input/ios/fr.lproj/Localizable.strings"
    },
    {
        language = "fr-CA"
        androidFile = "./input/android/values-fr-rCA/strings.xml"
        iosFile = ".input/ios/fr-CA.lproj/Localizable.strings"
    }
]
```

## Using the Jar
```
java -jar ~/translation-tool.jar 
```

For additional information view the [sample](./sample) folder.

## Output

It will compare the localization differences per platform and generate csv files for:
- strings that only exist on Android `unique-android-strings.csv`
- strings that only exist on iOS `unique-ios-strings.csv`
- strings that match exactly between the platform `exact-matches.csv`
- strings that are different between the platforms `differences.csv` (*The name of this file changes if there are mismatched placeholder counts ie. "3 WARNINGS - differences.csv"*)

It will also create a synchronized android localization file where any differences default to the iOS text copy.

# Credits
Originally Inspired by https://github.com/jkwiecien/android-ios-translations-analyzer

[^1]: Synchronized android localization files will only replace text for common string identifiers that exist on both platforms.   
  Android text copy will not be changed for string identifiers that exist only within the android localization file.  
  Similarly, string identifiers that exist only within the iOS localization file will not be synchronized to android.  

[^2]: Common String Identifiers refers localization keys that match exactly between Android and iOS.  
  On Android `<string name="common_key">some text...</string>`  
  On iOS `"common_key" = "some text...";`

[^3]: We use [HOCON](https://github.com/lightbend/config/blob/main/HOCON.md) configuration syntax.