数字人功能说明文档
1. 功能简介
项目已接入微信小程序 Live2D 数字人组件，可用于在页面中展示数字人助手。

当前支持：

页面中引入数字人组件
加载 Live2D 模型
控制模型缩放和位置
监听初始化成功 / 失败日志
与输入框、对话框等页面 UI 组合使用
2. 相关文件结构
wxcomponents/
├─ libs/                    # Live2D / Pixi 运行依赖
├─ live2d-view/             # 数字人组件
│  ├─ live2d-view.js
│  ├─ live2d-view.json
│  ├─ live2d-view.wxml
│  └─ live2d-view.wxss
└─ utils/
   ├─ live2d-config.js      # 默认配置
   └─ live2d-manager.js     # 画布、模型初始化与渲染逻辑
页面示例：

pages/live2d/live2d.vue
页面注册位置：

pages.json
3. 基本使用方法
3.1 页面注册组件
在 pages.json 中为页面配置组件：

{
  "path": "pages/live2d/live2d",
  "style": {
    "navigationBarTitleText": "数字人助手",
    "usingComponents": {
      "live2d-view": "/wxcomponents/live2d-view/live2d-view"
    }
  }
}
3.2 页面中使用
<live2d-view
  className="live2d-box"
  :autoInit="true"
  :stageWidth="750"
  @ready="handleReady"
  @error="handleError"
/>
3.3 日志回调
function handleReady(e: any) {
  console.log('[live2d-page] component ready:', e)
}

function handleError(e: any) {
  console.error('[live2d-page] component error:', e)
}
4. 画布说明
数字人组件内部会创建 canvas 并在其上渲染模型。

常用参数：

:stageWidth="750"
作用：

用来设置舞台的逻辑宽度
影响模型位置和缩放计算基准
一般页面中保持一个固定值即可，常用 750。

5. 模型缩放和位置说明
当前模型主要通过这几个参数控制：

scaleBase：控制模型大小
xRatio：控制模型左右位置
yRatio：控制模型上下位置
anchorX：模型水平锚点
anchorY：模型垂直锚点
例如：

{
  scaleBase: 0.17,
  xRatio: 0.5,
  yRatio: 0.88,
  anchorX: 0.5,
  anchorY: 1
}
含义：

scaleBase 越大，模型越大
xRatio 越大，模型越靠右
yRatio 越大，模型越靠下
anchorX: 0.5 表示以模型水平中心定位
anchorY: 1 表示以模型底部定位
6. 不同页面使用不同位置和缩放
不建议直接改 live2d-config.js
因为它更适合作为默认配置。
如果多个页面都共用它，那么改一个页面时会影响其他页面。

推荐做法
把每个页面自己的位置和缩放参数，通过组件属性传入，而不是直接写死在 config.js。

6.1 推荐组件调用方式
例如页面 A：

<live2d-view
  className="live2d-box"
  :autoInit="true"
  :stageWidth="750"
  :scaleBase="0.17"
  :xRatio="0.5"
  :yRatio="0.88"
  :anchorX="0.5"
  :anchorY="1"
  @ready="handleReady"
  @error="handleError"
/>
页面 B：

<live2d-view
  className="live2d-box"
  :autoInit="true"
  :stageWidth="750"
  :scaleBase="0.22"
  :xRatio="0.45"
  :yRatio="0.9"
  :anchorX="0.5"
  :anchorY="1"
  @ready="handleReady"
  @error="handleError"
/>
这样不同页面可以独立控制模型显示效果。

6.2 配置优先级建议
建议按以下优先级处理：

页面传入参数
live2d-config.js 默认参数
也就是：

页面传了，就用页面的
页面没传，再用默认配置
6.3 manager 层处理思路
在 live2d-manager.js 中读取参数时，建议写成这种形式：

const scaleBase = options.scaleBase ?? config.model.scaleBase;
const xRatio = options.xRatio ?? config.model.xRatio;
const yRatio = options.yRatio ?? config.model.yRatio;
const anchorX = options.anchorX ?? config.model.anchorX;
const anchorY = options.anchorY ?? config.model.anchorY;
这样就能同时支持：

默认配置统一管理
页面按需覆盖
7. 页面样式说明
组件外层样式也会影响模型最终显示效果。
当前实践中，推荐通过外层容器控制展示区域，例如：

.live2d-wrap {
  width: 80%;
  height: 1000rpx;
  margin: 0 auto;
}
作用：

控制数字人组件显示宽高
margin: 0 auto 可让组件保持水平居中
8. 推荐使用方式总结
默认配置放在 live2d-config.js
适合放：

模型地址
通用默认缩放
通用默认位置
通用舞台宽度
页面个性化配置放在页面组件调用处
适合放：

当前页面专用的 scaleBase
当前页面专用的 xRatio / yRatio
当前页面专用的锚点参数
9. 简单示例
<template>
  <view class="live2d-wrap">
    <live2d-view
      className="live2d-box"
      :autoInit="true"
      :stageWidth="750"
      :scaleBase="0.18"
      :xRatio="0.5"
      :yRatio="0.86"
      :anchorX="0.5"
      :anchorY="1"
      @ready="handleReady"
      @error="handleError"
    />
  </view>
</template>
function handleReady(e: any) {
  console.log('ready:', e)
}

function handleError(e: any) {
  console.error('error:', e)
}
10. 建议
live2d-config.js 作为默认配置保留
页面差异化需求通过组件参数传递
页面只负责“想要什么效果”
manager 负责“有参数用参数，没有参数用默认值”