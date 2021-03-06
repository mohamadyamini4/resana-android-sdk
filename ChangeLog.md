# Resana Change Log

## 8.0.0 STABLE
*  remove create methods
*  release method deprecated and will be removed on January 2019
*  remove title from getting native ads
*  make Resana class Singleton
*  remove downloading apks directly from apk
*  getVisual method is deprecated and will be removed on January 2019
*  remoed AdDelegate

## 7.4.6 STABLE
* bug fixed in showing ads instantly
* bug fixed in getting ads with zone
* checking storage permission befor downloading apk files
* bug fixed in installing apk files
* adding ad delegate for ads which have apk to download
* reporting time of downloading files to server
* performance improved

## 7.4.1 
* bug fixed in blocked zones
* SDK_VERSION_NUM = 18
* DB_VERSION = 8

## 7.4.0 STABLE
* added Resana.init
* bug fixed logging
* SDK_VERSION_NUM = 18
* DB_VERSION = 8

## 7.3.0 STABLE
* downloading and installing apk when clicking on ad.
* deleting corrupt downloaded files.
* some bugs fixed.
* SDK_VERSION_NUM = 17
* DB_VERSION = 7

## 7.1.0 STABLE
* handling apk. ads that their apk file exists will not show.
* removing some part of subtitle ads. subtitle ads will completly removed as soon as december 1,2018
* creating resana configuration for developers to select type of ads and visuals.
* SDK_VERSION_NUM = 17
* DB_VERSION = 7

## 6.3.4 STABLE
* some enhancements in landing
* some bugs fixed about background color of splash ads 
* SDK_VERSION_NUM = 16
* DB_VERSION = 6

## 6.3.2 
* subtitle ads are deprecated
* SDK_VERSION_NUM = 16
* DB_VERSION = 6

## 6.3.0
* proguard enabled for resana (just for 6.3.0-min)
* subtitle ads deleted
* bug fixed for ads without landing
* handling and counting apk installing
* SDK_VERSION_NUM = 15
* DB_VERSION = 6

## 6.2.0
* downloading only max view number of visuals of native ads.
* showing visual to user randomly.(NativeAd.getVisual() returns a single visual. not a list)
* there is no link for native ad visuals. user can get file of a visual by getFile() method.
* SDK_VERSION_NUM = 14

## 6.1.3
* some bugs fixed.
* downloading visuals for native ads in background.
* only giving video or image file to user



