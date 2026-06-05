<script setup lang="ts">
import { computed } from 'vue'
import type { DictOption } from '@/types/models'
import { getDictOption } from '@/constants/dicts'

const props = withDefaults(
  defineProps<{
    value: number | string | null | undefined
    options: DictOption[]
    fallback?: string
  }>(),
  {
    fallback: '未知',
  },
)

const current = computed(() => getDictOption(props.options, props.value))
</script>

<template>
  <ElTag :type="current?.tagType || 'info'" effect="light" round>
    {{ current?.label || fallback }}
  </ElTag>
</template>
