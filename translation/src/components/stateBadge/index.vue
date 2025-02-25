<template>
  <a-badge v-if="entryState === '已审核'" color="#87d068" style="color:#87d068" :text="entryState" />
  <a-badge v-else-if="entryState === '审核中'" color="#2db7f5" style="color:#2db7f5" :text="entryState" />
  <a-badge v-else-if="entryState === '审核不通过'" color="#f50" style="color:#f50" :text="entryState" />
  <a-badge v-else color="yellow" style="color:yellow" :text="entryState" />
</template>

<script>
export default {
  props: {
    entryState: {
      type: String,
      required: true,
    },
  },
  methods: {},
};
</script>
