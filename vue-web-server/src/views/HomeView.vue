<template>
  <div ref="wrapRef" class="home-wrapper">
    <canvas ref="canvasRef" class="snake-canvas" aria-hidden="true" />
    <aside class="score-board" aria-label="Snake score" aria-live="polite">
      <div class="score-block">
        <div class="score-label">SCORE</div>
        <div class="score-val">{{ scoreNow }}</div>
      </div>
      <div class="score-block">
        <div class="score-label">BEST</div>
        <div class="score-val score-best">{{ highScore }}</div>
      </div>
    </aside>
    <div class="hello">
      <span class="hello-top">Hello，</span>
      <span class="hello-bottom">世界</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

const wrapRef = ref(null)
const canvasRef = ref(null)

/** 当前得分 = 蛇长 - 初始长度；最高存 localStorage */
const scoreNow = ref(0)
const highScore = ref(0)
const HIGH_KEY = 'undersky-snake-high'
const START_LEN = 3

const COLS = 52
const ROWS = 36
const TICK_MS = 72

let ctx = null
/** 单格边长：取 min(宽/COLS, 高/ROWS)，整块棋盘不超出可视区 */
let cell = 10
let offsetX = 0
let offsetY = 0
let dpr = 1
let resizeObserver = null

let snake = []
let dir = { x: 1, y: 0 }
let food = { x: 0, y: 0 }
let timer = null

const DIRS = [
  [0, -1],
  [1, 0],
  [0, 1],
  [-1, 0]
]

function randInt(n) {
  return Math.floor(Math.random() * n)
}

function placeFood() {
  const occ = new Set(snake.map((s) => `${s.x},${s.y}`))
  const empty = []
  for (let x = 0; x < COLS; x++) {
    for (let y = 0; y < ROWS; y++) {
      if (!occ.has(`${x},${y}`)) empty.push({ x, y })
    }
  }
  if (empty.length === 0) return { x: 0, y: 0 }
  return empty[randInt(empty.length)]
}

function syncScore() {
  const s = Math.max(0, snake.length - START_LEN)
  scoreNow.value = s
  if (s > highScore.value) {
    highScore.value = s
    try {
      localStorage.setItem(HIGH_KEY, String(s))
    } catch (_) {
      /* ignore */
    }
  }
}

function reset() {
  const cx = Math.floor(COLS / 2)
  const cy = Math.floor(ROWS / 2)
  snake = [
    { x: cx, y: cy },
    { x: cx - 1, y: cy },
    { x: cx - 2, y: cy }
  ]
  dir = { x: 1, y: 0 }
  food = placeFood()
  syncScore()
}

/** BFS 找向食物的第一步；身体占格不含尾部（下一步会挪走） */
function chooseDir() {
  const head = snake[0]
  const blocked = new Set()
  for (let i = 1; i < snake.length - 1; i++) {
    blocked.add(`${snake[i].x},${snake[i].y}`)
  }

  const q = []
  const visited = new Set()

  for (const [dx, dy] of DIRS) {
    if (dx === -dir.x && dy === -dir.y) continue
    const nx = head.x + dx
    const ny = head.y + dy
    if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) continue
    if (blocked.has(`${nx},${ny}`)) continue
    const k = `${nx},${ny}`
    visited.add(k)
    q.push({ x: nx, y: ny, fdx: dx, fdy: dy })
  }

  while (q.length) {
    const cur = q.shift()
    if (cur.x === food.x && cur.y === food.y) {
      return { x: cur.fdx, y: cur.fdy }
    }
    for (const [dx, dy] of DIRS) {
      const nx = cur.x + dx
      const ny = cur.y + dy
      const k = `${nx},${ny}`
      if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) continue
      if (blocked.has(k)) continue
      if (visited.has(k)) continue
      visited.add(k)
      q.push({ x: nx, y: ny, fdx: cur.fdx, fdy: cur.fdy })
    }
  }

  for (const [dx, dy] of DIRS) {
    if (dx === -dir.x && dy === -dir.y) continue
    const nx = head.x + dx
    const ny = head.y + dy
    if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) continue
    if (snake.some((s) => s.x === nx && s.y === ny)) continue
    return { x: dx, y: dy }
  }
  return null
}

function step() {
  const nextDir = chooseDir()
  if (nextDir) dir = nextDir

  const head = snake[0]
  const nx = head.x + dir.x
  const ny = head.y + dir.y

  if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) {
    reset()
    draw()
    return
  }
  if (snake.some((s) => s.x === nx && s.y === ny)) {
    reset()
    draw()
    return
  }

  snake.unshift({ x: nx, y: ny })
  if (nx === food.x && ny === food.y) {
    if (snake.length >= COLS * ROWS) {
      reset()
    } else {
      food = placeFood()
    }
  } else {
    snake.pop()
  }

  syncScore()
  draw()
}

function resize() {
  const canvas = canvasRef.value
  const wrap = wrapRef.value
  if (!canvas || !wrap) return

  dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = wrap.clientWidth
  const h = wrap.clientHeight
  cell = Math.max(4, Math.min(w / COLS, h / ROWS))
  const gw = cell * COLS
  const gh = cell * ROWS
  offsetX = (w - gw) / 2
  offsetY = (h - gh) / 2

  const pad = 10
  wrap.style.setProperty('--snake-grid-top', `${Math.max(pad, offsetY)}px`)
  wrap.style.setProperty('--snake-grid-right', `${Math.max(pad, w - offsetX - gw)}px`)

  canvas.width = Math.floor(w * dpr)
  canvas.height = Math.floor(h * dpr)
  canvas.style.width = `${w}px`
  canvas.style.height = `${h}px`
  ctx = canvas.getContext('2d')
  draw()
}

function draw() {
  if (!ctx) return
  const wrap = wrapRef.value
  if (!wrap) return

  const w = wrap.clientWidth
  const h = wrap.clientHeight
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  ctx.fillStyle = 'rgba(245, 245, 247, 0.94)'
  ctx.fillRect(0, 0, w, h)

  const gw = cell * COLS
  const gh = cell * ROWS

  ctx.strokeStyle = 'rgba(210, 210, 215, 0.35)'
  ctx.lineWidth = 1
  for (let c = 0; c <= COLS; c++) {
    const px = offsetX + c * cell
    ctx.beginPath()
    ctx.moveTo(px + 0.5, offsetY + 0.5)
    ctx.lineTo(px + 0.5, offsetY + gh + 0.5)
    ctx.stroke()
  }
  for (let r = 0; r <= ROWS; r++) {
    const py = offsetY + r * cell
    ctx.beginPath()
    ctx.moveTo(offsetX + 0.5, py + 0.5)
    ctx.lineTo(offsetX + gw - 0.5, py + 0.5)
    ctx.stroke()
  }

  const r = Math.max(2, cell * 0.38)
  for (let i = snake.length - 1; i >= 0; i--) {
    const s = snake[i]
    const cx = offsetX + s.x * cell + cell / 2
    const cy = offsetY + s.y * cell + cell / 2
    const t = i / Math.max(snake.length - 1, 1)
    ctx.fillStyle =
      i === 0
        ? 'rgba(0, 113, 227, 0.85)'
        : `rgba(0, 113, 227, ${0.22 + t * 0.45})`
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    ctx.fill()
  }

  const fx = offsetX + food.x * cell + cell / 2
  const fy = offsetY + food.y * cell + cell / 2
  ctx.fillStyle = 'rgba(52, 199, 89, 0.9)'
  ctx.beginPath()
  ctx.arc(fx, fy, r * 0.85, 0, Math.PI * 2)
  ctx.fill()
  ctx.strokeStyle = 'rgba(34, 160, 65, 0.6)'
  ctx.lineWidth = 1.5
  ctx.stroke()
}

onMounted(() => {
  nextTick(() => {
    try {
      const raw = localStorage.getItem(HIGH_KEY)
      if (raw != null) {
        const n = parseInt(raw, 10)
        if (!Number.isNaN(n) && n >= 0) highScore.value = n
      }
    } catch (_) {
      /* ignore */
    }
    reset()
    resize()
    resizeObserver = new ResizeObserver(() => resize())
    if (wrapRef.value) resizeObserver.observe(wrapRef.value)
    window.addEventListener('resize', resize)
    timer = window.setInterval(step, TICK_MS)
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resize)
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.home-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: calc(100vh - 72px);
  overflow: hidden;
  background: var(--color-bg, #f5f5f7);
}

.snake-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.score-board {
  position: absolute;
  top: var(--snake-grid-top, 12px);
  right: var(--snake-grid-right, 12px);
  left: auto;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.85rem;
  padding: 0;
  margin: 0;
  background: none;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
  pointer-events: none;
  text-align: right;
}

.score-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
  line-height: 1;
}

.score-label {
  font-family: 'Orbitron', system-ui, sans-serif;
  font-size: clamp(0.65rem, 1.6vmin, 0.8rem);
  font-weight: 700;
  letter-spacing: 0.28em;
  color: #424245;
  text-transform: uppercase;
  text-shadow:
    0 1px 0 rgba(255, 255, 255, 0.95),
    0 0 12px rgba(255, 255, 255, 0.6);
}

.score-val {
  font-family: 'Orbitron', system-ui, sans-serif;
  font-size: clamp(1.75rem, 5.2vmin, 2.85rem);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.06em;
  color: #0077ed;
  text-shadow:
    0 1px 0 #fff,
    0 2px 0 rgba(255, 255, 255, 0.5),
    0 4px 12px rgba(0, 113, 227, 0.35),
    0 0 28px rgba(0, 180, 255, 0.25);
}

.score-best {
  color: #34c759;
  text-shadow:
    0 1px 0 #fff,
    0 2px 0 rgba(255, 255, 255, 0.45),
    0 4px 14px rgba(52, 199, 89, 0.4),
    0 0 24px rgba(48, 209, 88, 0.2);
}

.hello {
  position: relative;
  z-index: 3;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-align: center;
  color: var(--color-text, #1d1d1f);
  pointer-events: none;
  text-shadow: 0 0 20px rgba(245, 245, 247, 0.95), 0 0 12px rgba(245, 245, 247, 0.8);
}

.hello-top,
.hello-bottom {
  display: block;
  font-size: clamp(2.8rem, 7vw, 4.4rem);
  line-height: 1.08;
}

.hello-top {
  animation: float-up 6s ease-in-out infinite;
}

.hello-bottom {
  background: linear-gradient(120deg, #0071e3, #64d2ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: float-down 4s ease-in-out infinite;
}

@keyframes float-up {
  0%,
  100% {
    transform: translateY(4px);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes float-down {
  0%,
  100% {
    transform: translateY(-2px);
  }
  50% {
    transform: translateY(10px);
  }
}
</style>
