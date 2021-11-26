# Translation-Tool

A fork of the translation analyzer: https://github.com/jkwiecien/android-ios-translations-analyzer
This will be customized to report differences in resource keys between platforms

## Building The Jar

```
./gradlew clean build
```

## Using a jar

Just download a released jar and execute

```
java -jar build/libs/android-ios-strings-merger-1.0.jar <path_to_android_file> <path_to_ios_file> <path_to_output_folder>
```

## Output

It will create csv files for:

* strings that only exist on android `unique-android-strings.csv`
* strings that only exist on iOS `unique-ios-strings.csv`
* strings that match exactly on each platform `exact-matches.csv`
* strings that are different `differences.csv`
