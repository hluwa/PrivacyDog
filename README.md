# PrivacyDog

基于 Soot 框架的 Android 隐私问题静态代码扫描器

## 使用方法

```
PrivacyDog 
    -t <APK/AAR/JAR/DEX> 输入文件/目录
    -r [可选] 规则文件地址, 默认使用内置规则
    -o [可选] JSON 输出目录, 默认仅输出 stdout
```

## 扫描规则

可参考 [privacydog.json](src/main/resources/privacydog.json)

除了 `stringPattern` 是对所有指令进行匹配，其他均为对函数调用的匹配。

支持对调用参数具体值的匹配，比如匹配第一个参数为 `android_id` 的 `Settings.Secure.getString`：

```
{
    "className": "android.provider.Settings$Secure",
    "methodName": "getString",
    "arguments": {
      "1": "android_id"
    }
}
```

采用的是成本较低的过程内分析。
