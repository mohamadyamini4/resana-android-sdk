# Resana User Guide

To use Resana in Android

```ruby
allprojects {
...
    repositories {
      maven { url "https://maven.oddrun.ir/artifactory/resana" }
     }
 }    
 
    
dependencies {
    implementation( 'io.resana:resana:8.0.0@aar' ) {transitive = true}
}
```
Resana progourd

```ruby
-keep class io.resana.**{ *; }
```

add the following codes to your manifest file

```ruby
<permission
        android:name="PACKAGE NAME.permission.RESANA_ADS"
        android:protectionLevel="signature" />
    <uses-permission android:name="PACKAGE NAME..permission.RESANA_ADS" />
 
<application
 ...
 <meta-data
            android:name="ResanaMediaId"
            android:value="YOUR RESANA MEDIA ID" />
 ...
</application>
```

<br />
Initialize Resana at the beginning of you application by caling Resana.init method

```ruby
Resana.init(Context, ResanaConfig)
```

`resanaCongig` is configuration of resana and should be implemented like this:

```ruby
ResanaConfig resanaConfig = new ResanaConfig(ResanaConfig.AdTypes[], ResanaConfig.VisualTypes[]);
```

first argument of this class is an array of ads you want to use and second is the array of visual types you want.

* `ResanaConfig.AdType.NATIVE`: native ads
* `ResanaConfig.AdType.SPLASH`: splash ads
* `ResanaConfig.VisualType.SQUARE`: square visual of ad
* `ResanaConfig.VisualType.HORIZONTAL`: horizental visual of ad
* `ResanaConfig.VisualType.OROGINAL`: original visual of ad

with `Resana.getInstance()` you can get an instance of resana. <br />
with `Resana.setLogLevel(int logLevel)` you can set Resana log level. this methods can take these values:

`Resana.LOG_LEVEL_VERBOSE`: Resana will log every thing <br />
`Resana.LOG_LEVEL_DEBUG`: Resana will log only debug logs <br />
`Resana.LOG_LEVEL_NO_LOG`: Resana will log nothing <br />


### Resana Native Ad
Developer of host application will decide how to show native ads.<br />
Resana itself handles click and landing showing.
<br /><br />
By the following code you can get a native ad and show it.

```ruby
NativeAd ad = Resana.getInstance().getNativeAd();
NativeAd ad = Resana.getInstance().getNativeAd(String zone);
```

There are two main groups of native ads. ads which have titles and ads which not. <br />
Some native ads have zone. zone is place of ad in application. <br /> <br />
getNativeAd can some times return null. in this case there is no ad available. if this method doesn't returns null, you can get the data of ad and show it.

### Resana Native Ad methods
The following method will return the String should be written in the ads click button. 

```ruby
String nativeAd.getCallForAction()
```

<br />
The following method will return ads background color.

```ruby
String nativeAd.getBackgroundColor()
```

<br />
Texts that should be shown have two main groups. Title and Ordinary text. these are provided in two versions: short and medium. by the following methods you can get the texts of ad. <br />

```ruby
String nativeAd.getShortOrdinaryText()

String nativeAd.getMediumOrdinaryText()

String nativeAd.getShortTitleText()

String nativeAd.getMediumTitleText()
```

Each native ad has some visuals. visual can be picture, video or webview. each visual has followings:
* `orgVisual`: original visual
* `sqVisual`: square visual
* `hrzLanding`: horizontal visual

by the following methods you can get visual data of a native ad. <br />

```ruby
File getFile();

int getType();

int getHeight();

int getWidth();
```
Some examples for getting visual data of an ad. <br />

```ruby
int type = ad.getSqVisual().getType()
 
File file = ad.getHrzLanding().getFile()
 
int type = ad.getHrzLanding().getType()
 
int height = ad.getHrzLanding().getHeight()
 
int width = ad.getHrzLanding().getWidth()
```
it is recommended to user Glide or Picasso library for showing ads image files.<br />

```ruby
File file = ad.getHrzLanding().getFile()

Picasso.get().load(file).into(adImage);
```
### Reporting Native Ad events to Resana server
When you show ad with its texts and visual you should call this method: <br />

```ruby
resana.onNativeAdRendered(NativeAd ad)
```

when a native ad is clicked, you should use this method in OnClick method for showing landing(use activity context for this method): <br />

```ruby
resana.onNativeAdClicked(Context context, NativeAd ad)
resana.onNativeAdClicked(Context context, NativeAd ad, AdDelegate adDelegate)
```

<br />
