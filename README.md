# 电池斜杠零（BatteryZeroFix）

状态栏时钟的 `0` 是斜杠零，电池百分比却不是。原因不是字体不同，而是电池百分比的
`CenteredTextView` 只设置了 `fontFeatureSettings="pnum"`，没有启用斜杠零特性。

本模块在 SystemUI 里：

1. 监听 `HwStatusBarClock.onAttachedToWindow`，记录时钟当前的
   `fontFeatureSettings` 和 `typeface`；
2. 监听 `HwStatusBarBatteryView.onAttachedToWindow / onBatteryUpdate`，
   把同一套特性（没有 `zero` 就补上 `,zero`）和 typeface 应用到
   `mInsidePercentView` / `mOutsidePercentView`。

## 使用

安装后在 LSPosed 启用，作用域勾选「系统界面」，重启系统界面。

## 说明

- 电池百分比不需要 `tnum`（等宽），`pnum` 保留即可；
- 如果字体把斜杠零放在 `ss01` 等 stylistic set 里，而不是 `zero`，把
  `ZeroHook` 里的 `",zero"` 换成 `",ss01"` 再构建即可。
