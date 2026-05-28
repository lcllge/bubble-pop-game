# Debug Session: celebration-crash-fix

**Status:** [OPEN]
**Bug:** 点击球球触发全屏庆祝特效时应用闪退
**Date:** 2026-05-28

## Symptoms
- 用户点击球球后，当触发全屏庆祝效果（礼花+文字）时应用闪退
- 之前尝试修复：移除emoji、替换setShadowLayer为描边方式
- 问题仍然存在

## Hypotheses
1. **H1: celebrationPaint/trailPaint 未正确初始化** - Paint对象可能在某些情况下为null或状态异常
2. **H2: fireworks列表并发修改** - 在drawCelebration中遍历fireworks时，其他地方可能同时修改列表
3. **H3: canvas状态异常** - drawCelebration中paint.style切换可能导致canvas状态不一致
4. **H4: 数值溢出或NaN** - 礼花粒子计算中可能出现除零或NaN值
5. **H5: 主线程阻塞** - 庆祝动画与渲染循环冲突导致ANR

## Instrumentation Plan
- 在drawCelebration入口、updateAndDrawFireworks入口、关键计算点添加日志
- 捕获异常并记录堆栈

## Evidence
(待收集)

## Fix
(待确定)

## Verification
(待验证)
