<template>
  <div class="home-wrapper">
    <div class="hello">
      <span class="hello-top">Hello，</span>
      <span class="hello-bottom">世界</span>
    </div>
  </div>
</template>

<script setup>
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
}

.hello {
  position: relative;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-align: center;
  color: #1d1d1f;
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
  opacity: 0.94;
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
