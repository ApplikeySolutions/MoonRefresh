[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/AppliKey/MoonRefresh/blob/master/LICENSE)
[![Twitter](https://img.shields.io/badge/contact-@Applikey_-blue.svg?style=flat)](https://twitter.com/Applikey_)

Made by [Applikey Solutions](https://applikeysolutions.com)

![Alt text](screenshots/Moon-Refresh.png?raw=true)

# Table of Contents
1. [Purpose](#purpose)
2. [Installation](#installation)
3. [Usage](#usage)
4. [Demo](#demo)
5. [Release Notes](#release-notes)
6. [Contact Us](#contact-us)
7. [License](#license)


# Purpose

MoonRefresh is a simple pull to refresh animation for Android completely written in Kotlin

# Installation

```groovy
compile 'com.github.applikeysolutions:moonrefresh:1.0.0'
```

# Usage

```kotlin
 moonRefresh = findViewById(R.id.pull_to_refresh)
        moonRefresh.setOnRefreshListener(
                object : MoonRefresh.OnRefreshListener {
                    override fun onRefresh() {
                        moonRefresh.postDelayed({ moonRefresh.setRefreshing() }, 4_000)
                    }
                })
```
```xml
<applikeysolutions.com.moonrefresh.MoonRefresh
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:ballColor="@color/color..."
        app:finishColor="@color/color..."
        app:backgroundColor="@color/color...">
        
         <android.support.v7.widget.RecyclerView
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </applikeysolutions.com.moonrefresh.MoonRefresh>
```
# Demo

![Alt text](screenshots/pull_to_refresh.gif?raw=true)

# Release Notes

Version 1.0.0

- Release version.

# Contact Us

You can always contact us via github@applikey.biz We are open for any inquiries regarding our libraries and controls, new open-source projects and other ways of contributing to the community. If you have used our component in your project we would be extremely happy if you write us your feedback and let us know about it!

# License

    MIT License

    Copyright (c) 2017 Applikey Solutions

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
