# 电池UI个性化（BatteryZeroFix）

状态栏时钟的 `0` 是“跑道 0”（左右竖直、上下圆弧收口），电池百分比却是圆弧 0。
原因是电池百分比用了另一套 typeface，不是时钟的字体。

本模块在 SystemUI 里：

1. 监听 `HwStatusBarClock.onAttachedToWindow`，记录时钟当前的 `Typeface` 和
   `fontFeatureSettings`；
2. 监听 `HwStatusBarBatteryView.onAttachedToWindow / onBatteryUpdate`，把同一套
   typeface 应用到 `mInsidePercentView` / `mOutsidePercentView`；
3. 按设置调整：
   - 图标内百分比：字号（50%–150%）、上下位置（-10~+10 dp）；
   - 图标外百分比：显示在电池左侧/右侧、上下位置（-10~+10 dp）。

## 使用

安装后在 LSPosed 启用，作用域勾选「系统界面」，打开 App 调整后点
「保存并应用」→「重启系统界面」。

## 说明

- 跑道 0 是字体字形，不是 OpenType 的斜杠零特性，所以模块复制的是 typeface，
  不是 `fontFeatureSettings`；
- 外显左右是通过调整 `mOutsidePercentView` 在父容器中的顺序实现；
- 如果系统主题在之后重置电池 typeface，可在 `ZeroHook` 里改为拦截
  `TextView.setTypeface` 强制替换。
