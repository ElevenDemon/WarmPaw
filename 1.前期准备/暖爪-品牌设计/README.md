# 暖爪 · Logo 设计

设计方向：**爪印里的家**。圆润的四趾爪印代表宠物，掌心的负形小屋代表在家被照顾。沿用产品现有的品牌橙，让官网、小程序和运营后台保持一致。

品牌文案：**你不在家，爱也在。**

## 文件使用

打开 `index.html` 查看完整展示与下载入口，`暖爪-Logo设计展示.png` 可直接用于方案评审。

| 文件（位于 assets） | 用途 |
| --- | --- |
| logo-horizontal.svg / .png | 横版主标志，适合官网、页眉及宣传物料；PNG 1704 × 768，透明底 |
| logo-stacked.svg / .png | 竖版组合，适合封面、竖向版面；PNG 720 × 860，透明底 |
| logo-mark.svg / .png | 独立图形；PNG 1024 × 1024，透明底 |
| logo-mark-micro.svg | 16–24 px 简化图形，移除小屋门洞以减少细节粘连 |
| logo-horizontal-mono.svg / logo-mark-mono.svg | 深墨单色，用于单色印刷和浅底页面 |
| logo-horizontal-reverse.svg / .png | 全白横版，适合深色背景；PNG 1704 × 768，透明底 |
| logo-mark-reverse.svg | 全白独立图形，适合深色背景 |
| app-icon.svg / app-icon-1024.png / app-icon-512.png | 橙底白标正方形应用图标；平台上传优先使用没有预切圆角的 PNG |
| app-icon-rounded.svg | 透明外角的圆角展示版，用于应用展示与宣传 |
| favicon-32.png / favicon-16.png | 网页小尺寸图标 |

## 色彩与规范

| 角色 | 色值 |
| --- | --- |
| 品牌主色 · 暖爪橙 | `#E8622D` |
| 文字与单色标志 · 深墨色 | `#292722` |
| 品牌展示底色 · 奶油白 | `#FFF8F0` |

- 标志周围建议保留至少图形画布宽度 1/8 的净空。
- 横版组合建议不小于 180 px 宽；更小的空间使用独立图形。
- 图形常规版本建议不小于 32 px；16–24 px 使用简化版本。
- 保持原始宽高比，不拉伸、不旋转、不添加描边或阴影到标志本身。
- 小屋是透明负形，会露出使用场景的背景色；复杂图片背景应先铺纯色底。
- 深色底使用反白版本；正式标志保持纯色，不使用渐变。

## 源文件与复现

SVG 是可编辑的矢量母版，图形由椭圆和贝塞尔路径构成。中文字标基于 Microsoft YaHei Bold，英文基于 Arial Bold；交付 SVG 中已转为路径，显示不依赖接收方安装字体。字体不是定制字形，未随包分发字体文件。

`scripts/type-outlines.json` 保存字形路径；`scripts/outline-type.ps1` 可在安装相应字体的 Windows 环境重新生成路径。`scripts/build.mjs` 生成 SVG 与 HTML，`scripts/render.mjs` 通过本机 Edge 导出 PNG。执行顺序：

```powershell
node scripts/build.mjs
node scripts/render.mjs
```

该资源包是独立设计交付，包含预览、母版、位图及生成脚本。
