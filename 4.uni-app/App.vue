<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './store/modules/user'
onLaunch(() => { useUserStore().restore() })
</script>
<style> @import './assets/styles/global.css'; </style>
