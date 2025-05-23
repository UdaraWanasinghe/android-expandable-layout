## Expandable Layout

[![android library](https://img.shields.io/badge/version-v1.0.2-orange)](https://github.com/UdaraWanasinghe/android-expandable-layout)

Animated Android view that expands and collapses with a smooth animation in horizontal or vertical
direction.

## Building

1. Clone this repository.

   ```shell
   git clone https://github.com/UdaraWanasinghe/android-expandable-layout.git
   cd expandable-layout
   ```

2. Add `local.properties` file and define the android sdk location. (`local.properties` file is
   automatically generated by android studio.)

   ```properties
   # local.properties
   sdk.dir=/home/udara/Android/Sdk
   ```

3. Run `gradle` task `publishToMavenLocal`.
   ```shell
   sh gradlew publishToMavenLocal
   ```

## Using

1. Include the `mavenLocal` repository in your project.

   ```groovy
   // settings.gradle.kts
   dependencyResolutionManagement {
       repositories {
           mavenLocal()
       }
   }
   ```
2. Import the library into your project.

   ```groovy
   // module level build.gradle.kts
   dependencies {
       implementation "com.aureusapps.android:expandable-layout:1.0.2"
   }
   ```

3. Wrap your views with the `ExpandableLayout`.

   ```xml
    <com.aureusapps.android.expandablelayout.ExpandableLayout
        android:id="@+id/expandable_layout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:layout_margin="8dp"
        app:animationDuration="2000"
        app:expandDirection="vertical"
        app:expanded="true"
        app:contentGravity="center"
        app:animationInterpolator="decelerate">

        <TextView
            style="@style/TextAppearance.MaterialComponents.Headline1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/frame"
            android:text="@string/hello_world"
            android:textAlignment="center"
            android:textColor="@color/white"
            android:textStyle="bold" />

    </com.aureusapps.android.expandablelayout.ExpandableLayout>
   ```

## Appreciate my work

If you like my work, please consider buying me a coffee.

<a href="https://www.buymeacoffee.com/udarawanasinghe" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy Me A Coffee" height="41" width="174"></a>