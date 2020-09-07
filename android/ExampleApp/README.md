An Example application requires the archived fjuul-sdk dependency.
If you're using Android Studio then the predefined run configuration will automatically prepare the archived dependency for you.
In case of you're working with gradle manually, in order to run the example app you need to build fjuul-sdk for the local maven repository by a
command from a directory of the root gradle project (/android):
`./gradlew clean publishToMavenLocal -PVERSION_NAME=105.1.1`
An every new change for a fjuul-sdk module will require rebuilding archives and refreshing dependencies
of the example app project.
