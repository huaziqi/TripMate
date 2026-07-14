Live2D 参数控制说明
1. 模型参数入口
当前项目中，Live2D 模型参数通过以下路径访问：

model.internalModel.coreModel
若该路径不存在，则尝试：

model.coreModel
封装中已经自动处理，无需业务层手动访问。

2. 当前可用的参数方法
经验证，当前模型运行时支持以下接口：

getParameterIndex
setParameterValueByIndex
addParameterValueByIndex
getParameterValueByIndex
setParameterValueById
addParameterValueById
getParameterValueById
因此可以通过参数 ID 或参数索引进行读写。

3. 已封装的组件方法
单参数操作
updateModelParam(paramId, value, clamp = true)
addModelParam(paramId, delta, clamp = true)
getModelParam(paramId)
getModelParamInfo(paramId)
resetModelParam(paramId)
批量操作
updateModelParams(params, clamp = true)
addModelParams(params, clamp = true)
resetModelParams(paramIds)
调试方法
inspectModel()
listModelParameters()
辅助动作
lookTo(x, y)
speakOnce(level = 1, duration = 300)
4. 常用参数示例
头部转向
ParamAngleX
ParamAngleY
ParamAngleZ
身体转向
ParamBodyAngleX
眼球方向
ParamEyeBallX
ParamEyeBallY
嘴巴开合
ParamMouthOpenY
呼吸/情绪类
不同模型可能存在：

ParamBreath
ParamBrowLY
ParamBrowRY
ParamEyeLOpen
ParamEyeROpen
5. 示例：直接设置参数
live2dRef.value.updateModelParam('ParamAngleX', 25)
live2dRef.value.updateModelParam('ParamMouthOpenY', 1)
6. 示例：批量设置
live2dRef.value.updateModelParams({
  ParamAngleX: 20,
  ParamEyeBallX: 1,
  ParamMouthOpenY: 0.8
})
7. 示例：读取参数信息
const info = live2dRef.value.getModelParamInfo('ParamAngleX')
console.log(info)
返回示例：

{
  index: 0,
  id: 'ParamAngleX',
  value: 12,
  defaultValue: 0,
  minValue: -30,
  maxValue: 30
}
8. 示例：列出模型全部参数
const params = live2dRef.value.listModelParameters()
console.log(params)
该方法可用于调试模型具体支持哪些参数。

9. 示例：看向某方向
live2dRef.value.lookTo(1, 0)   // 向右
live2dRef.value.lookTo(-1, 0)  // 向左
live2dRef.value.lookTo(0, -1)  // 向上
live2dRef.value.lookTo(0, 1)   // 向下
10. 示例：短暂张嘴
live2dRef.value.speakOnce(1, 300)
表示嘴巴张开后持续 300ms，再自动恢复默认值。

11. 注意事项
1）不同模型参数范围不同
虽然常见模型中：

ParamAngleX: 通常约 -30 ~ 30
ParamEyeBallX: 通常约 -1 ~ 1
ParamMouthOpenY: 通常约 0 ~ 1
但不同模型可能存在差异，建议优先通过：

getModelParamInfo(paramId)
确认范围。

2）建议默认启用 clamp
当前封装默认会根据参数最小值、最大值自动裁剪，避免写入异常值。

3）模型支持哪些参数取决于资源本身
如果某参数 ID 在当前模型中不存在，则设置不会生效。

可用以下方式确认：

getModelParamInfo(paramId)
listModelParameters()
4）当前模型中，参数设置后不会立即回弹
说明当前模型可直接使用参数驱动，不一定需要额外做每帧强制覆盖。

但若后续接入新模型，仍需关注 motion / breath / physics 是否会覆盖业务层参数。


点击发送
角色随机轻微转头
眼球跟随
嘴巴开合
300ms 后恢复



Live2D 组件方法说明文档
组件对外通过 ref 调用，示例：

<live2d-view ref="live2dRef" ... />
live2dRef.value.xxx()
1. setModelPosition(position)
设置模型位置。

live2dRef.value.setModelPosition({
  x: 0,
  y: 0
})
参数：

position: { x?: number, y?: number }
2. setModelScale(scaleBase)
设置模型缩放。

live2dRef.value.setModelScale(1.1)
参数：

scaleBase: number
3. updateModelParam(paramId, value, clamp = true)
直接设置单个模型参数。

live2dRef.value.updateModelParam('ParamAngleX', 10)
参数：

paramId: string 参数名
value: number 目标值
clamp?: boolean 是否限制在模型参数范围内
返回：

boolean
4. updateModelParams(params, clamp = true)
批量设置多个参数。

live2dRef.value.updateModelParams({
  ParamAngleX: 8,
  ParamAngleY: -4
})
参数：

params: Record<string, number>
clamp?: boolean
返回：

boolean
5. addModelParam(paramId, delta, clamp = true)
给单个参数叠加增量。

live2dRef.value.addModelParam('ParamAngleX', 2)
6. addModelParams(params, clamp = true)
批量叠加多个参数。

live2dRef.value.addModelParams({
  ParamAngleX: 2,
  ParamBodyAngleX: 1
})
7. getModelParam(paramId)
获取参数当前值。

const value = live2dRef.value.getModelParam('ParamMouthOpenY')
返回：

number | null
8. getModelParamInfo(paramId)
获取参数详细信息。

const info = live2dRef.value.getModelParamInfo('ParamMouthForm')
返回示例：

{
  id: 'ParamMouthForm',
  value: 0,
  defaultValue: 0,
  minValue: -1,
  maxValue: 1
}
9. listModelParameters()
列出模型所有参数。

const params = live2dRef.value.listModelParameters()
返回：

Array
10. resetModelParam(paramId)
重置单个参数到默认值。

live2dRef.value.resetModelParam('ParamAngleX')
11. resetModelParams(paramIds)
批量重置多个参数。

live2dRef.value.resetModelParams([
  'ParamAngleX',
  'ParamAngleY',
  'ParamMouthOpenY'
])
12. lookTo(x, y)
让模型朝某方向看。

live2dRef.value.lookTo(0.3, -0.2)
参数：

x: number
y: number
13. speakOnce(level = 1, duration = 300)
执行一次简单说话动作。

live2dRef.value.speakOnce(1, 300)
参数：

level?: number 开合强度
duration?: number 持续时间
适合短促“嗯”“啊”这类瞬时动作。

14. animateModelParam(paramId, toValue, options = {})
单参数动画。

await live2dRef.value.animateModelParam('ParamAngleX', 15, {
  duration: 300,
  easing: 'easeOutQuad'
})
返回：

Promise<boolean>
15. animateModelParams(params, options = {})
多参数动画。

await live2dRef.value.animateModelParams({
  ParamAngleX: 10,
  ParamAngleY: 5
}, {
  duration: 400
})
返回：

Promise<any[]>
16. stopModelParamAnimation(paramId)
停止某个参数动画。

live2dRef.value.stopModelParamAnimation('ParamAngleX')
17. stopAllModelAnimations()
停止全部参数动画。

live2dRef.value.stopAllModelAnimations()
18. blinkOnce(duration = 160)
执行一次眨眼。

await live2dRef.value.blinkOnce(160)
返回：

Promise<boolean>
19. playSendAction()
播放“发送动作”。

await live2dRef.value.playSendAction()
返回：

Promise<boolean>
适合在用户发送消息时触发。

20. startTalking(duration = 2000, options = {})
开始持续说话动画。

live2dRef.value.startTalking(2500, {
  enableBlink: true,
  enableHeadMotion: true,
  mouthOpenBase: 0.08,
  mouthOpenRange: 0.42,
  mouthFormBase: 0.2,
  mouthFormRange: 0.06
})
参数：

duration?: number 持续时长，单位 ms
options?: object
建议支持的 options：

enableBlink?: boolean 是否允许说话时自动眨眼
enableHeadMotion?: boolean 是否允许说话时轻微摇头/点头
mouthOpenBase?: number 嘴巴开合基础值
mouthOpenRange?: number 嘴巴开合波动范围
mouthFormBase?: number 嘴型基础表情值
mouthFormRange?: number 嘴型波动范围
interval?: number 更新间隔 ms
random?: boolean 是否随机波动
说明：

适合语音播放、长文本输出时使用
推荐与音频时长同步
21. stopTalking()
停止说话动画。

live2dRef.value.stopTalking()
适用于：

音频播放结束
用户手动暂停播放
页面卸载
22. inspectModel()
输出模型调试信息。

live2dRef.value.inspectModel()
适合开发期查看：

参数列表
参数范围

当前模型支持的控制项




后续优化方向，startTalking函数嘴型基准值设定，眨眼不显示修复，后端流式传输报错，音画不同步