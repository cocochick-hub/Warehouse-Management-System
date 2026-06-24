<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './store/user'

onLaunch(() => {
  const userStore = useUserStore()
  userStore.checkLogin()
})
</script>

<style>
@import 'vant/lib/index.css';

page {
  background-color: #f7f8fa;
}
</style>
