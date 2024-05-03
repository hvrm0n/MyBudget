
Ã,
Y
ExampleInstrumentedTestcom.example.mybudgetaddSameBudget2Ú‘ì°À·¼Å:Ý‘ì°€¨—c—%
¸androidx.test.espresso.PerformException: Error performing 'androidx.test.espresso.contrib.RecyclerViewActions$ActionOnItemAtPositionViewAction@c8b3154' on view 'Animations or transitions are enabled on the target device.
For more info check: https://developer.android.com/training/testing/espresso/setup#set-up-environment

view.getId() is <2131296394/com.example.mybudget:id/budgetsList>'.
at androidx.test.espresso.PerformException$Builder.build(PerformException.java:1)
at androidx.test.espresso.base.PerformExceptionHandler.handleSafely(PerformExceptionHandler.java:8)
at androidx.test.espresso.base.PerformExceptionHandler.handleSafely(PerformExceptionHandler.java:9)
at androidx.test.espresso.base.DefaultFailureHandler$TypedFailureHandler.handle(DefaultFailureHandler.java:4)
at androidx.test.espresso.base.DefaultFailureHandler.handle(DefaultFailureHandler.java:5)
at androidx.test.espresso.ViewInteraction.waitForAndHandleInteractionResults(ViewInteraction.java:8)
at androidx.test.espresso.ViewInteraction.desugaredPerform(ViewInteraction.java:11)
at androidx.test.espresso.ViewInteraction.perform(ViewInteraction.java:8)
at com.example.mybudget.ExampleInstrumentedTest.addSameBudget(ExampleInstrumentedTest.kt:42)
... 33 trimmed
Caused by: java.lang.IllegalStateException: No view holder at position: 1
at androidx.test.espresso.contrib.RecyclerViewActions$ActionOnItemAtPositionViewAction.perform(RecyclerViewActions.java:299)
at androidx.test.espresso.ViewInteraction$SingleExecutionViewAction.perform(ViewInteraction.java:2)
at androidx.test.espresso.ViewInteraction.doPerform(ViewInteraction.java:25)
at androidx.test.espresso.ViewInteraction.-$$Nest$mdoPerform(Unknown Source:0)
at androidx.test.espresso.ViewInteraction$1.call(ViewInteraction.java:7)
at androidx.test.espresso.ViewInteraction$1.call(ViewInteraction.java:1)
at java.util.concurrent.FutureTask.run(FutureTask.java:264)
at android.os.Handler.handleCallback(Handler.java:958)
at android.os.Handler.dispatchMessage(Handler.java:99)
at android.os.Looper.loopOnce(Looper.java:205)
at android.os.Looper.loop(Looper.java:294)
at android.app.ActivityThread.main(ActivityThread.java:8177)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)java.lang.IllegalStateException¸androidx.test.espresso.PerformException: Error performing 'androidx.test.espresso.contrib.RecyclerViewActions$ActionOnItemAtPositionViewAction@c8b3154' on view 'Animations or transitions are enabled on the target device.
For more info check: https://developer.android.com/training/testing/espresso/setup#set-up-environment

view.getId() is <2131296394/com.example.mybudget:id/budgetsList>'.
at androidx.test.espresso.PerformException$Builder.build(PerformException.java:1)
at androidx.test.espresso.base.PerformExceptionHandler.handleSafely(PerformExceptionHandler.java:8)
at androidx.test.espresso.base.PerformExceptionHandler.handleSafely(PerformExceptionHandler.java:9)
at androidx.test.espresso.base.DefaultFailureHandler$TypedFailureHandler.handle(DefaultFailureHandler.java:4)
at androidx.test.espresso.base.DefaultFailureHandler.handle(DefaultFailureHandler.java:5)
at androidx.test.espresso.ViewInteraction.waitForAndHandleInteractionResults(ViewInteraction.java:8)
at androidx.test.espresso.ViewInteraction.desugaredPerform(ViewInteraction.java:11)
at androidx.test.espresso.ViewInteraction.perform(ViewInteraction.java:8)
at com.example.mybudget.ExampleInstrumentedTest.addSameBudget(ExampleInstrumentedTest.kt:42)
... 33 trimmed
Caused by: java.lang.IllegalStateException: No view holder at position: 1
at androidx.test.espresso.contrib.RecyclerViewActions$ActionOnItemAtPositionViewAction.perform(RecyclerViewActions.java:299)
at androidx.test.espresso.ViewInteraction$SingleExecutionViewAction.perform(ViewInteraction.java:2)
at androidx.test.espresso.ViewInteraction.doPerform(ViewInteraction.java:25)
at androidx.test.espresso.ViewInteraction.-$$Nest$mdoPerform(Unknown Source:0)
at androidx.test.espresso.ViewInteraction$1.call(ViewInteraction.java:7)
at androidx.test.espresso.ViewInteraction$1.call(ViewInteraction.java:1)
at java.util.concurrent.FutureTask.run(FutureTask.java:264)
at android.os.Handler.handleCallback(Handler.java:958)
at android.os.Handler.dispatchMessage(Handler.java:99)
at android.os.Looper.loopOnce(Looper.java:205)
at android.os.Looper.loop(Looper.java:294)
at android.app.ActivityThread.main(ActivityThread.java:8177)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)"õ

logcatandroidß
ÜC:\Users\julia\AndroidStudioProjects\MyBudget\app\build\outputs\androidTest-results\connected\debug\Pixel_3a_API_34_extension_level_7_x86_64(AVD) - 14\logcat-com.example.mybudget.ExampleInstrumentedTest-addSameBudget.txt"Ã

device-infoandroid¨
¥C:\Users\julia\AndroidStudioProjects\MyBudget\app\build\outputs\androidTest-results\connected\debug\Pixel_3a_API_34_extension_level_7_x86_64(AVD) - 14\device-info.pb"Ä

device-info.meminfoandroid¡
žC:\Users\julia\AndroidStudioProjects\MyBudget\app\build\outputs\androidTest-results\connected\debug\Pixel_3a_API_34_extension_level_7_x86_64(AVD) - 14\meminfo"Ä

device-info.cpuinfoandroid¡
žC:\Users\julia\AndroidStudioProjects\MyBudget\app\build\outputs\androidTest-results\connected\debug\Pixel_3a_API_34_extension_level_7_x86_64(AVD) - 14\cpuinfo*¨
c
test-results.logOcom.google.testing.platform.runtime.android.driver.AndroidInstrumentationDriver²
¯C:\Users\julia\AndroidStudioProjects\MyBudget\app\build\outputs\androidTest-results\connected\debug\Pixel_3a_API_34_extension_level_7_x86_64(AVD) - 14\testlog\test-results.log 2
text/plain