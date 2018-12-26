مستندات پیش رو به منظور استفاده از سرویس رسانا در اپلیکیشن های اندروید تهیه شده است.</span>
<span style="font-weight: 400;">در فایل build.gradle کل پروژه تغییرات زیر را اعمال کنید.</span></p>

<pre class="show-plain-default:true scroll:true minimize:true expand:true lang:java decode:true ">allprojects {
          repositories {
          ...
          google()
          maven {
          url "https://maven.oddrun.ir/artifactory/resana"
     }
    }
}</pre>
<p style="text-align: right;"><span style="font-weight: 400;">کد زیر را به فایل build.gradle اضافه کنید تا رسانا به پروژه اضافه شود.</span></p>

<pre class="show-plain-default:true lang:default decode:true ">  implementation ('io.resana:resana:7.4.6@aar') {transitive = true}</pre>
<p style="text-align: right;">کد های زیر را به فایل proguard پروژه اضافه کنید.</p>

<pre class="lang:java decode:true">-keep class io.resana.**{
*;
}</pre>
<p style="text-align: right;"></p>
<p style="text-align: right;"><span style="font-weight: 400;">کد های زیر را به فایل manifest پروژه اضافه کنید.</span></p>

<pre class="lang:java decode:true">&lt;permission
        android:name="PACKAGE NAME.permission.RESANA_ADS"
        android:protectionLevel="signature" /&gt;
    &lt;uses-permission android:name="PACKAGE NAME..permission.RESANA_ADS" /&gt;

&lt;application
 ...
 &lt;meta-data
            android:name="ResanaMediaId"
            android:value="YOUR RESANA MEDIA ID" /&gt;
 ...
&lt;/application&gt;</pre>
<blockquote>نکته : چناچه در حال پیاده سازی SDK برای ارسال نسخه بتا (آزمایشی) می‌باشید در قسمت YOUR RESANA MEDIA ID از آیدی 10008 می‌توانید استفاده کنید.</blockquote>
<p style="text-align: right;">در ابتدای برنامه تابع Resana.init را به این شکل با تنظیمات رسانا به فراخانی کنید. با این کار رسانا شروع به کار و دریافت تبلیغات نمیکند و فقط تنظیمات را ذخیره میکند.</p>
<p style="text-align: right;">resanaConfig تنظیمات رسانا است.</p>

<pre class="lang:java decode:true">ResanaConfig resanaConfig = new ResanaConfig(ResanaConfig.AdType[], ResanaConfig.VisualType[]);

Resana.init(context, resanaConfig);</pre>
<p style="text-align: right;">برای مثال برای تبلیغات همسان با تصویر اصلی تکه کد زیر استفاده می‌شود:</p>

<pre class="lang:java decode:true ">ResanaConfig resanaConfig = new ResanaConfig(new ResanaConfig.AdType[]{ResanaConfig.AdType.NATIVE}, new ResanaConfig.VisualType[]{ResanaConfig.VisualType.ORIGINAL});</pre>
<p style="text-align: right;"></p>
<p style="text-align: right;">به کلاس ResanaConfig یک آرایه از نوع تبلیغ هایی که میخواهید و همچنین یک آرایه از نوع المان های تصویری ای که میخواهید نمایش دهید پاس دهید.</p>
<p style="text-align: right;">نوع تبلیغ ها:</p>

<blockquote>ResanaConfig.AdType.<strong>NATIVE :   </strong>تبلیغ های همسان

ResanaConfig.AdType.<strong>SPLASH :</strong>   تبلیغ های اسپلش</blockquote>
<p style="text-align: right;">نوع المان های تصویری:</p>

<blockquote>ResanaConfig.VisualType.<strong>SQUARE :‌   </strong>المان تصویری در ابعاد مربع

ResanaConfig.VisualType.<strong>HORIZONTAL :   </strong>المان تصویری مستطیلی افقی

ResanaConfig.VisualType.<strong>OROGINAL :‌  </strong>المان تصویری اصلی</blockquote>
<p style="text-align: right;">در هر صفحه از برنامه که میخواهید تبلیغ نشان دهید با تابع Resana.create یک instance از رسانا ایجاد کنید.</p>
<p style="text-align: right;">آرگومان این تابع application context است.</p>
<p style="text-align: right;"><span style="font-weight: 400;">دومین آرگومان تابع create مشخص می‌کند که رسانا کدام یک از لاگ‌های برنامه را بنویسد.</span></p>

<blockquote><span style="font-weight: 400;"><strong>LOG_LEVEL_VERBOSE</strong>       برای لاگ کردن همه چیز </span>

<span style="font-weight: 400;"><strong>LOG_LEVEL_DEBUG</strong>  برای لاگ کردن چیز های ضروری       </span>
<p style="text-align: right;"><strong>warning) LOG_LEVEL_DEBUG)</strong> برای لاگ کردن فقط ارورها و هشدارها</p>
<span style="font-weight: 400;"><strong>LOG_LEVEL_ERROR</strong>       برای لاگ کردن فقط ارورها   </span>

<span style="font-weight: 400;"><strong>LOG_LEVEL_NO_LOG</strong>        رسانا چیزی لاگ نمی‌کند</span></blockquote>
<pre class="lang:java decode:true ">Resana resana;
resana = Resana.create(context);</pre>
<p style="text-align: right;"></p>
<p style="text-align: right;"><span style="font-weight: 400;">در پایان هر بخش برنامه برای غیر فعال کردن رسانا باید</span></p>

<pre class="lang:java decode:true">resana.release()</pre>
<p style="text-align: right;"><span style="font-weight: 400;">را صدا کنید. پس از این کار دیگر امکان استفاده از رسانا وجود ندارد.</span></p>

<h3 style="text-align: right;">تبلیغات همسان (Native Ad) در رسانا:</h3>
<p style="text-align: right;"><span style="font-weight: 400;">نمایش این تبلیغات بر عهده‌ی اپلیکیشن است. نمایش لندیگ یا نصب اپلیکیشن‌های تبلیغ توسط SDK رسانا مدیریت می‌شود.</span></p>
<p style="text-align: right;"><span style="font-weight: 400;">به وسیله قطعه کد زیر می‌توان یک تبلیغ همسان برای نمایش دریافت کرد.</span></p>

<pre class="lang:java decode:true">NativeAd ad = resana.getNativeAd(boolean hasTitle);
NativeAd ad = resana.getNativeAd(boolean hasTitle, String zone);</pre>
<p style="text-align: right;"><span style="font-weight: 400;">تبلیغات بومی به دو دسته‌ی کلی تقسیم می‌شوند. تبلیغاتی که عنوان دارند و تبلیغاتی که عنوان ندارند.</span></p>

<pre class="lang:java decode:true">Resana.getNativeAd(boolean hasTitle, String zone)</pre>
<p style="text-align: right;"><span style="font-weight: 400;">به وسیله‌ی آرگومان اول میتوان تعیین کرد که تبلیغ دریافتی دارای تایتل باشد یا خیر. </span></p>
<p style="text-align: right;"><span style="font-weight: 400;">به وسیله ی آرگومان دوم میتوان تعیین کرد که این تبلیغ برای کدام محل (zone) مورد نیاز است.</span></p>
<p style="text-align: right;"><span style="font-weight: 400;">بسته به شرایط ad میتواند یک تبلیغ باشد یا میتواند null باشد. اگر null نبود میتوان با استفاده از متد های آن دیتا ها را گرفت و تبلیغ را نمایش داد.</span></p>

<h4 style="text-align: right;"><strong>توابع NativeAd :</strong></h4>
<p style="text-align: right;"><span style="font-weight: 400;">متد زیر عبارتی را برمیگرداند که باید روی تبلیغ نوشته شود تا روی آن کلیک شود.اگر تبلیغ این عبارت را نداشته باشد null برمیگرداند.</span></p>

<pre class="lang:java decode:true ">String NativeAd.getCallForAction()</pre>
<p style="text-align: right;"><span style="font-weight: 400;">متد ز یر رنگ پس زمینه ی تبلیغ را به صورت مبنای ۱۶ برمیگرداند.</span></p>

<pre class="lang:default decode:true">String NativeAd.getBackgroundColor()
</pre>
<p style="text-align: right;"></p>
<p style="text-align: right;"><span style="font-weight: 400;">متن هایی که در تبلیغ باید نمایش داده شوند به دو دسته ی عنوان و متن معمولی دسته بندی میشوند. هر کدام از این متن ها در دو نسخه ی کوتاه و متوسط ارایه میشوند.</span></p>
<p style="text-align: right;"><span style="font-weight: 400;">تمامی تبلیغ ها متن معمولی دارند. در حالی که ممکن است بعضی تبلیغ ها عنوان نداشته باشند.</span></p>
<p style="text-align: right;"><span style="font-weight: 400;">متد زیر نسخه ی کوتاه متن معمولی تبلیغ را برمیگرداند.</span></p>

<pre class="lang:default decode:true">String NativeAd.getShortOrdinaryText()
</pre>
<p style="text-align: right;"><span style="font-weight: 400;">متد زیر نسخه ی متوسط متن معمولی تبلیغ را برمیگرداند.</span></p>

<pre class="lang:default decode:true">String NativeAd.getMediumOrdinaryText()
</pre>
<p style="text-align: right;"><span style="font-weight: 400;">متد زیر نسخه ی کوتاه متن عنوان تبلیغ را برمیگرداند. اگر تبلیغ عنوان نداشت null برمیگرداند.</span></p>

<pre class="lang:default decode:true">String NativeAd.getShortTitleText()
</pre>
<p style="text-align: right;"><span style="font-weight: 400;">متد زیر نسخه ی متوسط متن عنوان تبلیغ را برمیگرداند. اگر تبلیغ عنوان نداشت null برمیگرداند.</span></p>

<pre class="lang:default decode:true">String NativeAd.getMediumTitleText()
</pre>
<p style="text-align: right;">هر تبلیغ چندین بسته المان تصویری دارد.</p>
<p style="text-align: right;"><span style="font-weight: 400;">هر المان تصویری تبلیغ(Visual) موارد زیر را شامل می‌شود.</span></p>

<blockquote><span style="font-weight: 400;"><strong>orgVisual</strong>  : المان تصویر اصلی</span>

<span style="font-weight: 400;"><strong>sqVisual</strong>: المان تصویری در ابعاد مربع</span>

<span style="font-weight: 400;"><strong>hrzVisual</strong>: المان تصویری مستطیلی افقی</span></blockquote>
<p style="text-align: right;">هر کدام از المان های تصویری تبلیغ متد های زیر را دارد.</p>

<blockquote><span style="font-weight: 400;"><strong>()getFile</strong>  : گرفتن فایل عکس یا فیلم</span>

<strong>()getType</strong> : گرفتن نوع یک المان که میتواند عکس یا فیلم باشد

<strong>()getHeight</strong> : گرفتن ارتفاع المان (اگر المان عکس باشد)

<strong>()getWidth</strong> : گرفتن عرض المان (اگر المان عکس باشد)

&nbsp;</blockquote>
<pre class="lang:default decode:true">int type = ad.getVisual().getSqVisual().getType()

File file = ad.getVisual().getHrzVisual().getFile()

int type = ad.getVisual().getHrzVisual().getType()

int height = ad.getVisual().getHrzVisual().getHeight()

int width = ad.getVisual().getHrzVisual().getWidth()</pre>
<p style="text-align: right;">پیشنهاد میشود برای نمایش عکس های تبلیغ از کتابخانه های معروفی مثل Glide یا Picasso استفاده کنید.</p>

<pre class="lang:java decode:true">File file = ad.getVisual().getHrzVisual().getFile()
Picasso.get().load(file).into(adImage);</pre>
<p style="text-align: right;"></p>


<hr />
<p style="text-align: right;"></p>
<p style="text-align: right;"><span style="font-weight: 400;">پس از آن که تبلیغ با استفاده از المان های تصویری اش به کاربر نمایش داده شد باید با متد زیر به SDK اطلاع داده شود.</span></p>

<pre class="lang:default decode:true">resana.onNativeAdRendered(NativeAd ad)
</pre>
<p style="text-align: right;"><span style="font-weight: 400;">وقتی یک تبلیغ کلیک شد باید حتما در متد onClick متد زیر از SDK صدا زده شود تا تصویر لندینگ باز شود.</span></p>

<pre class="lang:default decode:true">resana.onNativeAdClicked(Context context, NativeAd ad)

resana.onNativeAdClicked(Context context, NativeAd ad, AdDelegate adDelegate)</pre>
<p style="text-align: right;"><span style="font-weight: 400;">به این تابع Activity Context پاس دهید.</span></p>
<p style="text-align: right;">NativeAd ad همان تبلیغی است که توسط کاربر دیده شده است.</p>
<p style="text-align: right;">اگر تبلیغ فایل نصبی داشته باشد هنگام دانلود و نصب برنامه اینترفیس AdDelegate صدا زده میشود.</p>

<pre class="lang:java decode:true ">public interface AdDelegate {
    void onPreparingProgram();

    void onPreparingProgramError();

    void onInstallingProgramError();
}</pre>
<blockquote><strong>()onPreparingProgram</strong> زمانی صدا زده میشود که برنامه در حال دانلود و آماده سازی است.

<strong>()onPreparingProgramError</strong> زمانی صدا زده می شود که مشکلی در دانلود یا آماده سازی برنامه به وجود آمده باشد. این مشکل میتواند ناشی از کندی یا قطع اینترنت یا نداشتن مجوز نوشتن روی  دیسک باشد.

<strong>()onInstallingProgramError</strong> زمانی صدا زده می شود که مشکلی در نصب برنامه به وجود آمده باشد. این مشکل میتواند ناشی از نداشتن مجوز نوشتن روی دیسک باشد.</blockquote>
<p style="text-align: right;">با استفاده از این اینترفیس میتوانید هنگامی که هر کدام از وقایع بالا اتفاق افتاد، خودتان بسته به سلیقه خودتان کاربرتان را مطلع کنید. اگر تابع onNativeAdClicked را بدون این اینترفیس فراخوانی کنید، زمانی که SDK در حال دانلود و آماده سازی برنامه است یا زمانی که مشکلی در آماده سازی برنامه به وجود می آید یک تست (toast) به کاربر نشان میدهد.</p>
<p style="text-align: right;"><span style="font-weight: 400;">یک نمونه کد کامل: </span></p>

<pre class="lang:default decode:true ">public class MainActivity extends AppCompatActivity implements View.OnClickListener {

       Resana resana;
       @Override
       protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);

       //initializing resana
       resana = Resana.create(getApplicationContext(),  Resana.LOG_LEVEL_VERBOSE);

      }

       void getAd() {
              NativeAd ad = resana.getNativeAd(false, "A");
              if (ad != null) { //This is a valid ad
              showAd(); //should be implemented by app developer
              resana.onNativeAdRendered(ad);
      } 
}</pre>
<p style="text-align: right;"></p>

