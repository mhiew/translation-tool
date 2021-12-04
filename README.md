# Translation Management Tool

A small utility to help keep platform localization files in sync between Android and iOS Projects.  
This will audit the differences between Android and iOS strings and create a _synchronized android localization file_[^1].  
This synchronized file contains the iOS string values for _common string identifiers_[^2] on both platforms.

## Placeholder Mapping between Platforms

You can specify a list of text replacement using the `appConfig.textReplacements` option.  
When generating the synchronized android localization file all instances of `TextReplacement.target` will be replaced with the corresponding `TextReplacement.replacementValue`.

The default behaviour of the tool is to not replace any text that is not specified within the `appConfig.textReplacements`

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
To use this utility you must create a corresponding [.conf](./sample/translation-tool.conf) file[^3].

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

#optional (default emptylist) - this will replace all instances of target with the corresponding replacement value when generating the output android strings file
textReplacements = [
    {
        # In this case all instances of "%s" will be replaced with "%@"
        target = "%s"
        replacementValue = "%@"
    },
    {
        target = "$s",
        replacementValue= "$@"
    }
]
```

## Using the Jar
```
java -jar ~/translation-tool.jar <optional .conf file>
```

You can optionally specify the path to your own .conf file.  
If you do not specify the path to your .conf file it will default to accessing the **translation-tool.conf** within the same folder as the jar file.  

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