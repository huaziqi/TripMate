declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// 微信小程序原生 API（enableChunked 流式请求等 uni 类型未覆盖的能力）
declare const wx: any

declare module 'threejs-miniprogram'
