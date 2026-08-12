# Room and WorkManager ship consumer rules.

# jakarta.mail (Angus Mail) 的 SMTP transport provider 通过
# META-INF/javamail*.providers 资源文件以「类名字符串」反射加载（Class.forName）。
# R8 混淆会重命名这些类，导致运行期 NoSuchProviderException("smtp")、SMTP 发送失败，
# 因此必须保留全部相关类及其成员。
-keep class org.eclipse.angus.mail.** { *; }
-keep class jakarta.mail.** { *; }
-dontwarn org.eclipse.angus.**
-dontwarn jakarta.mail.**
-dontwarn jakarta.activation.**
