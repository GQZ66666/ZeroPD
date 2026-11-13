# 电池UI个性化（BatteryZeroFix）

状态栏时钟的 `0` 是“跑道 0”（左右竖直、上下圆弧收口），电池百分比却是圆弧 0。
原因是电池百分比用了另一套 typeface，不是时钟的字体。

本模块在 SystemUI 里：

1. 监听 `HwStatusBarClock.onAttachedToWindow`，记录时钟当前的 `Typeface` 和
   `fontFeatureSettings`；
2. 监听 `HwStatusBarBatteryView.onAttachedToWindow / onBatteryUpdate`，把同一套
   typeface 应用到 `mInsidePercentView` / `mOutsidePercentView`；
3. 按设置调整：
   - 图标内：字号、字重、上下位置；
   - 图标外：左侧/右侧、字号、字重、上下位置；
4. 内显的上下位置通过 `CenteredTextView.onDraw` 平移画布实现（`translationY`
   会被垂直居中逻辑吃掉），外显仍用 `translationY`。

## 使用

安装后在 LSPosed 启用，作用域勾选「系统界面」，打开 App 调整后点
「保存并应用」→「重启系统界面」。

## 说明

- 跑道 0 是字体字形，不是 OpenType 的斜杠零特性，所以模块复制的是 typeface；
- 外显左右通过调整 `mOutsidePercentView` 在父容器中的顺序实现；
- 字重范围 300–900，步进 50；字号范围 50%–150%。
