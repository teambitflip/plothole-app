# Plothole - Android App

The mobile component of Plothole consists of an Android app the citizens can use to click photos of the potholes they spot. This repository has the
code for the android app.

The photo clicked by the user is saved in local storage and then along with the GPS coordinates are written to Firebase Storage and Databse. After a successful write to firebase, the image id is then sent to the Plothole backend server for further inference.

## Dependencies

The app is written in Kotlin. The `PlotholeApp` directory can be opened up as a regular Android Studio project

* [Firebase UI](https://firebase.google.com/docs/android/setup)
* [Firebase Auth](https://firebase.google.com/docs/auth), [Storage](https://firebase.google.com/docs/storage) and [Database](https://firebase.google.com/docs/database)
* [OkHTTP](https://square.github.io/okhttp/)

As the project is not live currently, please make sure you put in the correct URLs in the `ReportFragment.kt` file.

## Screenshots

<p align="center">
<img src="screenshots/a.jpg" width="40%">
<img src="screenshots/b.jpg" width="40%">
</p>

## Contribute

Feel free to open issues and PRs regarding the features you would like to work on. Some features on the roadmap are mentioned below,

* UI Overhaul
* Better notifications regarding submissions
* View all the user's submimissions

## LICENSE
Copyright (c) **Team BitFlip**. All rights reserved. Licensed under the MIT License

[![](https://img.shields.io/github/license/junaidrahim/desiresalesportal?style=for-the-badge)](LICENSE)