<template>
  <div class="json-tool">
    <div class="tool-card">
      <h1 class="title">JSON 格式化工具</h1>
      <p class="subtitle">输入 JSON 自动格式化</p>

      <div class="editor-row">
        <div class="editor-panel">
          <label>输入 JSON</label>
          <textarea
            v-model="input"
            placeholder='{"name": "示例", "count": 123}'
            @input="formatInput"
            spellcheck="false"
          ></textarea>
          <p v-if="error" class="error-msg">{{ error }}</p>
        </div>
        <div class="editor-panel">
          <label>格式化结果</label>
          <div class="output-wrapper">
            <div class="output-inner">
              <div class="line-numbers">
                <span v-for="n in outputLineCount" :key="n">{{ n }}</span>
              </div>
              <pre class="output-content">{{ output || ' ' }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const input = ref('')
const output = ref('')
const error = ref('')
let formatTimer = null

const outputLineCount = computed(() => {
  if (!output.value) return 1
  return Math.max(1, output.value.split('\n').length)
})

const doFormat = () => {
  error.value = ''
  if (!input.value.trim()) {
    output.value = ''
    return
  }
  try {
    const parsed = JSON.parse(input.value)
    output.value = JSON.stringify(parsed, null, 2)
  } catch (e) {
    error.value = e.message || 'JSON 格式错误'
    output.value = ''
  }
}

const formatInput = () => {
  clearTimeout(formatTimer)
  formatTimer = setTimeout(doFormat, 300)
}
</script>

<style scoped>
.json-tool {
  width: 100%;
  margin: 0;
  padding: 1.5rem 1rem;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.tool-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e5e5ea;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 0.35rem;
}

.subtitle {
  color: #6e6e73;
  font-size: 0.95rem;
  margin-bottom: 1.5rem;
}

.editor-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  flex: 1;
  min-height: 0;
}

@media (max-width: 768px) {
  .editor-row {
    grid-template-columns: 1fr;
  }
}

.editor-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-height: 0;
}

.editor-panel label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #6e6e73;
}

.editor-panel textarea {
  width: 100%;
  flex: 1;
  min-height: 200px;
  padding: 1rem;
  border-radius: 10px;
  border: 1px solid #d2d2d7;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  resize: none;
  background: #fafafa;
}

.editor-panel textarea:focus {
  outline: none;
  border-color: #0071e3;
  background: #fff;
}

.editor-panel textarea::placeholder {
  color: #aeaeb2;
}

.output-wrapper {
  flex: 1;
  min-height: 200px;
  border-radius: 10px;
  border: 1px solid #d2d2d7;
  background: #fafafa;
  overflow: auto;
}

.output-inner {
  display: flex;
  min-height: min-content;
}

.line-numbers {
  padding: 1rem 0.5rem 1rem 1rem;
  background: #f0f0f5;
  color: #6e6e73;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  text-align: right;
  user-select: none;
  min-width: 2.5em;
}

.line-numbers span {
  display: block;
}

.output-content {
  flex: 1;
  margin: 0;
  padding: 1rem 1rem 1rem 0.5rem;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #1d1d1f;
  white-space: pre;
  overflow-x: auto;
}

.error-msg {
  font-size: 0.85rem;
  color: #ff3b30;
}
</style>
