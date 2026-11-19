<script setup lang="ts">defineProps<{text?:string}>()</script><template><view class="empty"><view class="empty-icon">🐾</view><view>{{text||'暂时还没有记录'}}</view><slot/></view></template>
