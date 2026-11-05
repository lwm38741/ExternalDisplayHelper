# 构建单 APK

使用 Python 3、JDK 21、Android SDK Platform 36、Build Tools 36.0.0 与 libxposed API 101.0.1。现有脚本适用 macOS 工具目录布局：

    TOOLS/jdk21/<jdk>/Contents/Home/
    TOOLS/android-sdk/platforms/android-36/android.jar
    TOOLS/android-sdk/build-tools/36.0.0/
    TOOLS/libxposed-api-101.0.1/classes.jar
    TOOLS/debug.keystore

本地调试（自备标准 Android debug keystore）：

    python3 scripts/build.py --tools /path/to/TOOLS --out dist/helper-local.apk

正式构建额外传入 --keystore、--password-file、--lineage，对应本地私钥、单行密码文件和升级签名链。不要提交任何签名文件。没有原签名不能覆盖已发布的 APK，也不应为测试随意卸载正在使用的版本。

构建脚本将应用和系统框架入口编入同一 APK。libxposed 仅用作编译依赖；运行时由模块框架提供。输出文件已存在时脚本拒绝覆盖。使用 JDK 21 的 java 和 javac 运行逻辑测试：python3 scripts/test.py。真机验收另需覆盖安装/全新安装、框架重启加载、外屏切换、触控板、键鼠、内屏黑屏和拔线恢复。
