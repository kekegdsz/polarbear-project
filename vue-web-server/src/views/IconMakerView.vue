<template>
  <div class="icon-maker-page">
    <section class="panel controls">
      <h2>App Icon 制作工具</h2>
      <p class="desc">上传一张源图，调整边缘裁剪和圆角后，一键导出 iOS / Android / OHOS 常用图标尺寸。</p>

      <div
        class="dropzone"
        :class="{ 'is-dragover': isDragOver }"
        @click="triggerFileSelect"
        @dragenter.prevent="isDragOver = true"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop"
      >
        <p>拖动图片到这里上传</p>
        <p class="dropzone-sub">或点击选择文件（PNG / JPG / WEBP）</p>
      </div>

      <label class="field">
        <span>上传图片</span>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          @change="handleFileChange"
        />
      </label>

      <label class="field">
        <span>边缘裁剪：{{ edgeTrimPercent }}%</span>
        <input v-model.number="edgeTrimPercent" type="range" min="0" max="45" step="1" />
      </label>

      <label class="field">
        <span>圆角：{{ cornerRadiusPercent }}%</span>
        <input v-model.number="cornerRadiusPercent" type="range" min="0" max="50" step="1" />
      </label>

      <label class="field">
        <span>预览尺寸</span>
        <select v-model.number="previewSize">
          <option :value="128">128 x 128</option>
          <option :value="256">256 x 256</option>
          <option :value="512">512 x 512</option>
          <option :value="1024">1024 x 1024</option>
        </select>
      </label>

      <div class="actions">
        <button class="btn btn-primary" :disabled="!imageReady || downloading" @click="downloadAll">
          {{ downloading ? '打包中...' : '一键裁剪并导出 ZIP' }}
        </button>
      </div>
    </section>

    <section class="panel preview">
      <h3>预览</h3>
      <div v-if="!imageReady" class="empty">请先上传图片（建议 1024 x 1024 或更大）</div>
      <canvas v-else ref="previewCanvas" :width="previewSize" :height="previewSize" />
    </section>

    <section class="panel outputs">
      <h3>导出尺寸</h3>
      <div class="grid">
        <div v-for="group in iconGroups" :key="group.key" class="group">
          <h4>{{ group.title }}</h4>
          <div class="sizes">
            <button
              v-for="size in group.sizes"
              :key="`${group.key}-${size}`"
              class="size-btn"
              :disabled="!imageReady"
              @click="downloadSingle(group.key, size)"
            >
              {{ size }} x {{ size }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import JSZip from 'jszip'

const edgeTrimPercent = ref(0)
const cornerRadiusPercent = ref(20)
const previewSize = ref(256)
const previewCanvas = ref(null)
const fileInputRef = ref(null)
const downloading = ref(false)
const sourceImage = ref(null)
const isDragOver = ref(false)

const iconGroups = [
  { key: 'ios', title: 'iOS', sizes: [20, 29, 40, 60, 76, 83, 1024] },
  { key: 'android', title: 'Android', sizes: [48, 72, 96, 144, 192, 512] },
  { key: 'ohos', title: 'OHOS', sizes: [64, 96, 128, 192, 256, 512] }
]

const imageReady = computed(() => Boolean(sourceImage.value))

const roundedRect = (ctx, x, y, width, height, radius) => {
  const safeRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2))
  ctx.beginPath()
  ctx.moveTo(x + safeRadius, y)
  ctx.lineTo(x + width - safeRadius, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + safeRadius)
  ctx.lineTo(x + width, y + height - safeRadius)
  ctx.quadraticCurveTo(x + width, y + height, x + width - safeRadius, y + height)
  ctx.lineTo(x + safeRadius, y + height)
  ctx.quadraticCurveTo(x, y + height, x, y + height - safeRadius)
  ctx.lineTo(x, y + safeRadius)
  ctx.quadraticCurveTo(x, y, x + safeRadius, y)
  ctx.closePath()
}

const drawIconOnCanvas = (canvas, size) => {
  if (!canvas || !sourceImage.value) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = size
  canvas.height = size
  ctx.clearRect(0, 0, size, size)

  const img = sourceImage.value
  const trimRatio = edgeTrimPercent.value / 100
  const trimX = img.width * trimRatio
  const trimY = img.height * trimRatio
  const sw = img.width - trimX * 2
  const sh = img.height - trimY * 2
  const side = Math.max(1, Math.min(sw, sh))
  const sx = trimX + (sw - side) / 2
  const sy = trimY + (sh - side) / 2

  const radius = (cornerRadiusPercent.value / 100) * size
  roundedRect(ctx, 0, 0, size, size, radius)
  ctx.save()
  ctx.clip()
  ctx.drawImage(img, sx, sy, side, side, 0, 0, size, size)
  ctx.restore()
}

const refreshPreview = async () => {
  await nextTick()
  drawIconOnCanvas(previewCanvas.value, previewSize.value)
}

const loadFromFile = async (file) => {
  if (!file) return

  if (!file.type.startsWith('image/')) return

  const url = URL.createObjectURL(file)
  const img = new Image()
  img.onload = async () => {
    sourceImage.value = img
    await refreshPreview()
    URL.revokeObjectURL(url)
  }
  img.onerror = () => {
    URL.revokeObjectURL(url)
  }
  img.src = url
}

const handleFileChange = async (event) => {
  const file = event.target?.files?.[0]
  await loadFromFile(file)
}

const triggerFileSelect = () => {
  fileInputRef.value?.click()
}

const handleDrop = async (event) => {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  await loadFromFile(file)
}

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const renderBlob = (size) =>
  new Promise((resolve, reject) => {
    if (!sourceImage.value) return reject(new Error('no source image'))
    const canvas = document.createElement('canvas')
    drawIconOnCanvas(canvas, size)
    canvas.toBlob(
      (blob) => {
        if (!blob) return reject(new Error('blob failed'))
        resolve(blob)
      },
      'image/png',
      1
    )
  })

const downloadSingle = async (platform, size) => {
  if (!imageReady.value) return
  const blob = await renderBlob(size)
  downloadBlob(blob, `icon-${platform}-${size}.png`)
}

const downloadAll = async () => {
  if (!imageReady.value || downloading.value) return
  downloading.value = true
  try {
    const zip = new JSZip()
    for (const group of iconGroups) {
      const folder = zip.folder(group.key)
      for (const size of group.sizes) {
        // eslint-disable-next-line no-await-in-loop
        const blob = await renderBlob(size)
        folder.file(`icon-${group.key}-${size}.png`, blob)
      }
    }
    const zipBlob = await zip.generateAsync({ type: 'blob' })
    downloadBlob(zipBlob, `app-icons-${Date.now()}.zip`)
  } finally {
    downloading.value = false
  }
}

watch([edgeTrimPercent, cornerRadiusPercent, previewSize, sourceImage], refreshPreview)
</script>

<style scoped>
.icon-maker-page {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(280px, 420px) minmax(280px, 1fr);
  gap: 1rem;
  width: 100%;
  min-height: calc(100vh - 92px);
}

.panel {
  background: #fff;
  border: 1px solid #e6e6eb;
  border-radius: 14px;
  padding: 1rem;
}

.controls h2 {
  margin: 0;
  font-size: 1.1rem;
}

.desc {
  margin: 0.5rem 0 1rem;
  font-size: 0.88rem;
  color: #6e6e73;
}

.dropzone {
  border: 1px dashed #b8c3d1;
  border-radius: 12px;
  padding: 1rem 0.8rem;
  text-align: center;
  margin-bottom: 0.9rem;
  background: #fafbfe;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropzone p {
  margin: 0;
  font-size: 0.9rem;
  color: #1d1d1f;
}

.dropzone-sub {
  margin-top: 0.35rem !important;
  font-size: 0.82rem !important;
  color: #6e6e73 !important;
}

.dropzone.is-dragover {
  border-color: #0071e3;
  background: #eef6ff;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 0.9rem;
  font-size: 0.9rem;
}

.field input[type='file'],
.field select,
.field input[type='range'] {
  width: 100%;
}

.actions {
  margin-top: 1rem;
}

.btn {
  border: none;
  border-radius: 10px;
  padding: 0.65rem 0.9rem;
  cursor: pointer;
}

.btn-primary {
  background: #0071e3;
  color: #fff;
  width: 100%;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.preview {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview h3,
.outputs h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
}

.preview canvas {
  max-width: 100%;
  border-radius: 12px;
  border: 1px dashed #cfd3da;
  background:
    linear-gradient(45deg, #f2f3f7 25%, transparent 25%) -10px 0 / 20px 20px,
    linear-gradient(-45deg, #f2f3f7 25%, transparent 25%) -10px 0 / 20px 20px,
    linear-gradient(45deg, transparent 75%, #f2f3f7 75%) -10px 0 / 20px 20px,
    linear-gradient(-45deg, transparent 75%, #f2f3f7 75%) -10px 0 / 20px 20px;
}

.empty {
  width: 100%;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #8d8d93;
  border: 1px dashed #cfd3da;
  border-radius: 12px;
}

.grid {
  display: grid;
  gap: 0.8rem;
}

.group {
  border: 1px solid #ececf1;
  border-radius: 10px;
  padding: 0.7rem;
}

.group h4 {
  margin: 0 0 0.6rem;
  font-size: 0.92rem;
}

.sizes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.size-btn {
  border: 1px solid #d9dbe2;
  border-radius: 8px;
  background: #fff;
  padding: 0.35rem 0.55rem;
  font-size: 0.82rem;
  cursor: pointer;
}

.size-btn:hover:not(:disabled) {
  border-color: #0071e3;
  color: #0071e3;
}

@media (max-width: 1200px) {
  .icon-maker-page {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}
</style>
