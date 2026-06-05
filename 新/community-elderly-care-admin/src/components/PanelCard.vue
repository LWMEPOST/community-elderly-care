<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="panel-card">
    <header class="panel-card__header">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="panel-card__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="panel-card__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.panel-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface);
  box-shadow: 0 16px 44px rgba(18, 57, 84, 0.08);
  backdrop-filter: blur(10px);
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 22px 24px 0;
}

.panel-card__header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.panel-card__header p {
  margin: 8px 0 0;
  color: var(--app-text-soft);
  font-size: 13px;
}

.panel-card__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-card__body {
  padding: 20px 24px 24px;
}
</style>
